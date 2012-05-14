/*
 * Db2CompletionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.ibm;


import java.sql.SQLException;

import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2CompletionTest
	extends WbTestCase
{
	public Db2CompletionTest()
	{
		super("Db2CompletionTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		Db2TestUtil.initTestCase();

		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;
		String schema = Db2TestUtil.getSchemaName();

		String sql =
			"CREATE TABLE " + schema + ".data1 (pid integer not null primary key, info varchar(100));\n"  +
			"CREATE TABLE " + schema + ".data2 (did integer not null primary key, info varchar(100));\n"  +
			"commit;\n";
		TestUtil.executeScript(con, sql, true);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		Db2TestUtil.cleanUpTestCase();
	}

	@Test
	public void testSelectCompletion()
		throws SQLException
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		Set<TableIdentifier> tables = con.getObjectCache().getTables(Db2TestUtil.getSchemaName().toLowerCase());
		assertEquals(2, tables.size());
		assertTrue(tables.contains(new TableIdentifier(Db2TestUtil.getSchemaName(), "DATA1")));
		assertTrue(tables.contains(new TableIdentifier(Db2TestUtil.getSchemaName(), "DATA2")));

		tables = con.getObjectCache().getTables(Db2TestUtil.getSchemaName());
		assertEquals(2, tables.size());
		assertTrue(tables.contains(new TableIdentifier(Db2TestUtil.getSchemaName(), "DATA1")));
		assertTrue(tables.contains(new TableIdentifier(Db2TestUtil.getSchemaName(), "DATA2")));
	}

}
