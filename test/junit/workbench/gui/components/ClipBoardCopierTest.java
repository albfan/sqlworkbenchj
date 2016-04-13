/*
 * ClipBoardCopierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.IndexColumn;
import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.ExportType;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

import workbench.sql.parser.ScriptParser;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

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
    final Clipboard testClip = new TestClipboard("testCopyDataToClipboard");

		ClipBoardCopier copier = new ClipBoardCopier(createDataStore())
    {
      @Override
      protected Clipboard getClipboard()
      {
        return testClip;
      }

    };
		copier.copyDataToClipboard(true, false, false);
		Transferable contents = testClip.getContents(copier);
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
    final Clipboard testClip = new TestClipboard("testCopyAsSqlInsert");
		try
		{
			GuiSettings.setDisplayNullString("<[NULL]>");
			DataStore ds = createDataStore();
			int row = ds.addRow();
			ds.setValue(row, 0, new Integer(1));
			ds.setValue(row, 1, "Marvin");
			ds.setValue(row, 2, null);

			ClipBoardCopier copier = new ClipBoardCopier(ds)
      {
        @Override
        protected Clipboard getClipboard()
        {
          return testClip;
        }
      };
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
	public void testCopyIdentityAsInsert()
		throws Exception
	{
    final Clipboard testClip = new TestClipboard("testCopyIdentityAsInsert");

		TestUtil util = getTestUtil();
		WbConnection conn = util.getHSQLConnection("clipboard");

		TestUtil.executeScript(conn,
			"create table foo (id integer generated always as identity);\n" +
			"insert into foo values (default), (default);\n" +
			"commit;\n");

		boolean check = Settings.getInstance().getCheckEditableColumns();
		boolean identity = Settings.getInstance().getGenerateInsertIgnoreIdentity();
		boolean format = Settings.getInstance().getDoFormatInserts();
		try
		{
			Settings.getInstance().setCheckEditableColumns(false);
			Settings.getInstance().setGenerateInsertIgnoreIdentity(false);
			Settings.getInstance().setDoFormatInserts(false);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from foo order by id");
			DataStore ds = new DataStore(rs, true);
			ds.setOriginalConnection(conn);
			ds.setUpdateTable(new TableIdentifier("FOO"));
			ClipBoardCopier copier = new ClipBoardCopier(ds)
      {
        @Override
        protected Clipboard getClipboard()
        {
          return testClip;
        }
      };
			String sql = copier.createSqlString(ExportType.SQL_INSERT, false, false);

			ScriptParser parser = new ScriptParser(sql);
			int size = parser.getSize();
			assertEquals(2, size);
			assertEquals("INSERT INTO FOO (ID) VALUES (0)", parser.getCommand(0));
			assertEquals("INSERT INTO FOO (ID) VALUES (1)", parser.getCommand(1));
		}
		finally
		{
			Settings.getInstance().setCheckEditableColumns(check);
			Settings.getInstance().setGenerateInsertIgnoreIdentity(identity);
			Settings.getInstance().setDoFormatInserts(format);
			TestUtil.executeScript(conn, "drop table foo;");
		}
	}

	@Test
	public void testCopyReadOnlyColumns()
		throws Exception
	{
    final Clipboard testClip = new TestClipboard("testCopyReadOnlyColumns");

		TestUtil util = getTestUtil();
		WbConnection conn = util.getHSQLConnection("clipboard");

		TestUtil.executeScript(conn,
			"create table foo (id integer generated always as identity, c1 integer, c2 integer);\n" +
			"insert into foo values (default,1,1), (default,2,2);\n" +
			"commit;\n");

		boolean check = Settings.getInstance().getCheckEditableColumns();
		boolean identity = Settings.getInstance().getGenerateInsertIgnoreIdentity();
		boolean format = Settings.getInstance().getDoFormatInserts();
		try
		{
			Settings.getInstance().setCheckEditableColumns(false);
			Settings.getInstance().setDoFormatInserts(false);
			Settings.getInstance().setGenerateInsertIgnoreIdentity(true);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from foo order by id");
			DataStore ds = new DataStore(rs, true);
			ds.setOriginalConnection(conn);
			ds.setUpdateTable(new TableIdentifier("FOO"));
			ds.getResultInfo().getColumn(1).setReadonly(true);
			ds.getResultInfo().getColumn(2).setReadonly(true);

      ClipBoardCopier copier = new ClipBoardCopier(ds)
      {
        @Override
        protected Clipboard getClipboard()
        {
          return testClip;
        }
      };

      String sql = copier.createSqlString(ExportType.SQL_INSERT, false, false);

			ScriptParser parser = new ScriptParser(sql);
			int size = parser.getSize();
			assertEquals(2, size);
			assertEquals("INSERT INTO FOO (C1,C2) VALUES (1,1)", parser.getCommand(0));
			assertEquals("INSERT INTO FOO (C1,C2) VALUES (2,2)", parser.getCommand(1));

			Settings.getInstance().setGenerateInsertIgnoreIdentity(false);
			Settings.getInstance().setCheckEditableColumns(true);
			sql = copier.createSqlString(ExportType.SQL_INSERT, false, false);
			parser = new ScriptParser(sql);
			size = parser.getSize();
			assertEquals(2, size);
			assertEquals("INSERT INTO FOO (ID) VALUES (0)", parser.getCommand(0));
			assertEquals("INSERT INTO FOO (ID) VALUES (1)", parser.getCommand(1));
		}
		finally
		{
			Settings.getInstance().setCheckEditableColumns(check);
			Settings.getInstance().setGenerateInsertIgnoreIdentity(identity);
			Settings.getInstance().setDoFormatInserts(format);
			TestUtil.executeScript(conn, "drop table foo;");
		}
	}

	@Test
	public void testCopyAsSqlDeleteInsert()
		throws Exception
	{
    final Clipboard testClip = new TestClipboard("testCopyAsSqlDeleteInsert");

		ClipBoardCopier copier = new ClipBoardCopier(createDataStore())
    {
      @Override
      protected Clipboard getClipboard()
      {
        return testClip;
      }
    };

		copier.doCopyAsSql(ExportType.SQL_DELETE_INSERT, false, false);

		Transferable contents = testClip.getContents(copier);
    assertNotNull(contents);

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

    final Clipboard testClip = new TestClipboard("testCopyAsSqlUpdate");

		ClipBoardCopier copier = new ClipBoardCopier(ds)
    {
      @Override
      protected Clipboard getClipboard()
      {
        return testClip;
      }
    };
		copier.doCopyAsSql(ExportType.SQL_UPDATE, false, false);
		Transferable contents = testClip.getContents(copier);
    assertNotNull(contents);

		Object data = contents.getTransferData(DataFlavor.stringFlavor);
		assertNotNull(data);
		assertTrue(data instanceof String);
		ScriptParser p = new ScriptParser((String)data);
		assertEquals(2, p.getSize());
		assertEquals("UPDATE", SqlUtil.getSqlVerb(p.getCommand(0)));
		assertEquals("UPDATE", SqlUtil.getSqlVerb(p.getCommand(1)));
		assertTrue(p.getCommand(0).contains("id = 1"));
		assertTrue(p.getCommand(1).contains("id = 2"));
	}

  private static class TestClipboard
    extends Clipboard
  {
    public TestClipboard(String name)
    {
      super(name);
    }

    @Override
    public synchronized void setContents(Transferable contents, ClipboardOwner owner)
    {
      this.owner = owner;
      this.contents = contents;
    }

  }

}

