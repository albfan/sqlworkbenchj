/*
 * SqlServerSynonymReaderTest
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
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerSynonymReaderTest
	extends WbTestCase
{

	public SqlServerSynonymReaderTest()
	{
		super("SqlServerSynonymReaderTest");
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
			"create table person (id integer);\n " +
			"create synonym sperson for person;\n " +
			"commit;\n";
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
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		SynonymReader reader = conn.getMetadata().getSynonymReader();
		assertNotNull(reader);
		List<TableIdentifier> syns = reader.getSynonymList(conn, "dbo", null);
		assertNotNull(syns);
		assertEquals(1, syns.size());
		assertEquals("sperson", syns.get(0).getObjectName());

		String source = syns.get(0).getSource(conn).toString().trim();
		String expected =
				"CREATE SYNONYM sperson\n" +
				"   FOR wbjunit.dbo.person;";
		assertEquals(expected, source);
		TableIdentifier table = conn.getMetadata().getSynonymTable(syns.get(0));
		assertNotNull(table);
		assertEquals("person", table.getTableName());
	}
}
