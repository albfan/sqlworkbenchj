/*
 * PostgresTableSourceBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTableSourceBuilderTest
	extends WbTestCase
{
	private static final String TEST_SCHEMA = "sourcebuilder";

	public PostgresTableSourceBuilderTest()
	{
		super("PostgresTableSourceBuilder");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_SCHEMA);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;
		TestUtil.executeScript(con,
			"create table base_table (id integer, some_data varchar(100));\n" +
			"alter table base_table add constraint uc_some_data unique (some_data); \n" +
			"create table child_table (other_data varchar(100)) inherits (base_table);\n" +
			"create type order_status_type as enum ('new', 'open', 'closed');\n" +
			"create domain product_price as integer check (value > 0);\n" +
			"create table more_data (id integer, order_status order_status_type, price product_price);\n" +
			"create sequence data_seq owned by more_data.id;\n" +
			"commit;\n");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testTableOptions()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TableIdentifier tbl = new TableIdentifier(TEST_SCHEMA, "child_table");
		String sql = tbl.getSource(con).toString();
		assertTrue(sql.contains("INHERITS (base_table)"));

		TestUtil.executeScript(con, "create table child_fill (foo_data text) inherits (base_table) with (fillfactor=40);");
		tbl = con.getMetadata().findTable(new TableIdentifier("child_fill"));
		String source = tbl.getSource(con).toString();
		assertTrue(source.contains("INHERITS (base_table)\nWITH (fillfactor=40)"));
	}

	@Test
	public void testColumnOptions()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TableIdentifier tbl = new TableIdentifier(TEST_SCHEMA, "more_data");
		String sql = tbl.getSource(con).toString();
		assertTrue(sql.contains(" enum 'order_status_type':"));
		assertTrue(sql.contains(" domain 'product_price': integer CHECK (VALUE > 0)"));
		assertTrue(sql.contains("sequence sourcebuilder.data_seq"));
	}

	@Test
	public void testUniqueConstraint()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TableIdentifier tbl = new TableIdentifier(TEST_SCHEMA, "base_table");
		String sql = tbl.getSource(con).toString();
		assertTrue(sql.contains("ADD CONSTRAINT uc_some_data UNIQUE (some_data);"));
	}

	@Test
	public void testUnloggedTables()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;
		if (!JdbcUtils.hasMinimumServerVersion(con, "9.1")) return;
		TestUtil.executeScript(con, "create unlogged table no_crash_safe (id integer, some_data varchar(100));");
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("no_crash_safe"));
		String source = tbl.getSource(con).toString();
		assertTrue(source.indexOf("CREATE UNLOGGED TABLE") > -1);
	}

	@Test
	public void testConfigSettings()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;
		TestUtil.executeScript(con, "create table foo_fill (id integer, some_data varchar(100)) with (fillfactor=40);");
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("foo_fill"));
		String source = tbl.getSource(con).toString();
		assertTrue(source.contains("fillfactor=40"));
	}

}
