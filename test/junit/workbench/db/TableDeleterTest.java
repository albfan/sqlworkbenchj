/*
 * TableDeleterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import workbench.TestUtil;
import workbench.interfaces.JobErrorHandler;

import workbench.sql.parser.ScriptParser;

import workbench.util.SqlUtil;

import static org.junit.Assert.*;

import org.junit.Test;

import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDeleterTest
	extends WbTestCase
{

	private boolean errorCalled;
	private int errorAction;
	private boolean fatalError;

	public TableDeleterTest()
	{
		super("TableDeleterTest");
	}

	@Test
	public void testDeleteTableData()
		throws Exception
	{
		WbConnection con = null;
		Statement stmt = null;
		try
		{
			con = setupDatabase();
			List<TableIdentifier> tables = new ArrayList<>();
			tables.add(new TableIdentifier("person_company"));
			tables.add(new TableIdentifier("person_address"));
			tables.add(new TableIdentifier("person"));
			tables.add(new TableIdentifier("company"));

			TableDeleter deleter = new TableDeleter(con);
			List<TableIdentifier> deleted = deleter.deleteTableData(tables, false, false, false);
			assertEquals(tables.size(), deleted.size());
			stmt = con.createStatement();

			for (TableIdentifier table : deleted)
			{
				ResultSet rs = stmt.executeQuery("SELECT count(*) from " + table.getTableName());
				int count = -1;
				if (rs.next())
				{
					count = rs.getInt(1);
				}
				SqlUtil.closeResult(rs);
				assertEquals(0, count);
			}
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
			ConnectionMgr.getInstance().disconnectAll();
		}
	}


	@Test
	public void testErrorHandling()
		throws Exception
	{
		WbConnection con = null;
		Statement stmt = null;
		try
		{
			con = setupDatabase();

			final List<TableIdentifier> tables =
				Collections.unmodifiableList(
					Arrays.asList(new TableIdentifier[]
					{
						new TableIdentifier("company"),
						new TableIdentifier("person"),
						new TableIdentifier("person_company"),
						new TableIdentifier("person_address")
					}
				));

			errorCalled = false;
			fatalError = false;
			errorAction = JobErrorHandler.JOB_ABORT;

			JobErrorHandler handler = new JobErrorHandler()
			{
        @Override
				public int getActionOnError(int errorRow, String errorColumn, String data, String errorMessage)
				{
					errorCalled = true;
					return errorAction;
				}

        @Override
				public void fatalError(String msg)
				{
					System.out.println("Fatal error :" + msg);
					fatalError = true;
				}
			};

			TableDeleter deleter = new TableDeleter(con);
			deleter.setErrorHandler(handler);

			List<TableIdentifier> deleted = deleter.deleteTableData(tables, false, false, false);
			assertTrue(errorCalled);
			assertFalse(fatalError);
			assertEquals(0, deleted.size());
			stmt = con.createStatement();

			for (TableIdentifier table : tables)
			{
				ResultSet rs = stmt.executeQuery("SELECT count(*) from " + table.getTableName());
				int count = -1;
				if (rs.next())
				{
					count = rs.getInt(1);
				}
				SqlUtil.closeResult(rs);
				assertTrue(count > 0);
			}

			errorAction = JobErrorHandler.JOB_IGNORE_ALL;

			con.disconnect();
			con = setupDatabase();
			deleter = new TableDeleter(con);
			deleter.setErrorHandler(handler);

			// company and person cannot be deleted, but person_company and person_address can.
			// As the job error handler will make the deleter continue, two tables should be
			// deleted in the end.
			deleted = deleter.deleteTableData(tables, true, false, false);
			assertTrue(errorCalled);
			assertFalse(fatalError);
			assertEquals(2, deleted.size());

			con.disconnect();
			con = setupDatabase();
			deleter = new TableDeleter(con);
			deleter.setErrorHandler(handler);

			// Now test with a single commit at the end.
			deleted = deleter.deleteTableData(tables, false, false, false);
			assertTrue(errorCalled);
			assertFalse(fatalError);
			assertEquals(2, deleted.size());

		}
		finally
		{
			SqlUtil.closeStatement(stmt);
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testGenerateScript()
		throws Exception
	{
		WbConnection con = null;
		try
		{
			con = setupDatabase();
			List<TableIdentifier> tables = new ArrayList<>();
			tables.add(new TableIdentifier("person_company"));
			tables.add(new TableIdentifier("person_address"));
			tables.add(new TableIdentifier("person"));
			tables.add(new TableIdentifier("company"));

			TableDeleter deleter = new TableDeleter(con);
			String sql = deleter.generateScript(tables, CommitType.once, false, false).toString();
			ScriptParser p = new ScriptParser(sql);
			assertEquals(5, p.getSize());
			assertTrue(p.getCommand(0).startsWith("DELETE"));
			assertTrue(p.getCommand(4).startsWith("COMMIT"));

			sql = deleter.generateScript(tables, CommitType.each, false, false).toString();
			p = new ScriptParser(sql);
			assertEquals(tables.size() * 2, p.getSize());
			assertTrue(p.getCommand(0).startsWith("DELETE"));
			assertTrue(p.getCommand(1).startsWith("COMMIT"));
			assertTrue(p.getCommand(2).startsWith("DELETE"));
			assertTrue(p.getCommand(3).startsWith("COMMIT"));

			sql = deleter.generateScript(tables, CommitType.each, true, false).toString();
      System.out.println("*****\n" + sql + "\n****");
			p = new ScriptParser(sql);
			assertEquals(tables.size(), p.getSize());
			assertTrue(p.getCommand(0).startsWith("TRUNCATE"));
			assertTrue(p.getCommand(1).startsWith("TRUNCATE"));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	private WbConnection setupDatabase()
		throws Exception
	{
		TestUtil util = new TestUtil("tableDeleterTest");
		WbConnection conn = util.getConnection();
		String tables = "CREATE TABLE person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" +
			"create table address (address_id integer primary key, address varchar(50));\n" +
			"create table company (company_id integer primary key, company_name varchar(50));\n" +
			"create table person_company (person_id integer, company_id integer, primary key (person_id, company_id));\n" +
			"create table person_address (person_id integer, address_id integer, primary key (person_id, address_id));\n" +
			"commit;\n";

		TestUtil.executeScript(conn, tables);
		String fk = "alter table person_company add constraint fk_pc_p foreign key (person_id) references person(person_id);\n" +
				"alter table person_company add constraint fk_pc_c foreign key (company_id) references company (company_id);\n" +
				"alter table person_address add constraint fk_pa_p foreign key (person_id) references person(person_id);\n" +
				"alter table person_address add constraint fk_pa_a foreign key (address_id) references address (address_id);\n" +
				"commit;\n";
		TestUtil.executeScript(conn, fk);

		String data = "insert into person values (1, 'Arthur', 'Dent');\n" +
			"insert into person values (2, 'Ford', 'Prefect');\n" +
			"insert into person values (3, 'Zaphod', 'Beeblebrox');\n" +
			"insert into person values (4, 'Tricia', 'McMillian');\n" +
			"commit;\n" +
			"insert into address values (1, 'Arthur''s Home');\n" +
			"insert into address values (2, 'Ford''s Home');\n" +
			"insert into address values (3, 'Zaphod''s Home');\n" +
		  "commit;\n" +
			"insert into person_address values (1, 1);\n" + // arthur
			"insert into person_address values (2, 2);\n" + // ford
			"insert into person_address values (3, 3);\n" + // zaphod
			"insert into person_address values (4, 3);\n" + // tricia
			"commit;\n" +
			"insert into company (company_id, company_name) values (1, 'Some Company');\n" +
			"insert into company (company_id, company_name) values (2, 'h2g');\n" +
			"commit; \n" +
			"insert into person_company (person_id, company_id) values (1, 2);\n" +
			"insert into person_company (person_id, company_id) values (2, 2);\n" +
			"commit;\n";

		TestUtil.executeScript(conn, data);

		return conn;

	}
}
