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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.util.FileUtil;

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
		Collection<String> result = helper.getSystemFunctions();
		assertTrue(result.size() > 0);
	}
	
	public void testCustomKeywords()
	{
		PrintWriter out = null;
		try
		{
			
			File custom = new File(util.getBaseDir(), "keywords.wb");
			out = new PrintWriter(new FileWriter(custom));
			out.println("ARTHUR");
			out.println("DENT");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			fail(ex.getMessage());
		}
		finally
		{
			FileUtil.closeQuitely(out);
		}
		
		SqlKeywordHelper helper = new SqlKeywordHelper();
		Collection<String> result = helper.getKeywords();
		assertTrue(result.size() > 0);
		assertTrue(result.contains("ARTHUR"));
		assertTrue(result.contains("DENT"));
	}
	
}
