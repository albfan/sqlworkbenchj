/*
 * TableCommentReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db;

import java.sql.Statement;
import java.util.List;

import workbench.TestUtil;

import workbench.sql.parser.ScriptParser;

import workbench.util.SqlUtil;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class TableCommentReaderTest
	extends WbTestCase
{
	private WbConnection connection;

	public TableCommentReaderTest()
	{
		super("TableCommentReaderTest");
	}

	@Before
	public void setUp()
		throws Exception
	{
		TestUtil util = new TestUtil("Comments");
		connection = util.getConnection("commentsTest");
		Statement stmt = null;
		try
		{
			TestUtil.executeScript(connection,
			"drop all objects;\n" +
			"CREATE TABLE comment_test (id integer PRIMARY KEY, first_name varchar(50));\n" +
			"COMMENT ON TABLE comment_test IS 'Table comment';\n" +
			"COMMENT ON COLUMN comment_test.id IS 'Primary key column';\n" +
			"COMMENT ON COLUMN comment_test.first_name IS 'Firstname';\n" +
			"commit;\n");
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	@Test
	public void testCommentSql()
		throws Exception
	{
		try
		{
			TableIdentifier table = connection.getMetadata().findTable(new TableIdentifier("COMMENT_TEST"));
			TableCommentReader reader = new TableCommentReader();

			String tableComment = reader.getTableCommentSql(connection, table);
			assertNotNull(tableComment);
			assertTrue("Comment not found ", tableComment.equalsIgnoreCase("COMMENT ON TABLE comment_test IS 'Table comment';"));

			List<ColumnIdentifier> columns = connection.getMetadata().getTableColumns(table);

			StringBuilder colComments = reader.getTableColumnCommentsSql(connection, table, columns);
			assertNotNull(colComments);
			ScriptParser p = new ScriptParser(colComments.toString());
			p.setReturnStartingWhitespace(false);
			assertEquals(2, p.getSize());
			assertEquals("COMMENT ON COLUMN COMMENT_TEST.ID IS 'Primary key column'", p.getCommand(0));
			assertEquals("COMMENT ON COLUMN COMMENT_TEST.FIRST_NAME IS 'Firstname'", p.getCommand(1));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testFallbackDBID()
		throws Exception
	{
		try
		{
			TableIdentifier table = connection.getMetadata().findTable(new TableIdentifier("COMMENT_TEST"));
			TableCommentReader reader = new TableCommentReader();

			String tableComment = reader.getTableCommentSql("XXX", connection, table);
			assertNotNull(tableComment);
			assertTrue("Comment not found ", tableComment.equalsIgnoreCase("COMMENT ON TABLE comment_test IS 'Table comment';"));

			List<ColumnIdentifier> columns = connection.getMetadata().getTableColumns(table);

			StringBuilder colComments = reader.getTableColumnCommentsSql("xxx", connection, table, columns);
			assertNotNull(colComments);
			ScriptParser p = new ScriptParser(colComments.toString());
			p.setReturnStartingWhitespace(false);
			assertEquals(2, p.getSize());
			assertEquals("COMMENT ON COLUMN COMMENT_TEST.ID IS 'Primary key column'", p.getCommand(0));
			assertEquals("COMMENT ON COLUMN COMMENT_TEST.FIRST_NAME IS 'Firstname'", p.getCommand(1));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testDB2()
		throws Exception
	{
		try
		{
			TableIdentifier table = connection.getMetadata().findTable(new TableIdentifier("COMMENT_TEST"));
			TableCommentReader reader = new TableCommentReader();

			String tableComment = reader.getTableCommentSql("db2i", connection, table);
			assertNotNull(tableComment);
			assertTrue("Comment not found ", tableComment.equalsIgnoreCase("COMMENT ON TABLE comment_test IS 'Table comment';"));

			List<ColumnIdentifier> columns = connection.getMetadata().getTableColumns(table);

			StringBuilder colComments = reader.getTableColumnCommentsSql("db2i", connection, table, columns);
			assertNotNull(colComments);
			ScriptParser p = new ScriptParser(colComments.toString());
			p.setReturnStartingWhitespace(false);
			assertEquals(2, p.getSize());
			assertEquals("COMMENT ON COLUMN COMMENT_TEST.ID IS 'Primary key column'", p.getCommand(0));
			assertEquals("COMMENT ON COLUMN COMMENT_TEST.FIRST_NAME IS 'Firstname'", p.getCommand(1));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
