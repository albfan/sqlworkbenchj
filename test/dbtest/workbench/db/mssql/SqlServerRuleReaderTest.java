/*
 * SqlServerRuleReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;


import java.sql.SQLException;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerRuleReaderTest
	extends WbTestCase
{
	public SqlServerRuleReaderTest()
	{
		super("SqlServerRuleReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("SqlServerRuleReaderTest");
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();

		if (conn == null) return;
		SQLServerTestUtil.dropAllObjects(conn);

		TestUtil.executeScript(conn,
			"CREATE rule positive_value as @value > 0;\n" +
			"COMMIT;\n"
		);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		if (con == null) return;
		SQLServerTestUtil.dropAllObjects(con);
		ConnectionMgr.getInstance().disconnect(con);
	}

	@Test
	public void testGetRuleList()
		throws SQLException
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		if (con == null) return;
		try
		{
			List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] { "RULE" } );
			assertEquals(1, objects.size());
			TableIdentifier tbl = objects.get(0);
			assertEquals("RULE", tbl.getType());
			CharSequence source = tbl.getSource(con);
			assertNotNull(source);
			assertEquals("CREATE rule positive_value as @value > 0;", source.toString().trim());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnect(con);
		}
	}

	@Test
	public void testHandlesType()
	{
		SqlServerRuleReader reader = new SqlServerRuleReader();
		assertTrue(reader.handlesType("RULE"));
		assertTrue(reader.handlesType(new String[] {"TABLE", "RULE"}));
	}

	@Test
	public void testSupportedTypes()
	{
		SqlServerRuleReader reader = new SqlServerRuleReader();
		List<String> types = reader.supportedTypes();
		assertTrue(types.contains("RULE"));
	}

}
