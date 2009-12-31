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
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableCommentReaderTest
	extends TestCase
{
	
	public TableCommentReaderTest(String testName)
	{
		super(testName);
	}

	public void testCommentSql()
	{
		
		try
		{
			TestUtil util = new TestUtil("Comments");
			WbConnection con = util.getConnection("commentsTest");
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table comment_test (id integer primary key, first_name varchar(50))");
			stmt.executeUpdate("COMMENT ON TABLE comment_test IS 'Table comment'");
			stmt.executeUpdate("COMMENT ON COLUMN comment_test.id IS 'Primary key column'");
			stmt.executeUpdate("COMMENT ON COLUMN comment_test.first_name IS 'Firstname'");
			con.commit();
			
			TableIdentifier table = con.getMetadata().findTable(new TableIdentifier("COMMENT_TEST"));
			TableCommentReader reader = new TableCommentReader();
			
			String tableComment = reader.getTableCommentSql(con, table);
			assertNotNull(tableComment);
			assertTrue("Comment not found ", tableComment.equalsIgnoreCase("COMMENT ON TABLE comment_test IS 'Table comment';"));
			
			List<ColumnIdentifier> columns = con.getMetadata().getTableColumns(table);
			
			StringBuilder colComments = reader.getTableColumnCommentsSql(con, table, columns);
			assertNotNull(colComments);
			//System.out.println("*********\n" + colComments + "\n**********");
			ScriptParser p = new ScriptParser(colComments.toString());
			p.setReturnStartingWhitespace(false);
			assertEquals(2, p.getSize());
			assertEquals("COMMENT ON COLUMN COMMENT_TEST.ID IS 'Primary key column'", p.getCommand(0));
			assertEquals("COMMENT ON COLUMN COMMENT_TEST.FIRST_NAME IS 'Firstname'", p.getCommand(1));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
