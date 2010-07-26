/*
 * TableCommentReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Statement;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.sql.ScriptParser;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableCommentReaderTest
	extends TestCase
{
	private WbConnection connection;

	public TableCommentReaderTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
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

	@Override
	protected void tearDown()
		throws Exception
	{
		super.tearDown();
	}


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
