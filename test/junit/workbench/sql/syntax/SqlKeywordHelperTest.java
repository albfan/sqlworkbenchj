/*
 * SqlKeywordHelperTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.sql.syntax;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import workbench.TestUtil;
import workbench.WbTestCase;

import static org.junit.Assert.*;
import org.junit.Test;


/**
 *
 * @author Thomas Kellerer
 */
public class SqlKeywordHelperTest
	extends WbTestCase
{
	private TestUtil util;

	public SqlKeywordHelperTest()
	{
		super("SqlKeywordHelperTest");
		util = getTestUtil();
	}


	@Test
	public void testStandardKeywords()
	{
		assertTrue(SqlKeywordHelper.getDefaultReservedWords().contains("default"));
		assertTrue(SqlKeywordHelper.getDefaultReservedWords().contains("by"));
	}

	@Test
	public void testGetKeywords()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		Collection<String> result = helper.getKeywords();
		assertTrue(result.contains("SELECT"));
		assertTrue(result.contains("DELETE"));
		assertTrue(result.contains("UPDATE"));
		assertTrue(result.contains("INSERT"));
		assertTrue(result.contains("CREATE"));
		assertTrue(result.contains("KEY"));
	}

	@Test
	public void testOracleKeywords()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper("oracle");
		Collection<String> keywords = helper.getKeywords();
		assertTrue(keywords.contains("ABORT"));
		assertTrue(keywords.contains("IDENTIFIED"));
		assertTrue(keywords.contains("EXCEPTION"));
		Set<String> functions = helper.getSqlFunctions();
		assertTrue(functions.contains("nvl"));
		assertTrue(functions.contains("decode"));
		assertTrue(functions.contains("dense_rank"));
	}

	@Test
	public void testPostgresKeywords()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper("postgresql");
		Collection<String> keywords = helper.getKeywords();
		assertTrue(keywords.contains("EXCEPT"));
		Set<String> functions = helper.getSqlFunctions();
		assertTrue(functions.contains("coalesce"));
		assertTrue(functions.contains("dense_rank"));
		assertTrue(functions.contains("row_number"));
		assertTrue(functions.contains("REGEXP_MATCHES"));
		assertTrue(functions.contains("REGEXP_REPLACE"));
		assertTrue(functions.contains("REGEXP_SPLIT_TO_ARRAY"));
		assertTrue(functions.contains("string_agg"));
		assertTrue(functions.contains("UNNEST"));
		assertTrue(functions.contains("generate_series"));
	}

	@Test
	public void testSqlServerKeywords()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper("microsoft_sql_server");
		Collection<String> keywords = helper.getKeywords();
		assertTrue(keywords.contains("OPEN"));
	}

	@Test
	public void testGetDataTypes()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		Collection<String> result = helper.getDataTypes();
		assertTrue(result.size() > 0);
	}

	@Test
	public void testGetOperators()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		Collection<String> result = helper.getOperators();
		assertTrue(result.size() > 0);
	}

	@Test
	public void testGetSystemFunctions()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		Set<String> result = helper.getSqlFunctions();
		assertTrue(result.size() > 0);
		assertTrue(result.contains("row_number"));
		assertTrue(result.contains("coalesce"));
	}

	@Test
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

	@Test
	public void testRemoveKeywords()
		throws Exception
	{
		File custom = new File(util.getBaseDir(), "myid.reserved_words.wb");
		TestUtil.writeFile(custom, "FOO\n-SELECT");
		SqlKeywordHelper helper = new SqlKeywordHelper("myid");
		Set<String> reserved = helper.getReservedWords();
		assertTrue(reserved.contains("FOO"));
		assertFalse(reserved.contains("SELECT"));
		assertFalse(reserved.contains("-SELECT"));
	}

}
