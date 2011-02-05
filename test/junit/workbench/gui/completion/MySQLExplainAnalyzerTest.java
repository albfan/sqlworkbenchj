/*
 * MySQLExplainAnalyzerTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.completion;

import java.util.List;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLExplainAnalyzerTest
	extends WbTestCase
{

	public MySQLExplainAnalyzerTest()
	{
		super("MySQLExplainAnalyzerTest");
	}

	@Test
	public void testCheckContext()
	{
		String sql = "explain ";
		MySQLExplainAnalyzer analyzer = new MySQLExplainAnalyzer(null, sql, 8);
		assertNull(analyzer.getExplainedStatement());

		analyzer.checkContext();
		List options = analyzer.getData();
		assertNotNull(options);
		assertEquals(2, options.size());

		sql = "explain extended ";
		analyzer = new MySQLExplainAnalyzer(null, sql, sql.length() - 1);
		analyzer.checkContext();
		options = analyzer.getData();
		assertNotNull(options);
		assertEquals(1, options.size());
		assertEquals("PARTITIONS", options.get(0));

		sql = "explain extended ";
		analyzer = new MySQLExplainAnalyzer(null, sql, 8);
		analyzer.checkContext();
		options = analyzer.getData();
		assertNotNull(options);
		assertEquals(1, options.size());
		assertEquals("PARTITIONS", options.get(0));

		sql = "explain partitions ";
		analyzer = new MySQLExplainAnalyzer(null, sql, 8);
		analyzer.checkContext();
		options = analyzer.getData();
		assertNotNull(options);
		assertEquals(1, options.size());
		assertEquals("EXTENDED", options.get(0));

		sql = "explain extended partitions ";
		analyzer = new MySQLExplainAnalyzer(null, sql, 2);
		analyzer.checkContext();
		options = analyzer.getData();
		assertNotNull(options);
		assertEquals(0, options.size());
	}

	@Test
	public void testGetExplainedStatement()
	{
		String sql = "explain select * from person";
		MySQLExplainAnalyzer analyzer = new MySQLExplainAnalyzer(null, sql, 1);
		assertNotNull(analyzer.getExplainedStatement());

		sql = "explain ";
		analyzer = new MySQLExplainAnalyzer(null, sql, 1);
		assertNull(analyzer.getExplainedStatement());
	}
}