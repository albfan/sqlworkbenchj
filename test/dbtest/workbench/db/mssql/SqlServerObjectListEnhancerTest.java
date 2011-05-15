/*
 * SqlServerObjectListEnhancerTest
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
import java.sql.Statement;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ColumnChanger;
import workbench.db.ColumnIdentifier;
import workbench.db.TableCommentReader;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerObjectListEnhancerTest
	extends WbTestCase
{

	public SqlServerObjectListEnhancerTest()
	{
		super("SqlServerObjectListEnhancerTest");
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
				"create table person \n" +
				"( \n" +
				"   id integer, \n" +
				"   firstname varchar(100), \n" +
				"   lastname varchar(100) \n" +
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
	public void testRemarks()
		throws SQLException
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		Settings.getInstance().setProperty("workbench.db.microsoft_sql_server.remarks.object.retrieve", true);
		TableIdentifier sales = conn.getMetadata().findTable(new TableIdentifier("person"));

		sales.setComment("One person");
		TableCommentReader reader = new TableCommentReader();
		String sql = reader.getTableCommentSql(conn, sales);
		Statement stmt = null;
		try
		{
			stmt = conn.createStatement();
			stmt.execute(sql);
			conn.commit();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw e;
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
		List<TableIdentifier> tables = conn.getMetadata().getTableList("person", "dbo");
		assertEquals(1, tables.size());
		sales = tables.get(0);
		assertEquals("One person", sales.getComment());
		sales = conn.getMetadata().findTable(new TableIdentifier("person"));
		assertEquals("One person", sales.getComment());
	}

}
