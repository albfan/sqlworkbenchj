/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.parser;

import java.io.IOException;

import workbench.sql.ScriptCommandDefinition;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ScriptIteratorTest
{

	public ScriptIteratorTest()
	{
	}

	@Test
	public void testMixedEmptyLinesWithTerminator()
		throws Exception
	{
		for (ParserType type : ParserType.values())
		{
			doTestMixedEmptyLinesWithTerminator(new LexerBasedParser(type));
		}
	}

	private void doTestMixedEmptyLinesWithTerminator(ScriptIterator parser)
		throws Exception
	{
		String sql = "select * from foo;\n\n" + "select * from bar;\n";
		parser.setEmptyLineIsDelimiter(true);
		parser.setScript(sql);
		parser.setStoreStatementText(true);

		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from foo", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from bar", cmd.getSQL());

		sql =
			"select * from foo;\n" +
			"select * from bar;\n" +
			"select * from foobar;\n" +
			"\n" +
			"select * from foo;";
		parser.setScript(sql);
		parser.setStoreStatementText(true);

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from foo", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from bar", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from foobar", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from foo", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNull(cmd);
	}

	@Test
	public void testEmptyLineDelimiter()
		throws Exception
	{
		for (ParserType type : ParserType.values())
		{
			doTestEmptyLineDelimiter(new LexerBasedParser(type));
		}
	}

	private void doTestEmptyLineDelimiter(final ScriptIterator parser)
		throws Exception
	{
		String sql = "select * from test\n\n" + "select * from person\n";
		parser.setScript(sql);
		parser.setEmptyLineIsDelimiter(true);
		parser.setStoreStatementText(true);

		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from test", cmd.getSQL().trim());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from person", cmd.getSQL().trim());

		sql = "select a,b,c\r\nfrom test\r\nwhere x = 1";
		parser.setScript(sql);
		parser.setEmptyLineIsDelimiter(true);
		parser.setStoreStatementText(true);

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select a,b,c\r\nfrom test\r\nwhere x = 1", cmd.getSQL());

		sql = "select *\nfrom foo\n\nselect * from bar";
		parser.setScript(sql);
		parser.setStoreStatementText(true);

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select *\nfrom foo", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from bar", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNull(cmd);
	}


	@Test
	public void testQuotedDelimiter()
		throws Exception
	{
		for (ParserType type : ParserType.values())
		{
			doTestQuotedDelimiter(new LexerBasedParser(type));
		}
	}

	private void doTestQuotedDelimiter(ScriptIterator parser)
		throws Exception
	{
		String sql = "select 'test\n;lines' from test;";
		parser.setScript(sql);
		parser.setStoreStatementText(true);

		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select 'test\n;lines' from test", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNull(cmd);
	}

	@Test
	public void testWhiteSpaceAtEnd()
		throws Exception
	{
		for (ParserType type : ParserType.values())
		{
			doTestWhiteSpaceAtEnd(new LexerBasedParser(type));
		}
	}

	public void doTestWhiteSpaceAtEnd(ScriptIterator parser)
		throws IOException
	{
		String sql = "create table target_table (id integer);\n" +
			"wbcopy \n";

		parser.setScript(sql);
		parser.setCheckEscapedQuotes(false);
		parser.setEmptyLineIsDelimiter(false);
		parser.setStoreStatementText(false);

		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertNull(cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertNull(cmd.getSQL());
		assertEquals(sql.length(), cmd.getEndPositionInScript());
	}

	@Test
	public void testEscapedQuotes()
	{
		for (ParserType type : ParserType.values())
		{
			doTestEscapedQuotes(new LexerBasedParser(type));
		}
	}

	public void doTestEscapedQuotes(ScriptIterator parser)
	{
		parser.setCheckEscapedQuotes(true);
		parser.setScript(
			"insert into foo (data) values ('foo\\'s data1');\n" +
			"insert into foo (data) values ('foo\\'s data2');" +
			"commit;\n");
		parser.setStoreStatementText(true);

		ScriptCommandDefinition c = parser.getNextCommand();
		assertNotNull(c);
		assertTrue(c.getSQL().startsWith("insert"));

		c = parser.getNextCommand();
		assertNotNull(c);
		assertTrue(c.getSQL().startsWith("insert"));

		c = parser.getNextCommand();
		assertNotNull(c);
		assertEquals("commit", c.getSQL());
	}
}
