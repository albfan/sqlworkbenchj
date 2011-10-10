/*
 * ColumnDropperTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.sql.ScriptParser;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnDropperTest
	extends WbTestCase
{
	public ColumnDropperTest()
	{
		super("ColumnDropperTest");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}


	@Test
	public void testDropObjects()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		TestUtil.executeScript(con,
			"create table person (nr integer, firstname varchar(20), lastname varchar(20), dummy1 integer, dummy2 date);\n" +
			"commit;\n");
		con.commit();

		TableIdentifier table = con.getMetadata().findTable(new TableIdentifier("PERSON"));
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
		cols.add(new ColumnIdentifier("DUMMY1"));
		cols.add(new ColumnIdentifier("DUMMY2"));

		ColumnDropper dropper = new ColumnDropper(con, table, cols);
		String sql = dropper.getScript().toString();

		assertNotNull(sql);
		ScriptParser p = new ScriptParser(sql.trim());
		p.setReturnStartingWhitespace(false);
		assertEquals(4, p.getSize());

		assertEquals("ALTER TABLE PERSON DROP COLUMN DUMMY1", p.getCommand(0).trim());
		assertEquals("COMMIT", p.getCommand(1).trim());
		assertEquals("ALTER TABLE PERSON DROP COLUMN DUMMY2", p.getCommand(2).trim());
		assertEquals("COMMIT", p.getCommand(3).trim());

		dropper.dropObjects();

		List<ColumnIdentifier> tableCols = con.getMetadata().getTableColumns(table);
		assertEquals(3, tableCols.size());
		assertEquals("NR", tableCols.get(0).getColumnName());
		assertEquals("FIRSTNAME", tableCols.get(1).getColumnName());
		assertEquals("LASTNAME", tableCols.get(2).getColumnName());
	}
}
