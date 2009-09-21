/*
 * LexerBasedParserTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

import junit.framework.TestCase;
import workbench.sql.LexerBasedParser;

/**
 *
 * @author Thomas Kellerer
 */
public class LexerBasedParserTest
	extends TestCase
{

	public LexerBasedParserTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		super.tearDown();
	}

	public void testGetNextCommand()
		throws Exception
	{
		String sql = "select * from test;\n" + "select * from person;";
		LexerBasedParser parser = new LexerBasedParser(sql);
		ScriptCommandDefinition cmd = null;
		while ((cmd = parser.getNextCommand()) != null)
		{
			int index = cmd.getIndexInScript();
			if (index == 0)
			{
				assertEquals("select * from test", cmd.getSQL());
			}
			else if (index == 1)
			{
				assertEquals("select * from person", cmd.getSQL());
			}
			else
			{
				fail("Wrong command index: " + index);
			}
		}

	}
	public void testStoreIndexOnly()
		throws Exception
	{
		String sql = "select * from test;\n" + "select * from person;";
		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setStoreStatementText(false);
		ScriptCommandDefinition cmd = null;
		while ((cmd = parser.getNextCommand()) != null)
		{
			int index = cmd.getIndexInScript();
			if (index == 0)
			{
				String cmdSql = sql.substring(cmd.getStartPositionInScript(), cmd.getEndPositionInScript());
				assertEquals("select * from test", cmdSql.trim());
			}
			else if (index == 1)
			{
				String cmdSql = sql.substring(cmd.getStartPositionInScript(), cmd.getEndPositionInScript());
				assertEquals("select * from person", cmdSql.trim());
			}
			else
			{
				fail("Wrong command index: " + index);
			}
		}
	}

	public void testReturnLeadingWhiteSpace()
		throws Exception
	{
		String sql = " select * from test;\n" + " select * from person;";
		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setReturnLeadingWhitespace(true);
		ScriptCommandDefinition cmd = null;
		while ((cmd = parser.getNextCommand()) != null)
		{
			int index = cmd.getIndexInScript();
			if (index == 0)
			{
				assertEquals(" select * from test", cmd.getSQL());
			}
			else if (index == 1)
			{
				assertEquals("\n select * from person", cmd.getSQL());
			}
			else
			{
				fail("Wrong command index: " + index);
			}
		}
	}

	public void testEmptyLineDelimiter()
		throws Exception
	{
		String sql = "select * from test\n\n" + "select * from person\n";
		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setEmptyLineIsDelimiter(true);
		ScriptCommandDefinition cmd = null;
		while ((cmd = parser.getNextCommand()) != null)
		{
			int index = cmd.getIndexInScript();
			if (index == 0)
			{
				assertEquals("select * from test", cmd.getSQL().trim());
			}
			else if (index == 1)
			{
				assertEquals("select * from person", cmd.getSQL().trim());
			}
			else
			{
				fail("Wrong command index: " + index);
			}
		}
	}
	
	public void testAlternateDelimiter()
		throws Exception
	{
		String sql = "select * from test\n./\n" + "select * from person\n./\n";
		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setDelimiter(new DelimiterDefinition("./", false));
		ScriptCommandDefinition cmd = null;
		while ((cmd = parser.getNextCommand()) != null)
		{
			int index = cmd.getIndexInScript();
			if (index == 0)
			{
				assertEquals("select * from test", cmd.getSQL().trim());
			}
			else if (index == 1)
			{
				assertEquals("select * from person", cmd.getSQL().trim());
			}
			else
			{
				fail("Wrong command index: " + index);
			}
		}

		sql = "select * from test./\n./\n" + "select * from person\n./\n";
		parser = new LexerBasedParser(sql);
		parser.setDelimiter(new DelimiterDefinition("./", true));
		while ((cmd = parser.getNextCommand()) != null)
		{
			//System.out.println(cmd.getSQL() + "\n******************\n");
			int index = cmd.getIndexInScript();
			if (index == 0)
			{
				assertEquals("select * from test./", cmd.getSQL().trim());
			}
			else if (index == 1)
			{
				assertEquals("select * from person", cmd.getSQL().trim());
			}
			else
			{
				fail("Wrong command index: " + index);
			}
		}
	}
}
