/*
 * SqlServerProcedureReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import static org.junit.Assert.*;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerProcedureReaderTest
	extends WbTestCase
{

	public SqlServerProcedureReaderTest()
	{
		super("SqlServerProcedureReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("SqlServerProcedureReaderTest");
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		SQLServerTestUtil.dropAllObjects(conn);
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
	public void testGetProcedures()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;

		String sql =
			"create procedure answer(@value integer output)\n" +
			"as \n" +
			" set @value = 42;\n";

		TestUtil.executeScript(conn, sql);
		ProcedureReader reader = conn.getMetadata().getProcedureReader();
		assertTrue(reader instanceof SqlServerProcedureReader);
		List<ProcedureDefinition> procedureList = reader.getProcedureList(SQLServerTestUtil.DB_NAME, "dbo", null);
		assertNotNull(procedureList);
		assertEquals(1, procedureList.size());
		CharSequence source = procedureList.get(0).getSource(conn);
		assertNotNull(source);
		String sourceSql = source.toString();
		assertEquals(SqlUtil.trimSemicolon(sql), sourceSql);
	}
}
