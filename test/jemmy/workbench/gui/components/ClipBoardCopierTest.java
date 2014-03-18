/*
 * ClipBoardCopierTest.java
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
package workbench.gui.components;


import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.sql.Types;
import java.util.List;

import workbench.WbTestCase;
import workbench.resource.GuiSettings;

import workbench.db.ColumnIdentifier;
import workbench.db.IndexColumn;
import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.exporter.ExportType;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

import workbench.sql.ScriptParser;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ClipBoardCopierTest
	extends WbTestCase
{
	public ClipBoardCopierTest()
	{
		super("ClipBoardCopierTest");
	}

	@Before
	public void clearClipboard()
	{
		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection sel = new StringSelection("");
		clp.setContents(sel, sel);
	}

	private DataStore createDataStore()
	{
		ColumnIdentifier id = new ColumnIdentifier("id", Types.INTEGER);
		id.setIsPkColumn(true);
		ColumnIdentifier fname = new ColumnIdentifier("firstname", Types.VARCHAR);
		ColumnIdentifier lname = new ColumnIdentifier("lastname", Types.VARCHAR);
		ResultInfo info = new ResultInfo(new ColumnIdentifier[] {id, fname, lname});

		IndexColumn col = new IndexColumn("id", 1);
		PkDefinition pk = new PkDefinition("pk_person", CollectionUtil.arrayList(col));
		TableIdentifier tbl = new TableIdentifier("person");
		tbl.setPrimaryKey(pk);

		info.setUpdateTable(tbl);
		DataStore ds = new DataStore(info);
		int row = ds.addRow();
		ds.setValue(row, 0, new Integer(1));
		ds.setValue(row, 1, "Arthur");
		ds.setValue(row, 2, "Dent");

		row = ds.addRow();
		ds.setValue(row, 0, new Integer(2));
		ds.setValue(row, 1, "Ford");
		ds.setValue(row, 2, "Prefect");

		ds.setUpdateTableToBeUsed(tbl);
		return ds;
	}

	@Test
	public void testCopyDataToClipboard()
		throws Exception
	{
		ClipBoardCopier copier = new ClipBoardCopier(createDataStore());
		copier.copyDataToClipboard(true, false, false);
		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clp.getContents(copier);
		Object data = contents.getTransferData(DataFlavor.stringFlavor);
		assertNotNull(data);
		assertTrue(data instanceof String);
		List<String> lines = StringUtil.getLines((String)data);
		assertEquals(3, lines.size());
		assertEquals("id\tfirstname\tlastname", lines.get(0));
		assertEquals("1\tArthur\tDent", lines.get(1));
		assertEquals("2\tFord\tPrefect", lines.get(2));
	}

	@Test
	public void testCopyAsSqlInsert()
		throws Exception
	{
		try
		{
			GuiSettings.setDisplayNullString("<[NULL]>");
			DataStore ds = createDataStore();
			int row = ds.addRow();
			ds.setValue(row, 0, new Integer(1));
			ds.setValue(row, 1, "Marvin");
			ds.setValue(row, 2, null);

			ClipBoardCopier copier = new ClipBoardCopier(ds);
			String sql = copier.createSqlString(ExportType.SQL_INSERT, false, false);
			ScriptParser p = new ScriptParser(sql);
			assertEquals(3, p.getSize());

			String verb = SqlUtil.getSqlVerb(p.getCommand(0));
			assertEquals("INSERT", verb);
			assertTrue(p.getCommand(0).contains("'Arthur'"));
			assertTrue(p.getCommand(1).contains("'Ford'"));
			assertFalse(sql.contains("<[NULL]>"));
		}
		finally
		{
			GuiSettings.setDisplayNullString(null);
		}
	}

	@Test
	public void testCopyAsSqlDeleteInsert()
		throws Exception
	{
		ClipBoardCopier copier = new ClipBoardCopier(createDataStore());
		copier.doCopyAsSql(ExportType.SQL_DELETE_INSERT, false, false);
		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clp.getContents(copier);
		Object data = contents.getTransferData(DataFlavor.stringFlavor);
		assertNotNull(data);
		assertTrue(data instanceof String);
		ScriptParser p = new ScriptParser((String)data);
		assertEquals(4, p.getSize());
		assertEquals("DELETE", SqlUtil.getSqlVerb(p.getCommand(0)));
		assertEquals("DELETE", SqlUtil.getSqlVerb(p.getCommand(2)));
		assertEquals("INSERT", SqlUtil.getSqlVerb(p.getCommand(1)));
		assertEquals("INSERT", SqlUtil.getSqlVerb(p.getCommand(3)));
	}

	@Test
	public void testCopyAsSqlUpdate()
		throws Exception
	{
		DataStore ds = createDataStore();
		assertTrue(ds.hasPkColumns());
		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
		ClipBoardCopier copier = new ClipBoardCopier(ds);
		copier.doCopyAsSql(ExportType.SQL_UPDATE, false, false);
		Transferable contents = clp.getContents(copier);
		Object data = contents.getTransferData(DataFlavor.stringFlavor);
		assertNotNull(data);
		assertTrue(data instanceof String);
//		System.out.println(data);
		ScriptParser p = new ScriptParser((String)data);
		assertEquals(2, p.getSize());
		assertEquals("UPDATE", SqlUtil.getSqlVerb(p.getCommand(0)));
		assertEquals("UPDATE", SqlUtil.getSqlVerb(p.getCommand(1)));
		assertTrue(p.getCommand(0).contains("id = 1"));
		assertTrue(p.getCommand(1).contains("id = 2"));
	}

}
