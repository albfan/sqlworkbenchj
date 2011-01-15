/*
 * ColumnDropperTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.TestUtil;
import workbench.sql.ScriptParser;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnDropperTest
{
	public ColumnDropperTest()
	{
	}

	@Test
	public void testDropObjects()
		throws Exception
	{
		TestUtil util = new TestUtil("dropColumn");
		WbConnection con = util.getConnection();

		try
		{
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table person (nr integer, firstname varchar(20), lastname varchar(20), dummy1 integer, dummy2 date)");
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
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
