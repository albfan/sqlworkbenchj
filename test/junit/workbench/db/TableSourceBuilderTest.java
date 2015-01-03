/*
 * TableSourceBuilderTest.java
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


import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.DbExplorerSettings;
import workbench.resource.Settings;

import workbench.db.sqltemplates.ColumnChanger;
import workbench.db.sqltemplates.ColumnDefinitionTemplate;

import workbench.util.CollectionUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableSourceBuilderTest
	extends WbTestCase
{

	public TableSourceBuilderTest()
	{
		super("TableSourceBuilderTest");
	}

	@Test
	public void testGetTableSource()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		try
		{
			TestUtil.executeScript(con,
				"CREATE TABLE person (id integer not null, firstname varchar(20), lastname varchar(20));\n" +
				"ALTER TABLE PERSON ADD constraint pk_person primary key (id);\n" +
				"COMMIT;\n");
			TableSourceBuilder builder = new TableSourceBuilder(con);
			TableIdentifier tbl = new TableIdentifier("PERSON");
			String sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.startsWith("CREATE TABLE PERSON"));
			assertTrue(sql.indexOf("PRIMARY KEY (ID)") > -1);

			String dbid = con.getMetadata().getDbId();
			Settings.getInstance().setProperty("workbench.db." + dbid + ".coldef", ColumnChanger.PARAM_DATATYPE + " " + ColumnChanger.PARAM_NULLABLE);

			builder = new TableSourceBuilder(con);
			sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.indexOf("FIRSTNAME  VARCHAR(20)   NULL") > -1);

			TestUtil.executeScript(con,
				"ALTER TABLE person ALTER COLUMN firstname SET DEFAULT 'Arthur';" +
				"COMMIT;\n");

			builder = new TableSourceBuilder(con);
			Settings.getInstance().setProperty("workbench.db." + dbid + ".coldef", ColumnChanger.PARAM_DATATYPE + " " + ColumnChanger.PARAM_DEFAULT_VALUE + " " + ColumnChanger.PARAM_NULLABLE);
			sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.indexOf("DEFAULT 'Arthur' NULL") > -1);

			builder = new TableSourceBuilder(con);
			Settings.getInstance().setProperty("workbench.db." + dbid + ".coldef", ColumnChanger.PARAM_DATATYPE + " " + ColumnChanger.PARAM_NULLABLE + " " + ColumnChanger.PARAM_DEFAULT_VALUE);
			sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.indexOf("NULL DEFAULT 'Arthur'") > -1);

			builder = new TableSourceBuilder(con);
			Settings.getInstance().setProperty("workbench.db." + dbid + ".coldef", ColumnChanger.PARAM_DATATYPE + " " + ColumnDefinitionTemplate.PARAM_NOT_NULL + " " + ColumnChanger.PARAM_DEFAULT_VALUE);
			sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.indexOf("FIRSTNAME  VARCHAR(20)   DEFAULT 'Arthur'") > -1);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
			ConnectionMgr.getInstance().clearProfiles();
		}
	}

	@Test
	public void testCheckConstraints()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		try
		{
			TestUtil.executeScript(con,
				"CREATE TABLE orders (\n" +
				"  id integer not null, \n" +
				"  amount decimal(10,2), \n" +
				"  status_flag integer not null default 0, \n" +
				"  constraint chk_amount check (amount >= 0), \n" +
				"  constraint chk_status check (status_flag in (0,1,2)) \n" +
				");\n" +
				"COMMIT;\n"
			);
			TableSourceBuilder builder = new TableSourceBuilder(con);
			TableIdentifier tbl = new TableIdentifier("ORDERS");
			String sql = builder.getTableSource(tbl, false, false);
			System.out.println(sql);
			assertTrue(sql.contains("CONSTRAINT CHK_AMOUNT CHECK (AMOUNT >= 0)"));
			assertTrue(sql.contains("CONSTRAINT CHK_STATUS CHECK (STATUS_FLAG IN(0, 1, 2))"));

			TestUtil.executeScript(con,
				"drop table orders; \n" +
				"CREATE TABLE orders\n" +
				"(\n" +
				"  id integer not null, \n" +
				"  amount decimal(10,2), \n" +
				"  status_flag integer not null default 0, \n" +
				"  constraint chk_amount check (amount >= 0)\n" +
				");\n" +
				"COMMIT;\n"
			);
			sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.contains("CONSTRAINT CHK_AMOUNT CHECK (AMOUNT >= 0)\n);"));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
			ConnectionMgr.getInstance().clearProfiles();
		}
	}

	@Test
	public void testFkSource()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		try
		{
			TestUtil.executeScript(con,
				"create table customers (id integer not null primary key, name varchar(20) not null);\n" +
				"CREATE TABLE orders (\n" +
				"  id integer not null, \n" +
				"  customer_id integer not null references customers, \n" +
				"  amount decimal(10,2)" +
				");\n" +
				"COMMIT;\n"
			);
			TableSourceBuilder builder = new TableSourceBuilder(con);
			TableIdentifier tbl = new TableIdentifier("ORDERS");
			String sql = builder.getTableSource(tbl, false, true);
//			System.out.println(sql);
			assertTrue(sql.contains("FOREIGN KEY (CUSTOMER_ID)"));
			assertTrue(sql.contains("REFERENCES CUSTOMERS (ID)"));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
			ConnectionMgr.getInstance().clearProfiles();
		}

	}

	@Test
	public void testGeneratePKName()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		boolean oldProp = DbExplorerSettings.getAutoGeneratePKName();
		DbExplorerSettings.setAutoGeneratePKName(true);
		try
		{
			TableIdentifier tbl = new TableIdentifier("OTHER.PERSON");
			TableSourceBuilder builder = new TableSourceBuilder(con);
			PkDefinition pk = new PkDefinition(CollectionUtil.arrayList("ID"));
			String sql = builder.getPkSource(tbl, pk, false).toString();
//			System.out.println(sql);
			assertTrue(sql.indexOf("ADD CONSTRAINT pk_person") > -1);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
			ConnectionMgr.getInstance().clearProfiles();
			DbExplorerSettings.setAutoGeneratePKName(oldProp);
		}
	}

}
