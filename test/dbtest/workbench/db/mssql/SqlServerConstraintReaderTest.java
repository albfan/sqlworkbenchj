/*
 * SqlServerConstraintReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerConstraintReaderTest
	extends WbTestCase
{

	public SqlServerConstraintReaderTest()
	{
		super("SqlServerConstraintReaderTest");
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
			"   constraint positive_amount check (pieces > 0) \n" +
			");\n" +
			"commit;\n" +
			"create table def_test ( id integer constraint inital_value default 1);\n" +
			"commit;";
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
	public void testReader()
		throws SQLException
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;

		TableIdentifier tbl = conn.getMetadata().findTable(new TableIdentifier("sales"));
		assertNotNull(tbl);
		String source = tbl.getSource(conn).toString();
		assertNotNull(source);
		assertTrue(source.indexOf("CHECK ([pieces]>(0))") > -1);
		assertTrue(source.indexOf("CONSTRAINT positive_amount") > -1);
	}

	@Test
	public void testDefaultConstraintName()
		throws SQLException
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;

		TableIdentifier tbl = conn.getMetadata().findTable(new TableIdentifier("def_test"));
		assertNotNull(tbl);
		String source = tbl.getSource(conn).toString();
		assertNotNull(source);
//		System.out.println(source);
		assertTrue(source.indexOf("CONSTRAINT inital_value") > -1);
	}

}
