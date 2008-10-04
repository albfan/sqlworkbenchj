/*
 * SqlKeywordHelperTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.syntax;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Set;
import workbench.TestUtil;
import workbench.WbTestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlKeywordHelperTest
	extends WbTestCase
{

	private TestUtil util;

	public SqlKeywordHelperTest(String testName)
	{
		super(testName);
		util = getTestUtil();
	}

	public void testGetKeywords()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		Collection<String> result = helper.getKeywords();
		assertTrue(result.size() > 0);
		assertTrue(result.contains("SELECT"));
		assertTrue(result.contains("DELETE"));
		assertTrue(result.contains("UPDATE"));
		assertTrue(result.contains("INSERT"));
		assertTrue(result.contains("CREATE"));
	}

	public void testOracleKeywords()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper("oracle");
		Collection<String> keywords = helper.getKeywords();
		assertTrue(keywords.contains("ABORT"));
		assertTrue(keywords.contains("IDENTIFIED"));
		assertTrue(keywords.contains("EXCEPTION"));
	}

	public void testGetDataTypes()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		Collection<String> result = helper.getDataTypes();
		assertTrue(result.size() > 0);
	}

	public void testGetOperators()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		Collection<String> result = helper.getOperators();
		assertTrue(result.size() > 0);
	}

	public void testGetSystemFunctions()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		Set<String> result = helper.getSqlFunctions();
		assertTrue(result.size() > 0);
	}

	public void testCustomKeywords()
		throws IOException
	{
		File custom = new File(util.getBaseDir(), "keywords.wb");
		TestUtil.writeFile(custom, "ARTHUR\nDENT");

		File custom2 = new File(util.getBaseDir(), "myid.keywords.wb");
		TestUtil.writeFile(custom2, "ZAPHOD\nBEBLEBROX");

		File custom3 = new File(util.getBaseDir(), "testid.keywords.wb");
		TestUtil.writeFile(custom3, " FORD\nPREFECT ");

		SqlKeywordHelper helper = new SqlKeywordHelper("testid");
		Collection<String> result = helper.getKeywords();
		assertTrue(result.size() > 0);
		assertTrue(result.contains("ARTHUR"));
		assertTrue(result.contains("DENT"));

		assertTrue(result.contains("FORD"));
		assertTrue(result.contains("PREFECT"));

		assertFalse(result.contains("ZAPHOD"));
		assertFalse(result.contains("BEBLEBROX"));

		custom.delete();
		custom2.delete();
		custom3.delete();
	}

}
