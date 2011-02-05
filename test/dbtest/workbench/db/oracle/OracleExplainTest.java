/*
 * OracleExplainAnalyzerTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import workbench.db.TableIdentifier;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.gui.completion.BaseAnalyzer;
import workbench.gui.completion.OracleExplainAnalyzer;
import workbench.gui.completion.StatementContext;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleExplainTest
	extends WbTestCase
{

	public OracleExplainTest()
	{
		super("OracleExplainAnalyzerTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql =
			"create table some_table (id integer, some_data varchar(100));\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testRetrieveTables()
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		String sql = "explain plan into " + OracleTestUtil.SCHEMA_NAME + ". for select * from person";

		StatementContext context = new StatementContext(con, sql, sql.indexOf('.') + 1);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof OracleExplainAnalyzer);
		List tables = analyzer.getData();
		assertNotNull(tables);
		assertEquals(1, tables.size());
		TableIdentifier tbl = (TableIdentifier)tables.get(0);
		assertEquals("SOME_TABLE", tbl.getTableName());
	}
}