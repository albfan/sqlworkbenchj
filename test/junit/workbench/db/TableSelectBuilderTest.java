/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */
package workbench.db;

import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableSelectBuilderTest
	extends TestCase
{

	public TableSelectBuilderTest(String testName)
	{
		super(testName);
	}

	public void testBuildSelect()
		throws Exception
	{
		Settings.getInstance();
		
		TestUtil util = new TestUtil("SelectBuilder");
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
	
	public void testCleanDataType()
	{
		TableSelectBuilder builder = new TableSelectBuilder(null);
		String type = builder.cleanDataType("varchar");
		assertEquals("varchar", type);
		type = builder.cleanDataType("varchar(10)");
		assertEquals("varchar", type);
	}
}
