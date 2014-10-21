/*
 * LexerBasedParserTest.java
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
package workbench.sql;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class LexerBasedParserTest
{

	@Test
	public void testGetNextCommand()
		throws Exception
	{
		String sql = "select * from test;\n" + "select * from person;";
		LexerBasedParser parser = new LexerBasedParser(sql);
		assertTrue(parser.hasMoreCommands());
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

	@Test
	public void testStoreIndexOnly()
		throws Exception
	{
		String sql = "select * from test;\n" + "select * from person;";
		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setStoreStatementText(false);
		ScriptCommandDefinition cmd = null;
		while ((cmd = parser.getNextCommand()) != null)
		{
			assertNull(cmd.getSQL());
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

	@Test
	public void testTrimLeadingWhiteSpace()
		throws Exception
	{
		String sql = "select * from test;\nselect * from person;\n";
		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setReturnStartingWhitespace(false);
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
		}

		sql = "COMMENT ON COLUMN COMMENT_TEST.ID IS 'Primary key column';\r\nCOMMENT ON COLUMN COMMENT_TEST.FIRST_NAME IS 'Firstname';\r\n";
		parser = new LexerBasedParser(sql);
		parser.setReturnStartingWhitespace(false);
		parser.setStoreStatementText(false);
		while ((cmd = parser.getNextCommand()) != null)
		{
			int index = cmd.getIndexInScript();
			if (index == 0)
			{
				int start = cmd.getStartPositionInScript();
				int end = cmd.getEndPositionInScript();
				String cmdSql = sql.substring(start, end);
				assertEquals("COMMENT ON COLUMN COMMENT_TEST.ID IS 'Primary key column'", cmdSql);
			}
			else if (index == 1)
			{
				int start = cmd.getStartPositionInScript();
				int end = cmd.getEndPositionInScript();
				String cmdSql = sql.substring(start, end);
				assertEquals("COMMENT ON COLUMN COMMENT_TEST.FIRST_NAME IS 'Firstname'", cmdSql);
			}
		}
	}

	@Test
	public void testReturnLeadingWhiteSpace()
		throws Exception
	{
		String sql = " select * from test;\n" + " select * from person;";
		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setReturnStartingWhitespace(true);
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

	@Test
	public void testPatterns()
	{
		LexerBasedParser parser = new LexerBasedParser();
		assertTrue(parser.isMultiLine("\n\n"));
		assertTrue(parser.isMultiLine("\r\n\r\n"));
		assertFalse(parser.isMultiLine(" \r\n "));
		assertFalse(parser.isMultiLine("\r\n"));
		assertTrue(parser.isMultiLine(" \r\n\t\r\n "));
		assertTrue(parser.isMultiLine(" \r\n\t  \r\n\t"));

		assertTrue(parser.isLineBreak("\n"));
		assertTrue(parser.isLineBreak(" \n "));
		assertTrue(parser.isLineBreak("\r\n"));
		assertTrue(parser.isLineBreak(" \r\n "));
		assertTrue(parser.isLineBreak("\r\n  "));
		assertTrue(parser.isLineBreak("           \t\r\n\t"));
	}

	@Test
	public void testCursorInEmptyLine()
		throws Exception
	{
		String sql = "\nselect 42\nfrom dual;\nselect * \nfrom table\n;";
		LexerBasedParser p = new LexerBasedParser();
		p.setEmptyLineIsDelimiter(false);
		p.setScript(sql);
		ScriptCommandDefinition cmd = p.getNextCommand();
		assertNotNull(cmd);
//		System.out.println("*** start: " + cmd.getWhitespaceStart());
	}

	@Test
	public void testPgParser()
		throws Exception
	{
		String sql =
			"select * from foo;\n" +
			"commit;\n" +
			"delete from bar;";

		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setCheckPgQuoting(true);
		List<String> script = getStatements(parser);
		assertEquals(3, script.size());
		assertEquals("commit", script.get(1));

		sql =
			"create or replace function foo()\n" +
			"  returns integer \n" +
			"as \n" +
			"$body$\n" +
			"  declare l_value integer;\n" +
			"begin \n" +
			"   l_value := 42; \n" +
			"   return l_value; \n" +
			"end;\n" +
			"$body$\n"+
			"language plpgsql;";

		parser.setScript(sql);
		script = getStatements(parser);
		assertEquals(1, script.size());

		sql =
			"drop function foo()\n" +
			"/\n" +
			"create or replace function foo()\n" +
			"  returns integer \n" +
			"as \n" +
			"$body$\n" +
			"  declare l_value integer;\n" +
			"begin \n" +
			"   l_value := 42; \n" +
			"   return l_value; \n" +
			"end;\n" +
			"$body$\n"+
			"language plpgsql" +
			"/\n";

		parser.setScript(sql);
		parser.setDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		script = getStatements(parser);
		assertEquals(2, script.size());

		sql =
			"delete from foo where descr = 'arthur''s house';\n" +
			"commit;";

		parser.setScript(sql);
		parser.setDelimiter(DelimiterDefinition.STANDARD_DELIMITER);
		script = getStatements(parser);
		assertEquals(2, script.size());
		assertEquals("delete from foo where descr = 'arthur''s house'", script.get(0));
		assertEquals("commit", script.get(1));

		parser.setScript(sql);
		parser.setCheckPgQuoting(false);
		script = getStatements(parser);
		assertEquals(2, script.size());
		assertEquals("delete from foo where descr = 'arthur''s house'", script.get(0));
		assertEquals("commit", script.get(1));

		parser.setScript(
			"wbImport -fileColumns=one,two,$wb_skip$,three -table=x -file=x.txt;\n" +
			"select count(*) from x;\n");
		script = getStatements(parser);
		assertEquals(2, script.size());
		assertEquals("wbImport -fileColumns=one,two,$wb_skip$,three -table=x -file=x.txt", script.get(0));
		assertEquals("select count(*) from x", script.get(1));


		sql =
			"drop function foo();\n" +
			"\n" +
			"create or replace function foo()\n" +
			"  returns integer \n" +
			"as \n" +
			"$body$\n" +
			"  declare l_value varchar;\n" +
			"begin \n" +
			"   select \"$body$\" into l_value where some_column <> '$body$'; \n" +
			"   return l_value; \n" +
			"end;\n" +
			"$body$\n"+
			"language plpgsql;";

		parser.setScript(sql);
		parser.setCheckPgQuoting(true);
		script = getStatements(parser);
		assertEquals(2, script.size());
		assertEquals("drop function foo()", script.get(0));
		assertTrue(script.get(1).startsWith("create or replace"));
		assertTrue(script.get(1).endsWith("language plpgsql"));


		sql =
			"drop function foo()\n" +
			"/?\n" +
			"create or replace function foo()\n" +
			"  returns integer \n" +
			"as \n" +
			"$body$\n" +
			"  declare l_value varchar;\n" +
			"begin \n" +
			"   select \"$body$\" into l_value where some_column <> '$body$'; \n" +
			"   return l_value; \n" +
			"end;\n" +
			"$body$\n"+
			"language plpgsql\n" +
			"/?\n";

		parser = new LexerBasedParser(sql);
		parser.setDelimiter(new DelimiterDefinition("/?"));
		parser.setCheckPgQuoting(true);
		parser.setStoreStatementText(true);

		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("drop function foo()", cmd.getSQL());

//		System.out.println(script.get(1));
		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertTrue(cmd.getSQL().startsWith("create or replace"));
		assertTrue(cmd.getSQL().endsWith("language plpgsql"));
	}

	private List<String> getStatements(LexerBasedParser parser)
	{
		List<String> result = new ArrayList<>();
		while (parser.hasMoreCommands())
		{
			ScriptCommandDefinition cmd = parser.getNextCommand();
			result.add(cmd.getSQL());
		}
		return result;
	}

	@Test
	public void testMsGO()
		throws Exception
	{
		String sql = "select * from test\n GO \n" + "select * from person\nGO";
		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		parser.setStoreStatementText(true);

		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from test", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from person", cmd.getSQL());
	}


	@Test
	public void testAlternateDelimiter()
		throws Exception
	{
		String sql =
			"create table one (id integer)\n" +
			"/?\n" +
			"create table two (id integer)\n" +
			"/?\n";

		LexerBasedParser parser = new LexerBasedParser(sql);
		parser.setDelimiter(new DelimiterDefinition("/?"));
		parser.setStoreStatementText(false);

		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertNotNull(cmd.getDelimiterUsed());
		String stmt = sql.substring(cmd.getStartPositionInScript(), cmd.getEndPositionInScript());
		assertEquals("create table one (id integer)", stmt.trim());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertNotNull(cmd.getDelimiterUsed());
		stmt = sql.substring(cmd.getStartPositionInScript(), cmd.getEndPositionInScript());
		assertEquals("create table two (id integer)", stmt.trim());
	}

	@Test
	public void quotedQuotes()
	{
		String sql = "select \";\" from foobar;\ncommit;";
		LexerBasedParser parser = new LexerBasedParser(ParserType.Standard);
		parser.setScript(sql);
		parser.setStoreStatementText(true);
		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select \";\" from foobar", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("commit", cmd.getSQL());

		sql = "select ';', \"'\" from foobar;\ncommit;";
		parser.setScript(sql);
		parser.setStoreStatementText(true);
		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select ';', \"'\" from foobar", cmd.getSQL());

		sql =
			"wbexport  -type=text -delimiter=';' -quoteChar=\"'\" -file=test.txt; \n" +
      "wbimport  -type=text;";

		parser.setScript(sql);
		parser.setStoreStatementText(true);
		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		System.out.println(cmd.getSQL());
		assertEquals("wbexport  -type=text -delimiter=';' -quoteChar=\"'\" -file=test.txt", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("wbimport  -type=text", cmd.getSQL());
	}

	@Test
	public void testOracleInclude()
		throws Exception
	{
		String sql = "select \n@i = 1,id from test;\n" + "select * from person;delete from test2;commit;";
		LexerBasedParser parser = new LexerBasedParser(ParserType.Standard);
		parser.setScript(sql);
		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select \n@i = 1,id from test", cmd.getSQL().trim());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("select * from person", cmd.getSQL().trim());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("delete from test2", cmd.getSQL().trim());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("commit", cmd.getSQL().trim());

		parser = new LexerBasedParser(ParserType.Oracle);
		sql = "delete from person;\n  @insert_person.sql\ncommit;";
		parser.setScript(sql);
		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("delete from person", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("@insert_person.sql", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("commit", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNull(cmd);

		parser = new LexerBasedParser(ParserType.Standard);
		sql = "delete from person;\n  @insert_person.sql\ncommit;";
		parser.setScript(sql);
		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("delete from person", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("@insert_person.sql\ncommit", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNull(cmd);
	}

	@Test
	public void testOracleSingleLine()
	{
		String sql =
			"whenever sql error continue\n" +
			"\n" +
			"drop table foo;\n";
		LexerBasedParser parser = new LexerBasedParser(ParserType.Oracle);
		parser.setStoreStatementText(true);
		parser.setScript(sql);
		ScriptCommandDefinition cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("whenever sql error continue", cmd.getSQL());

		cmd = parser.getNextCommand();
		assertNotNull(cmd);
		assertEquals("drop table foo", cmd.getSQL());
	}
}
