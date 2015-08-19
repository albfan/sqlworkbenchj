/*
 * WbCopyPostgresTest.java
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
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.WbCopy;

import workbench.util.SqlUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCopyPostgresTest
	extends WbTestCase
{

	private static final String TEST_ID = "wbcopy_test_pg";

	public WbCopyPostgresTest()
	{
		super(TEST_ID);
	}


	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

		TestUtil.executeScript(con,
			"create table person (id integer primary key, first_name varchar(50), last_name varchar(50));\n" +
			"insert into person (id, first_name, last_name) values (1, 'Arthur', 'Dent');\n" +
			"insert into person (id, first_name, last_name) values (2, 'Ford', 'Prefect');\n" +
			"commit;\n" +
			"create table address (id integer primary key, person_id integer, address_info varchar(100));\n" +
			"alter table address add constraint fk_person_address foreign key (person_id) references person (id);\n" +
			"insert into address (id, person_id, address_info) values (1, 1, 'arthur'); \n" +
			"insert into address (id, person_id, address_info) values (2, 2, 'ford'); \n" +
			"commit;\n"
			);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		Settings.getInstance().setProperty("workbench.sql.ignoreschema.postgresql", "public");
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testTempTarget()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

    Statement stmt = null;
    ResultSet rs = null;
		try
		{
			con.getMetadata().resetSchemasToIgnores();

			Settings.getInstance().setProperty("workbench.sql.ignoreschema.postgresql", "public," + TEST_ID);

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(con);

			TestUtil.executeScript(con,
				"create temporary table target_person (id integer, first_name varchar(50), last_name varchar(50));\n " +
				"commit;\n");

			String sql =
				"wbcopy -sourceQuery='select id, first_name, last_name from person' " +
				"       -targetTable=target_person -skipTargetCheck=true";

			StatementRunnerResult result = copyCmd.execute(sql);
			String msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());

			stmt = con.createStatement();
			rs = stmt.executeQuery("select count(*) from target_person");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 2, count);
			}
		}
		finally
		{
			if (con != null) con.getMetadata().resetSchemasToIgnores();
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Test
	public void testDifferentSchemaCopy()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

		TestUtil util = getTestUtil();
		WbConnection source = util.getConnection("copySourceSchema"); // H2

		WbCopy copyCmd = new WbCopy();
		copyCmd.setConnection(con);

		TestUtil.executeScript(con,
			"create table public.person (id integer, first_name varchar(50), last_name varchar(50));\n " +
			"create table public.data_ (id integer, some_info varchar(50));\n " +
			"commit;\n");

		TestUtil.executeScript(source,
			"create schema foobar; \n" +
			"create table foobar.person (id integer, first_name varchar(50), last_name varchar(50));\n " +
			"insert into foobar.person (id, first_name, last_name) values (42, 'Arthur', 'Dent');\n" +
			"create table foobar.data_ (id integer, some_info varchar(50));\n " +
			"insert into foobar.data_ (id, some_info) values (24, 'foo');\n " +
			"commit;\n");

		String sql =
			"wbcopy -sourceSchema=FOOBAR \n" +
			"       -sourceTable=* \n" +
			"       -targetSchema=public \n"+
			"       -sourceProfile=" + source.getProfile().getName() + " \n ";

		StatementRunnerResult result = copyCmd.execute(sql);
		String msg = result.getMessageBuffer().toString();
//    System.out.println(msg);
		assertEquals(msg, true, result.isSuccess());

		Integer id = (Integer)TestUtil.getSingleQueryValue(con, "select id from public.person where last_name = 'Dent'");
		assertEquals(42, id.intValue());

		id = (Integer)TestUtil.getSingleQueryValue(con, "select id from public.data_ where some_info = 'foo'");
		assertEquals(24, id.intValue());

	}

	@Test
	public void testCopyFromH2()
		throws Exception
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			WbConnection pgCon = PostgresTestUtil.getPostgresConnection();
			if (pgCon == null) return;

			TestUtil util = getTestUtil();
			WbConnection source = util.getConnection("copyCreateTestSource"); // H2

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(pgCon);

			TestUtil.executeScript(source,
				"create table person (id integer, first_name varchar(50), last_name varchar(50));\n " +
				"insert into person (id, first_name, last_name) values (1, 'Arthur', 'Dent');\n" +
				"commit;\n");

			String sql =
				"wbcopy -sourceQuery='select id as person_id, first_name as vorname, last_name  as nachname from person' " +
				"       -targetTable=some_person -createTarget=true \n"+
				"       -sourceProfile=" + source.getProfile().getName() + " \n ";

			StatementRunnerResult result = copyCmd.execute(sql);
			String msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());

			stmt = pgCon.createStatement();
			rs = stmt.executeQuery("select count(*) from some_person where nachname = 'Dent'");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 1, count);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Test
	public void testFKError()
		throws Exception
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			WbConnection pgCon = PostgresTestUtil.getPostgresConnection();
			if (pgCon == null) return;

			TestUtil util = getTestUtil();
			WbConnection source = util.getConnection("copyCreateTestSource"); // H2

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(pgCon);

			TestUtil.executeScript(source,
				"create table address (id integer primary key, person_id integer, address_info varchar(100));\n" +
				"insert into address (id, person_id, address_info) values (1, 10, 'arthur'); \n" +
				"insert into address (id, person_id, address_info) values (2, 2, 'ford'); \n" +
				"commit;\n");

			TestUtil.executeScript(pgCon,
				"delete from address;\n" +
				"commit;\n"
			);

			String sql =
				"wbcopy -sourceTable=address " +
				"       -targetTable=address -mode=insert,update \n"+
				"       -sourceProfile=" + source.getProfile().getName() + " \n ";

			StatementRunnerResult result = copyCmd.execute(sql);
			String msg = result.getMessageBuffer().toString();

			// has to fail because the FK exception should not be ignored!
			assertEquals(msg, false, result.isSuccess());

			TestUtil.executeScript(pgCon,
				"delete from address;\n" +
				"insert into address (id, person_id, address_info) values (2, 2, 'oldvalue'); \n" +
				"commit;\n"
			);

			sql =
				"wbcopy -sourceTable=address -continueOnError=true -useSavepoint=true " +
				"       -targetTable=address -mode=insert,update \n"+
				"       -sourceProfile=" + source.getProfile().getName() + " \n ";

			result = copyCmd.execute(sql);
			msg = result.getMessageBuffer().toString();

			assertEquals(msg, true, result.isSuccess());
			stmt = pgCon.createStatement();
			rs = stmt.executeQuery("select count(*) from address");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 1, count);
			}
			rs.close();
			rs = stmt.executeQuery("select address_info from address where id = 2");
			if (rs.next())
			{
				String value = rs.getString(1);
				assertEquals("Wrong value imported", "ford", value);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

	}
}
