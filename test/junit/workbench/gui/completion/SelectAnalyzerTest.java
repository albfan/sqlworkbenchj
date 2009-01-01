/*
 * SelectAnalyzerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class SelectAnalyzerTest extends TestCase
{

	public SelectAnalyzerTest(String testName)
	{
		super(testName);
	}

	protected void setUp() throws Exception
	{
	}

	protected void tearDown() throws Exception
	{
	}

	public void testAnalyzer()
	{
		String sql = "SELECT a.att1\n      ,a.\nFROM   adam   a";
		SelectAnalyzer analyzer = new SelectAnalyzer(null, sql, 23);
		String quali = analyzer.getQualifierLeftOfCursor();
		assertEquals("Wrong qualifier detected", "a", quali);
	}

}
