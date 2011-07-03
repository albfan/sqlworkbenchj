/*
 * PostgresExplainAnalyzerTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.completion;

import workbench.util.CollectionUtil;
import java.util.List;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresExplainAnalyzerTest
	extends WbTestCase
{

	public PostgresExplainAnalyzerTest()
	{
		super("PostgresExplainAnalyzerTest");
	}

	@Test
	public void testGetExplainedStatement()
	{
		String sql = "explain verbose select * from person;";
		PostgresExplainAnalyzer explain = new PostgresExplainAnalyzer(null, sql, 2);
		assertEquals("explain verbose", explain.getExplainSql());
		assertEquals("select * from person;", explain.getExplainedStatement().trim());
	}

	@Test
	public void test9xOptions()
	{
		String sql = "explain ( ) select * from person;";
		PostgresExplainAnalyzer explain = new PostgresExplainAnalyzer(null, sql, 9);
		explain.checkContext();
		List options = explain.getData();
		assertNotNull(options);
		List<String> expectedOptions = CollectionUtil.arrayList("analyze", "verbose", "format", "buffers", "costs");
		for (String option : expectedOptions)
		{
			assertTrue(options.contains(option));
		}

		sql = "explain (analyze true, ) select * from person;";
		explain = new PostgresExplainAnalyzer(null, sql, 22);
		explain.checkContext();
		options = explain.getData();
		assertNotNull(options);
		expectedOptions = CollectionUtil.arrayList("verbose", "format", "buffers", "costs");
		for (String option : expectedOptions)
		{
			assertTrue(options.contains(option));
		}

		sql = "explain (analyze true, buffers true) select * from person;";
		explain = new PostgresExplainAnalyzer(null, sql, 22);
		explain.checkContext();
		options = explain.getData();
		assertNotNull(options);
		expectedOptions = CollectionUtil.arrayList("verbose", "format", "costs");
		for (String option : expectedOptions)
		{
			assertTrue(options.contains(option));
		}

		sql = "explain (analyze , buffers true) select * from person;";
		explain = new PostgresExplainAnalyzer(null, sql, 17);
		explain.checkContext();
		options = explain.getData();
		assertNotNull(options);
		expectedOptions = CollectionUtil.arrayList("false", "true");
		for (String option : expectedOptions)
		{
			assertTrue(options.contains(option));
		}

		sql = "explain (analyze , buffers true, ) select * from person;";
		explain = new PostgresExplainAnalyzer(null, sql, 32);
		explain.checkContext();
		options = explain.getData();
		assertNotNull(options);
		expectedOptions = CollectionUtil.arrayList("verbose", "format", "costs");
		for (String option : expectedOptions)
		{
			assertTrue(options.contains(option));
		}
	}

	@Test
	public void test8xOptions()
	{
		String sql = "explain verbose select * from person;";
		PostgresExplainAnalyzer explain = new PostgresExplainAnalyzer(null, sql, 8);
		explain.checkContext();
		List options = explain.getData();
		assertNotNull(options);
		assertEquals(1, options.size());
		assertEquals("analyze", options.get(0));

		sql = "explain select * from person;";
		explain = new PostgresExplainAnalyzer(null, sql, 8);
		explain.checkContext();
		options = explain.getData();
		assertNotNull(options);
		assertEquals(1, options.size());
		assertEquals("analyze", options.get(0));

		sql = "explain analyze select * from person;";
		explain = new PostgresExplainAnalyzer(null, sql, 16);
		explain.checkContext();
		options = explain.getData();
		assertNotNull(options);
		assertEquals(1, options.size());
		assertEquals("verbose", options.get(0));

	}
}