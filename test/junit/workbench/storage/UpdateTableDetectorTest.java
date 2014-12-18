/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.derby.DerbyTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class UpdateTableDetectorTest
	extends WbTestCase
{

	public UpdateTableDetectorTest()
	{
		super("UpdateTableDetectorTest");
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testSynonyms()
		throws Exception
	{
		try
		{
			TestUtil util = getTestUtil();
			WbConnection conn = DerbyTestUtil.getDerbyConnection(util.getBaseDir());

			TestUtil.executeScript(conn,
				"create table person (id integer primary key, name varchar(20) not null);\n" +
				"create synonym psyn for person;\n"
			);
			String sql = "select id, name from psyn";
			ResultInfo info = null;
			try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql))
			{
				info = new ResultInfo(rs.getMetaData(), conn);
			}

			info.getColumn(0).setIsPkColumn(false);

			UpdateTableDetector detector = new UpdateTableDetector(conn);
			detector.setCheckPKOnly(false);
			TableIdentifier toCheck = new TableIdentifier("psyn");
			detector.checkUpdateTable(toCheck, info);

			TableIdentifier tbl = detector.getUpdateTable();
			assertEquals("PSYN", tbl.getTableName());
			assertTrue(info.getColumn(0).isPkColumn());
			assertFalse(info.getColumn(1).isPkColumn());

			resetInfo(info);

			detector.setCheckPKOnly(true);
			detector.checkUpdateTable(toCheck, info);
			assertEquals("PSYN", tbl.getTableName());
			assertTrue(info.getColumn(0).isPkColumn());
			assertFalse(info.getColumn(1).isPkColumn());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testSpecialName()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		TestUtil.executeScript(con,
			"create table \"FOO.BAR\" (id integer primary key, somedata varchar(50));\n" +
			"commit;");

		String sql = "select id, somedata from \"FOO.BAR\"";
		ResultInfo info = null;
		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			info = new ResultInfo(rs.getMetaData(), con);
		}

		UpdateTableDetector detector = new UpdateTableDetector(con);
		detector.setCheckPKOnly(false);

		TableIdentifier toCheck = new TableIdentifier("\"FOO.BAR\"");
		detector.checkUpdateTable(toCheck, info);

		TableIdentifier tbl = detector.getUpdateTable();
		assertEquals("FOO.BAR", tbl.getTableName());
		assertTrue(info.getColumn(0).isPkColumn());
		assertFalse(info.getColumn(1).isPkColumn());

		resetInfo(info);

		detector.setCheckPKOnly(true);
		detector.checkUpdateTable(toCheck, info);
		tbl = detector.getUpdateTable();

		assertEquals("FOO.BAR", tbl.getTableName());
		assertTrue(info.getColumn(0).isPkColumn());
		assertFalse(info.getColumn(1).isPkColumn());
	}

	@Test
	public void testMissingPKColumns()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		TestUtil.executeScript(con,
			"create table foobar (id1 integer not null, id2 integer not null, somedata varchar(50), primary key (id1, id2));\n" +
			"commit;");

		String sql = "select id1, somedata from FOOBAR";
		ResultInfo info = null;
		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			info = new ResultInfo(rs.getMetaData(), con);
		}

		UpdateTableDetector detector = new UpdateTableDetector(con);
		detector.setCheckPKOnly(false);

		TableIdentifier toCheck = new TableIdentifier("foobar");
		detector.checkUpdateTable(toCheck, info);

		TableIdentifier tbl = detector.getUpdateTable();
		assertEquals("FOOBAR", tbl.getTableName());
		assertTrue(info.getColumn(0).isPkColumn());
		assertFalse(info.getColumn(1).isPkColumn());
		List<ColumnIdentifier> cols = detector.getMissingPkColumns();
		assertNotNull(cols);
		assertEquals(1, cols.size());
		assertEquals("ID2", cols.get(0).getColumnName());
	}

	@Test
	public void testMultipleSchemas()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		TestUtil.executeScript(con,
			"create schema one;\n" +
			"create schema two;\n" +
			"create table public.foobar (id integer not null primary key, somedata varchar(50));\n" +
			"create table one.foobar (id_one integer not null primary key, somedata varchar(50));\n" +
			"create table two.foobar (id_two integer not null primary key, somedata varchar(50));\n" +
			"commit;");

		String sql = "select id, somedata from foobar";
		ResultInfo info = null;
		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			info = new ResultInfo(rs.getMetaData(), con);
		}

		UpdateTableDetector detector = new UpdateTableDetector(con);
		detector.setCheckPKOnly(false);

		TableIdentifier toCheck = new TableIdentifier("foobar");
		detector.checkUpdateTable(toCheck, info);
		TableIdentifier tbl = detector.getUpdateTable();
		assertEquals("FOOBAR", tbl.getTableName());
		assertEquals("PUBLIC", tbl.getSchema());
		assertTrue(CollectionUtil.isEmpty(detector.getMissingPkColumns()));
	}

	private void resetInfo(ResultInfo info)
	{
		for (ColumnIdentifier col : info.getColumns())
		{
			col.setIsPkColumn(false);
			col.setIsNullable(true);
		}
	}

}
