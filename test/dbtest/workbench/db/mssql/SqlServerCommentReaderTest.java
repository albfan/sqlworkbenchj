/*
 * SqlServerCommentReaderTest.java
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
package workbench.db.mssql;

import org.junit.AfterClass;
import workbench.db.TableDefinition;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableCommentReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.sql.ScriptParser;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerCommentReaderTest
	extends WbTestCase
{
	public SqlServerCommentReaderTest()
	{
		super("SqlServerCommentReaderTest");
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
				"create table foo \n" +
				"( \n" +
				"   id1 integer, \n" +
				"   id2 numeric(19,2) \n" +
				")\n"  +
			"EXEC sp_addextendedproperty 'MS_DESCRIPTION', 'foo table comment', 'schema', 'dbo', 'table', 'foo';\n" +
			"EXEC sp_addextendedproperty 'MS_DESCRIPTION', 'foo.id1 column comment', 'schema', 'dbo', 'table', 'foo', 'column', 'id1';\n"+
			"EXEC sp_addextendedproperty 'MS_DESCRIPTION', 'foo.id2 column comment', 'schema', 'dbo', 'table', 'foo', 'column', 'id2';\n" +
			"commit;\n";
		TestUtil.executeScript(conn, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		if (con == null) return;
		SQLServerTestUtil.dropAllObjects(con);
	}
	
	@Test
	public void testComments()
		throws Exception
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		if (con == null) return;

		try
		{
			Settings.getInstance().setProperty("workbench.db.microsoft_sql_server.remarks.column.retrieve", true);
			Settings.getInstance().setProperty("workbench.db.microsoft_sql_server.remarks.object.retrieve", true);
			TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("foo"));
			assertNotNull(tbl);
			assertNotNull(tbl.getComment());
			TableDefinition def = con.getMetadata().getTableDefinition(tbl);
			assertNotNull(def);

			TableCommentReader commentReader = new TableCommentReader();
			String sql = commentReader.getTableCommentSql(con, tbl);
			assertEquals("EXEC sp_addextendedproperty 'MS_DESCRIPTION', 'foo table comment', 'schema', 'dbo', 'table', 'foo';", sql);

			StringBuilder colSql = commentReader.getTableColumnCommentsSql(con, tbl, def.getColumns());
			assertNotNull(colSql);
			ScriptParser p = new ScriptParser(colSql.toString());
			assertEquals(2, p.getSize());
			String col = p.getCommand(0);
			assertEquals("EXEC sp_addextendedproperty 'MS_DESCRIPTION', 'foo.id1 column comment', 'schema', 'dbo', 'table', 'foo', 'column', 'id1'", col);
			col = p.getCommand(1);
			assertEquals("EXEC sp_addextendedproperty 'MS_DESCRIPTION', 'foo.id2 column comment', 'schema', 'dbo', 'table', 'foo', 'column', 'id2'", col);
		}
		finally
		{
			Settings.getInstance().setProperty("workbench.db.microsoft_sql_server.remarks.column.retrieve", false);
			Settings.getInstance().setProperty("workbench.db.microsoft_sql_server.remarks.object.retrieve", false);
		}
	}
}
