/*
 * TableSelectBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.TestUtil;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class TableSelectBuilderTest
	extends WbTestCase
{

	public TableSelectBuilderTest()
	{
		super("TableSelectBuilderTest");
	}

	@Test
	public void testBuildSelect()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		TestUtil.executeScript(con, "create table person (nr integer, firstname varchar(20), lastname varchar(20))");

		DbSettings dbconfig = con.getDbSettings();
		TableSelectBuilder builder = new TableSelectBuilder(con);
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("person"));

		String sql = builder.getSelectForTable(tbl);
		String expected = "SELECT NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\n" +
			"FROM PERSON";
		assertEquals(expected, sql);

		dbconfig.setDataTypeExpression("varchar", "upper(" + TableSelectBuilder.COLUMN_PLACEHOLDER + ")");

		sql = builder.getSelectForTable(tbl);
		expected = "SELECT NR,\n" +
			"       upper(FIRSTNAME),\n" +
			"       upper(LASTNAME)\n" +
			"FROM PERSON";
		assertEquals(expected, sql);

		dbconfig.setDataTypeExpression("varchar", null);

		// test invalid expression
		dbconfig.setDataTypeExpression("integer", "abs()");
		sql = builder.getSelectForTable(tbl);
		expected = "SELECT NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\n" +
			"FROM PERSON";
		assertEquals(expected, sql);
	}

	@Test
	public void testCleanDataType()
	{
		TableSelectBuilder builder = new TableSelectBuilder(null);
		String type = builder.cleanDataType("varchar");
		assertEquals("varchar", type);
		type = builder.cleanDataType("varchar(10)");
		assertEquals("varchar", type);
	}
}
