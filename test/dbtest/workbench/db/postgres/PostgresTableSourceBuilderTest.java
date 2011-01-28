/*
 * PostgresTableSourceBuilderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
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
		TestUtil.executeScript(con, "create table base_table (id integer, some_data varchar(100));\n" +
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
		PostgresTestUtil.cleanUpTestCase(TEST_SCHEMA);
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
	}

	@Test
	public void testColumnOptions()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TableIdentifier tbl = new TableIdentifier(TEST_SCHEMA, "more_data");
		String sql = tbl.getSource(con).toString();
		System.out.println(sql);
		assertTrue(sql.contains(" enum 'order_status_type': 'new','open','closed'"));
		assertTrue(sql.contains(" domain 'product_price': integer CHECK (VALUE > 0)"));
		assertTrue(sql.contains("sequence sourcebuilder.data_seq"));
	}

}
