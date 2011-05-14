/*
 * SqlServerColumnEnhancerTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import java.sql.SQLException;
import java.util.List;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerColumnEnhancerTest
	extends WbTestCase
{

	public SqlServerColumnEnhancerTest()
	{
		super("SqlServerColumnEnhancerTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("SqlServerProcedureReaderTest");
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		SQLServerTestUtil.dropAllObjects(conn);
		String sql =
				"create table sales \n" +
				"( \n" +
				"   pieces integer, \n" +
				"   single_price numeric(19,2), \n" +
				"   total_price as (pieces * single_price), \n" +
				"   avg_price as (single_price / pieces) persisted \n" +
				")";
		TestUtil.executeScript(conn, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@Test
	public void testEnhancer()
		throws SQLException
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;

		TableDefinition def = conn.getMetadata().getTableDefinition(new TableIdentifier("sales"));
		assertNotNull(def);
		List<ColumnIdentifier> cols = def.getColumns();
		assertEquals(4, cols.size());
		ColumnIdentifier total = cols.get(2);
		assertEquals("total_price", total.getColumnName());
		assertEquals("AS ([pieces]*[single_price])", total.getComputedColumnExpression());

		ColumnIdentifier avg = cols.get(3);
		assertEquals("avg_price", avg.getColumnName());
		assertEquals("AS ([single_price]/[pieces]) PERSISTED", avg.getComputedColumnExpression());
	}

}
