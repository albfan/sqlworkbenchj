/*
 * ResultNameParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultNameParserTest
	extends TestCase
{

	public ResultNameParserTest(String testName)
	{
		super(testName);
	}

	public void testGetResultName()
	{
		String sql = "/* test select */\nSELECT * FROM dummy;";
		ResultNameParser p = new ResultNameParser();
		String name = p.getResultName(sql);
		assertNull(name);

		sql = "/**@wbresult all rows*/\nSELECT * FROM dummy;";
		name = p.getResultName(sql);
		assertEquals("all rows", name);

		sql = "/* @wbresult result for my select\nanother line */\nSELECT * FROM dummy;";
		name = p.getResultName(sql);
		assertEquals("result for my select", name);

		sql = "-- @wbresult test select\nSELECT * FROM dummy;";
		name = p.getResultName(sql);
		assertEquals("test select", name);

		sql = "-- @wbresulttest select\nSELECT * FROM dummy;";
		name = p.getResultName(sql);
		assertNull(name);

		sql = "/*@wbresult mystuff\tselected\r\nanother line */\nSELECT * FROM dummy;";
		name = p.getResultName(sql);
		assertEquals("mystuff\tselected", name);
	}
}
