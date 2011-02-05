/*
 * ExplainPostgresTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.gui.completion.BaseAnalyzer;
import workbench.gui.completion.SelectAllMarker;
import workbench.gui.completion.StatementContext;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ExplainPostgresTest
	extends WbTestCase
{

	private static final String TEST_SCHEMA = "explaintest";

	public ExplainPostgresTest()
	{
		super("ExplainPostgresTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_SCHEMA);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"CREATE TABLE person (id integer, name varchar(100));\n" +
			"COMMIT; \n");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_SCHEMA);
	}

	@Test
	public void testExplain()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		String sql = "explain analyze select  from person";
		StatementContext context = new StatementContext(con, sql, sql.indexOf("select") + 7);
		BaseAnalyzer ba = context.getAnalyzer();
		List columns = context.getData();
		assertNotNull(columns);
		assertEquals(3, columns.size()); // two columns and the (All) entry
		assertTrue(columns.get(0) instanceof SelectAllMarker);
		ColumnIdentifier id = (ColumnIdentifier)columns.get(1);
		assertEquals("id", id.getColumnName());
		ColumnIdentifier name = (ColumnIdentifier)columns.get(2);
		assertEquals("name", name.getColumnName());

		// The cursor position used by the nested SELECT analyzer for the explain
		// must be the same as the position for a "regular" analyzer
		sql = "select  from person";
		context = new StatementContext(con, sql, sql.indexOf("select") + 7);
		BaseAnalyzer select = context.getAnalyzer();
		assertEquals(ba.getCursorPosition(), select.getCursorPosition());
	}
}
