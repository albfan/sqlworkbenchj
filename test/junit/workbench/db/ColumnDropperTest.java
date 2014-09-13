/*
 * ColumnDropperTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.sql.ScriptParser;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

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
		List<ColumnIdentifier> cols = new ArrayList<>();
		cols.add(new ColumnIdentifier("DUMMY1"));
		cols.add(new ColumnIdentifier("DUMMY2"));

		ColumnDropper dropper = new ColumnDropper(con, table, cols);
		String sql = dropper.getScript().toString();

		assertNotNull(sql);
		ScriptParser p = new ScriptParser(sql.trim());
		p.setReturnStartingWhitespace(false);
		assertEquals(2, p.getSize());

		assertEquals("ALTER TABLE PERSON DROP COLUMN DUMMY1", p.getCommand(0).trim());
		assertEquals("ALTER TABLE PERSON DROP COLUMN DUMMY2", p.getCommand(1).trim());

		dropper.dropObjects();

		List<ColumnIdentifier> tableCols = con.getMetadata().getTableColumns(table);
		assertEquals(3, tableCols.size());
		assertEquals("NR", tableCols.get(0).getColumnName());
		assertEquals("FIRSTNAME", tableCols.get(1).getColumnName());
		assertEquals("LASTNAME", tableCols.get(2).getColumnName());
	}
}
