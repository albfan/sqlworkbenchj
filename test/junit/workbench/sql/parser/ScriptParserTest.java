/*
 * ScriptParserTest.java
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
package workbench.sql.parser;

import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

import org.junit.Test;

import workbench.sql.DelimiterDefinition;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ScriptParserTest
	extends WbTestCase
{

	public ScriptParserTest()
	{
		super("ScriptParserTest");
	}

	@Test
	public void testQuotes()
		throws Exception
	{
		String sql = "delete from gaga;\n" +
			"\n" +
			"insert into gaga (col1) values ('one, two);";
		ScriptParser p = new ScriptParser(ParserType.Standard);
		p.setScript(sql);
		p.setDelimiter(DelimiterDefinition.STANDARD_DELIMITER);
		p.setScript(sql);
		// Make sure the remainder of the script (after the initial delete) is
		// added as (an incorrect) statement to the list of statement. Otherwise
		// it won't be processed and won't give an error
		int count = p.getSize();
		assertEquals(2, count);
		assertEquals("delete from gaga", p.getCommand(0));
		assertEquals("insert into gaga (col1) values ('one, two)", p.getCommand(1));

		sql = "wbfeedback off; \n" +
             " \n" +
             "create procedure dbo.CopyTable \n" +
             "      @SrcTableName varchar(max), \n" +
             "      @DestTableName varchar(max) \n" +
             "      as \n" +
             "   set nocount on \n" +
             "   set xact_abort on \n" +
             "   declare @cols varchar(max) \n" +
             "   select @cols = case when @cols is null then '' else @cols + ',' end + name from sys.columns where object_id=object_id(@DestTableName) order by column_id \n" +
             "   declare @sql varchar(max) \n" +
             "   set @sql = 'insert into ' + @DestTableName + ' (' + @cols + ') ' + \n" +
             "      'select ' + @cols + ' from + @SrcTableName \n" +
             "   exec (@sql); \n" +
             " \n" +
             "commit;";
		p.setScript(sql);
//		System.out.println("*****\n" + p.getCommand(1));
		assertEquals(2, p.getSize());
	}


	@Test
	public void testEmbeddedQuotes()
		throws Exception
	{
		String sql =
			"wbexport  -type=text -delimiter=';' -quoteChar=\"'\" -file=test.txt; \n" +
      "wbimport  -type=text;";

		ScriptParser p = new ScriptParser(sql);
		assertEquals(2, p.getSize());

		sql =
      "wbimport  -type=text \n" +
			"          -fileColumns=one,two,three,$wb_skip$ \n" +
			"          -delimiter=';' \n" +
			"          -endRow=5 \n" +
			"          -table=foo \n" +
			"          -truncateTable=true \n" +
			";\n" +
			"select count(*) from foo;";
		p = new ScriptParser(sql, ParserType.Standard);
		assertEquals(2, p.getSize());

		sql =
			"wbimport -file=\"\\\\someserver\\somedir\\somefile.csv\" \n" +
			"         -fileColumns=one,two,three,four,five,six,seven,eight,nine,ten,eleven,twelve,thirteen,fourteen,fifteen,sixteen,seventeen,$wb_skip$ \n" +
			"         -table=foo \n" +
			"         -endRow=5 \n" +
			"         -truncateTable=true \n" +
			"         -delimiter=';' \n" +
			"; \n" +
			" \n" +
			"select count(*) \n" +
			"from foobar \n" +
			"where one is null \n" +
			"; \n" +
			" \n" +
			"select two \n" +
			"from foobar \n" +
			"where some_col = '12345' \n" +
			"; \n" +
			" \n" +
			"";
		p = new ScriptParser(sql, ParserType.SqlServer);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		assertEquals(3, p.getSize());

		p = new ScriptParser(sql, ParserType.Standard);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		assertEquals(3, p.getSize());

		p = new ScriptParser(sql, ParserType.Postgres);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		assertEquals(3, p.getSize());
	}

	@Test
	public void testArrayBasedGetNext()
		throws Exception
	{
		String script = "select 1 from bla;\nselect 2 from blub;\n";
		ScriptParser p = new ScriptParser(script);
		int count = 0;
		while (p.getNextCommand() != null)
		{
			count ++;
		}
		assertEquals(2, count);
	}

	@Test
	public void testMultiByteEncoding()
		throws Exception
	{
		TestUtil util = getTestUtil();

		File f = new File(util.getBaseDir(), "insert.sql");
		ScriptParser parser = null;
		try
		{
			int statementCount = 18789;
			Writer w = EncodingUtil.createWriter(f, "UTF-8", false);
			int scriptSize = 0;
			for (int i=0; i < statementCount; i++)
			{
				String sql = "insert into address (id, street) \nvalues \n(" + i + ", ' \u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153');\n";
				w.write(sql);
				scriptSize += sql.length();
			}
			FileUtil.closeQuietely(w);
			parser = new ScriptParser(ParserType.Oracle);
			parser.setFile(f, "UTF-8");
			assertEquals(scriptSize, parser.getScriptLength());
			parser.startIterator();

			int commandsInFile = 0;
			String command = parser.getNextCommand();
			while (command != null)
			{
				String sql = "insert into address (id, street) \nvalues \n(" + commandsInFile + ", ' \u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153')";
				assertEquals("Statement #" + commandsInFile + " of " + statementCount + " not correct!", sql, command.trim());
				commandsInFile++;
				command = parser.getNextCommand();
				if (StringUtil.isEmptyString(command)) break;
			}
			assertEquals(statementCount, commandsInFile);
		}
		finally
		{
			parser.done();
			f.delete();
		}
	}

	@Test
	public void testEmptyStatement()
		throws Exception
	{
		String sql = "command1\n;;command2\n;\n";
		ScriptParser p = new ScriptParser();
		p.setScript(sql);
		assertEquals(2, p.getSize());
	}

	@Test
	public void testCursorInEmptyLine()
		throws Exception
	{
		String sql = "\nselect 42\nfrom dual;\nselect * \nfrom table\n;";
		ScriptParser p = new ScriptParser();
		p.setEmptyLineIsDelimiter(false);
		p.setScript(sql);
		int index = p.getCommandIndexAtCursorPos(0);
		assertEquals("Wrong statement index", 0, index);
		assertEquals(2, p.getSize());
	}

	@Test
	public void testEndPosition()
	{
		String sql = "select 42 from dual;\n\nselect * \nfrom table\n;";
		ScriptParser p = new ScriptParser();
		p.setEmptyLineIsDelimiter(false);
		p.setScript(sql);
		int pos = sql.lastIndexOf(';');
		int index = p.getCommandIndexAtCursorPos(pos);
		assertEquals(2, p.getSize());
		assertEquals(1, index);
	}

	@Test
	public void testCursorPosInCommand()
	{
		String script = "select 42 from dual;\n\nselect x\n        from y\n        \n        \n        ;";
		int pos = script.length() - 3;
		ScriptParser p = new ScriptParser();
		p.setEmptyLineIsDelimiter(false);
		p.setScript(script);
		int index = p.getCommandIndexAtCursorPos(pos);
		assertEquals(2, p.getSize());
		assertEquals(1, index);
	}

	@Test
	public void testEmptyLines()
	{
		String sql = "select a,b,c\r\nfrom test\r\nwhere x = 1";
		ScriptParser p = new ScriptParser();
		p.setEmptyLineIsDelimiter(true);
		p.setScript(sql);
		int count = p.getSize();
		assertEquals("Wrong number of statements", 1 ,count);

		sql = "select a,b,c\nfrom test\nwhere x = 1";
		p.setScript(sql);
		count = p.getSize();
		assertEquals("Wrong number of statements", 1 ,count);

		sql = "select a,b,c\nfrom test\nwhere x = 1\n\nselect x from y";
		p.setScript(sql);
		count = p.getSize();
		assertEquals("Wrong number of statements", 2 ,count);
		String cmd = p.getCommand(1);
		assertEquals("Wrong statement returned", "select x from y" ,cmd);

		sql = "select a,b,c\r\nfrom test\r\nwhere x = 1\r\n\r\nselect x from y";
		p.setScript(sql);
		count = p.getSize();
		assertEquals("Wrong number of statements", 2 ,count);
		cmd = p.getCommand(1);
		assertEquals("Wrong statement returned", "select x from y" ,cmd);
	}

	@Test
	public void testOracleSingleLine()
	{
		String sql =
			"select * from person order by id\ndesc;\n" +
			"\n" +
			"desc person\n" +
			"\n" +
			"delete from person;";
		ScriptParser p = new ScriptParser(ParserType.Oracle);
		p.setScript(sql);
		int size = p.getSize();
		assertEquals(3, size);

		assertEquals("select * from person order by id\ndesc", p.getCommand(0));
		assertEquals("desc person", p.getCommand(1));
		assertEquals("delete from person", p.getCommand(2));

		sql =
			"\n   desc person\n" +
			"\n" +
			"select * from dual;";
		p.setScript(sql);
		size = p.getSize();
		assertEquals(2, size);
		assertEquals("desc person", p.getCommand(0));
		assertEquals("select * from dual", p.getCommand(1));

	}

	@Test
	public void testSingleLineDelimiter()
	{
		String sql = "DROP\n" +
			           "/ \n" +
			           "CREATE\n" +
								 "/ \n";
		ScriptParser p = new ScriptParser();
		p.setAlternateDelimiter(new DelimiterDefinition("/"));
		p.setScript(sql);
		int size = p.getSize();
		assertEquals("Wrong number of statements", 2, size);
		assertEquals("Wrong statement returned", "DROP", p.getCommand(0));
		assertEquals("Wrong statement returned", "CREATE", p.getCommand(1));

		sql = "DROP\r\n" +
					 "/\r\n" +
					 "CREATE\r\n" +
					 " /";

		p.setScript(sql);
		size = p.getSize();
		assertEquals("Wrong number of statements", 2, size);
		assertEquals("Wrong statement returned", "DROP", p.getCommand(0));
		assertEquals("Wrong statement returned", "CREATE", p.getCommand(1));
	}

	@Test
	public void testAlternateFileParsing()
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.prepareEnvironment();

		File scriptFile = new File(util.getBaseDir(), "testscript.sql");
		String script =
			"-- test script \n" +
			"CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100)) \n" +
			"/ \n" +
			"insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent') \n" +
			"/ \n" +
			"insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect') \n" +
			"/ \n" +
			"insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox') \n" +
			"/ \n" +
			"commit \n" +
			"/";
		TestUtil.writeFile(scriptFile, script);

		// Make sure the iterating parser is used, by setting
		// a very low max file size
		ScriptParser p = new ScriptParser(10);
		p.setDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		p.setCheckEscapedQuotes(false);

		p.setFile(scriptFile);
		p.startIterator();
		int size = 0;
		String sql = p.getNextCommand();
		while (sql != null)
		{
			if (StringUtil.isBlank(sql)) break;
			size ++;

			if (size == 2)
			{
				assertEquals("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent')", sql);
			}
			sql = p.getNextCommand();
		}
		assertEquals("Wrong number of statements", 5, size);
		p.done();
		assertTrue(scriptFile.delete());
	}

	@Test
	public void testQuotedDelimiter()
	{
		String sql = "SELECT id,';' \n" +
								 "FROM person; \n" +
								 " \n" +
								 "select * \n" +
								 "from country;";
		ScriptParser p = new ScriptParser(sql);
		int size = p.getSize();
		assertEquals("Wrong number of statements", 2, size);
		assertEquals("Wrong statement returned", "SELECT id,';' \nFROM person", p.getCommand(0));
	}

	@Test
	public void testMsGO()
		throws Exception
	{
		String sql = "SELECT id \n" +
								 "FROM person GO\n" +
								 "  GO  \n" +
								 " \n" +
								 " \n" +
								 "select * \n" +
								 "from country \n" +
								 "  GO";
		ScriptParser p = new ScriptParser(ParserType.SqlServer);
		p.setScript(sql);
		// Test if the automatic detection of the MS SQL delimiter works
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		int size = p.getSize();
		assertEquals("Wrong number of statements", 2, size);
		//System.out.println("***********\nsql=" + p.getCommand(0) + "\n***\n" + p.getCommand(1) + "\n********");
		assertEquals("Wrong statement returned", "SELECT id \nFROM person GO", p.getCommand(0));
		assertEquals("Wrong statement returned", "select * \nfrom country", p.getCommand(1));

		sql = "SELECT id \r\n" +
					 "FROM person GO\r\n" +
					 "  GO  \r\n" +
					 " \r\n" +
					 "select * \r\n" +
					 "from country \r\n" +
					 "GO\n" +
					 "select * \r\n" +
					 "from country \r\n" +
					 "GO";
		p.setScript(sql);
		size = p.getSize();
		assertEquals("Wrong number of statements", 3, size);
		assertEquals("Wrong statement returned", "SELECT id \r\nFROM person GO", p.getCommand(0));
		assertEquals("Wrong statement returned", "select * \r\nfrom country", p.getCommand(1));

		sql = "SET QUOTED_IDENTIFIER ON\nGO\nSET ANSI_NULLS ON\nGO";
		p.setScript(sql);
		size = p.getSize();
		assertEquals("Wrong number of statements", 2, size);
		assertEquals("SET QUOTED_IDENTIFIER ON", p.getCommand(0));
		assertEquals("SET ANSI_NULLS ON", p.getCommand(1));

		sql = "SET QUOTED_IDENTIFIER ON\nRUN\nSET ANSI_NULLS ON\nRUN";
		p.setScript(sql);
		p.setAlternateDelimiter(new DelimiterDefinition("RUN"));
		size = p.getSize();
		assertEquals("Wrong number of statements", 2, size);
		assertEquals("SET QUOTED_IDENTIFIER ON", p.getCommand(0));
		assertEquals("SET ANSI_NULLS ON", p.getCommand(1));
	}

	@Test
	public void testGoWithComments()
	{
		String sql =
						 "IF  EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID('something') AND OBJECTPROPERTY(id,'IsProcedure') = 1) \n" +
             "DROP PROCEDURE something \n" +
             "GO\n" +
             "\n" +
             "-- Test comment \n" +
             "CREATE PROCEDURE something \n" +
             "AS  \n" +
             "BEGIN " +
						 "   DECLARE @counter INT\n" +
						 "   SELECT @counter = count(*) FROM person " +
             "END \n" +
             "GO";
		ScriptParser p = new ScriptParser(ParserType.SqlServer);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		p.setScript(sql);
		int size = p.getSize();
		assertEquals(2, size);

		sql =
			"use Master \n" +
			"GO \n" +
			" \n" +
			"If  Exists (select * from master.dbo.syslogins where name = 'arthur' and dbname = 'master') \n" +
			"	drop login arthur \n" +
			"create login arthur with Password = 'dent' \n" +
			"GO \n" +
			" \n" +
			"If  Exists (select * from master.dbo.syslogins where name = 'ford' and dbname = 'master') \n" +
			"	drop login ford \n" +
			"create login ford with Password = 'prefect' \n" +
			"GO \n" +
			" \n" +
			" \n" +
			"-- CREATE database arthur \n" +
			"-- GO \n" +
			" \n" +
			"-- use arthur \n" +
			"-- GO \n" +
			" \n" +
			"-- Create User foo_admin for login arthur \n" +
			"-- GO \n" +
			" \n" +
			" \n" +
			"-- use master \n" +
			"-- GO \n" +
			"-- drop database arthur \n" +
			"-- GO";

		p = new ScriptParser(sql);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		size = p.getSize();
//		for (int i=0; i < size; i++)
//		{
//			System.out.println(p.getCommand(i) + "\n####################\n");
//		}
		assertEquals(4, size);
	}

	@Test
	public void testAlternateDelimiter()
		throws Exception
	{
		String sql = "SELECT id \n" +
								 "FROM person \n" +
								 "@@ \n" +
								 " \n" +
								 " \n" +
								 "select * \n" +
								 "from country \n" +
								 "@@";
		ScriptParser p = new ScriptParser(sql);
		p.setAlternateDelimiter(new DelimiterDefinition("@@"));
		int size = p.getSize();
		assertEquals("Wrong number of statements", 2, size);

		p.setAlternateDelimiter(new DelimiterDefinition("./"));
		size = p.getSize();
		assertEquals("Wrong number of statements", 1, size);

		sql = "SELECT id; \n" +
							 "FROM person \n" +
							 "./ \n" +
							 " \n" +
							 "select * \n" +
							 "from country \n" +
							 "./";
		p.setScript(sql);
		size = p.getSize();
		assertEquals("Wrong number of statements", 2, size);

		sql = "CREATE PROCEDURE remove_emp (employee_id NUMBER) AS\n" +
				"  tot_emps NUMBER;\n" +
				"  BEGIN\n" +
				"			DELETE FROM employees\n"+
				"			WHERE employees.employee_id = remove_emp.employee_id;\n"+
				"	 tot_emps := tot_emps - 1;\n"+
				"	 END;\n"+
				"/";
		p.setScript(sql);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		size = p.getSize();
		assertEquals("Wrong number of statements", 1, size);
		assertEquals(sql.substring(0, sql.lastIndexOf('/')).trim(), p.getCommand(0));

		sql = "DECLARE \n" +
					 "   Last_name    VARCHAR2(10) \n" +
					 "   Cursor       c1 IS SELECT last_name  \n" +
					 "                       FROM employees \n" +
					 "                       WHERE department_id = 20 \n" +
					 "BEGIN \n" +
					 "   OPEN c1 \n" +
					 "   LOOP \n" +
					 "      FETCH c1 INTO Last_name \n" +
					 "      EXIT WHEN c1%NOTFOUND \n" +
					 "      DBMS_OUTPUT.PUT_LINE(Last_name) \n" +
					 "   END LOOP \n" +
					 "END \n" +
					 "/";
		p.setScript(sql);
		size = p.getSize();

		assertEquals("Wrong number of statements", 1, size);
		assertEquals(sql.substring(0, sql.lastIndexOf('/')).trim(), p.getCommand(0));

		sql = "DECLARE\n" +
					 "\tresult varchar (100) := 'Hello, world!';\n" +
					 "BEGIN \n" +
					 "\t\tdbms_output.put_line(result);\n" +
					 "END;\n" +
					 "/ ";
		p.setScript(sql);
		String expected = sql.substring(0, sql.lastIndexOf('/') - 1).trim();

		p.setScript(sql);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		size = p.getSize();
		String cmd = p.getCommand(0);
//			System.out.println("--- sql ---\n" + cmd + "\n----- expected -----\n" + expected + "\n-----------");
		assertEquals(expected, cmd);

		sql = "update foo set bar = 1 where id = 1\n/\nupdate foo set bar = 2 where id = 2\n/\n";
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		p.setScript(sql);
		size = p.getSize();
		assertEquals(2, size);
	}

	@Test
	public void testAccessByCursorPos()
	{
		try
		{
			String sql = "-- comment line 1\n" +
				"select * from person;\n" +
				"\n" +
				"-- next comment\n" +
				"insert into bla;\n" +
				"\n" +
				"/* bla stuff \n" +
				"   bla stuff \n" +
				"   bla stuff */\n" +
				"-- line comment\n" +
				"delete from blub;";
			ScriptParser p = new ScriptParser(sql);
			assertEquals("Not enough commands", 3, p.getSize());

			String c = p.getCommand(0);
			assertEquals("Wrong command at index 0", "SELECT", SqlUtil.getSqlVerb(c));

			c = p.getCommand(2);
			assertEquals("Wrong command at index 0", "DELETE", SqlUtil.getSqlVerb(c));

			int index = p.getCommandIndexAtCursorPos(5);
			assertEquals("Wrong command at cursor pos", index, 0);

			index = p.getCommandIndexAtCursorPos(45);
			assertEquals("Wrong command at cursor pos", index, 1);

			index = p.getCommandIndexAtCursorPos(99999);
			assertEquals("Wrong command at cursor pos", index, -1);


			sql = "select 'One Value';\n\nselect 'Some other value';\n";
			p = new ScriptParser(sql);
			assertEquals(2, p.getSize());
			assertEquals("select 'One Value'", p.getCommand(0));
			assertEquals("select 'Some other value'", p.getCommand(1));

			assertEquals(0, p.getCommandIndexAtCursorPos(1));
			assertEquals(1, p.getCommandIndexAtCursorPos(31));

			sql = "create table target_table (id integer);\n" +
				"create table source_table (id varchar(20));\n\n\n" +
				"insert into source_table values ('1'), ('two');\n\n" +
				"wbcopy \n";

			int cursorPos = sql.length() - 1;

			ScriptParser parser = new ScriptParser(sql);
			parser.setCheckEscapedQuotes(false);
			parser.setEmptyLineIsDelimiter(false);

			index = parser.getCommandIndexAtCursorPos(cursorPos);
			assertTrue(index > 0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testShortInclude()
	{
		try
		{
			String sql = "-- comment line 1\n" +
				"select * from person where name = 'Dent';\n" +
				"\n" +
				"-- next comment\n" +
				"insert into bla (nr, name) values (1,'laber');\n" +
				"\n" +
				"@myfile.sql";
			ScriptParser p = new ScriptParser(ParserType.Oracle);
			p.setScript(sql);
			assertEquals("Not enough commands", 3, p.getSize());
			assertEquals("Wrong command", "@myfile.sql", p.getCommand(2));

			sql = "-- comment line 1\n" +
				"select * from person where name = 'Dent';\n" +
				"\n" +
				"-- next comment\n" +
				"insert into bla (nr, name) values (1,'laber');\n" +
				"\n" +
				"@myfile.sql\n" +
				"\n" +
				"delete from theTable;";
			p.setScript(sql);
			assertEquals("Not enough commands", 4, p.getSize());
			assertEquals("Wrong command", "@myfile.sql", p.getCommand(2));
			assertEquals("Wrong command", "delete from theTable", p.getCommand(3));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private File createScript(int counter, String lineEnd)
		throws IOException
	{
		File tempdir = new File(System.getProperty("java.io.tmpdir"));
		File script = new File(tempdir, "largefile.sql");
		try (BufferedWriter out = new BufferedWriter(new FileWriter(script)))
		{
			for (int i = 0; i < counter; i++)
			{
				out.write("--- test command");
				out.write(lineEnd);
				out.write("insert into test_table");
				out.write(lineEnd);
				out.write("col1, col2, col3, col4)");
				out.write(lineEnd);
				out.write("values ('1','2''',3,' a two line ");
				out.write(lineEnd);
				out.write("; quoted text');");
				out.write(lineEnd);
				out.write(lineEnd);
			}
		}
		return script;
	}

	@Test
	public void testFileParsing()
	{
		try
		{
			int counter = 500;
			File script = createScript(counter, "\n");
			ScriptParser p = new ScriptParser(100);
			p.setFile(script);
			p.startIterator();
			int count = 0;
			String sql = null;
			while ((sql = p.getNextCommand()) != null)
			{
				if (StringUtil.isBlank(sql)) continue; // ignore empty lines at the end
				assertNotNull("No SQL returned at " + count, sql);
				String verb = SqlUtil.getSqlVerb(sql);
				assertEquals("Wrong statement retrieved using LF", "insert", verb.toLowerCase());
				count ++;
			}
			p.done();
			assertEquals("Wrong number of statements using LF", counter, count);
			script.delete();

			script = createScript(counter, "\r\n");
			p.setFile(script);
			p.startIterator();
			count = 0;
			while ((sql = p.getNextCommand()) != null)
			{
				if (StringUtil.isBlank(sql)) continue; // ignore empty lines at the end
				assertNotNull("No SQL returned at " + count, sql);
				String verb = SqlUtil.getSqlVerb(sql);
				assertEquals("Wrong statement retrieved using CRLF", "insert", verb.toLowerCase());
				count ++;
			}
			p.done();
			assertEquals("Wrong number of statements using CRL", counter, count);
			script.delete();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testFileParsing2()
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.prepareEnvironment();
		String sql =
			"create table foo (data varchar(100) not null);\n" +
			"insert into foo values ('\u00c3');\n" +
			"insert into foo values ('f\u00fcr');\n" +
			"commit;\n";

		File scriptFile = new File(util.getBaseDir(), "foo.sql");
		TestUtil.writeFile(scriptFile, sql, "UTF-8");

		// Make sure the iterating parser is used, by setting
		// a very low max file size
		ScriptParser p = new ScriptParser(ParserType.Standard);
		p.setFile(scriptFile, "UTF-8");
		String cmd = p.getNextCommand();
		assertNotNull(cmd);
		assertEquals("create table foo (data varchar(100) not null)", cmd);

		cmd = p.getNextCommand();
		assertNotNull(cmd);
		assertEquals("insert into foo values ('\u00c3')", cmd);

		cmd = p.getNextCommand();
		assertNotNull(cmd);
		assertEquals("insert into foo values ('f\u00fcr')", cmd);

		cmd = p.getNextCommand();
		assertNotNull(cmd);
		assertEquals("commit", cmd);

		cmd = p.getNextCommand();
		assertNull(cmd);
	}


	@Test
	public void testMultiStatements()
	{
		String sql = "SELECT '(select l.label from template_field_label l where l.template_field_id = f.id and l.language_code = '''|| l.code ||''') as \"'||l.code||' ('||l.name||')\",' \n" +
									"FROM  (SELECT DISTINCT language_code FROM template_field_label) ll,  \n" +
									"      language l \n" +
									"WHERE ll.language_code = l.code \n" +
									";\n" +
								 "select * from template_field_label;\n\n" +
								 "SELECT distinct t.KEY \n" +
								 "FROM translation t, content_folder f \n" +
								 "WHERE t.key = f.folder_name;";

		ScriptParser p = new ScriptParser(ParserType.Oracle);
		p.setScript(sql);
		assertEquals(3, p.getSize());
		assertEquals("select * from template_field_label", p.getCommand(1));

		sql = "/* \n" +
					 "* comment comment comment \n" +
					 "* comment \n" +
					 "* comment \n" +
					 "*/ \n" +
					 "-- comment comment comment comment' comment comment comment. \n" +
					 "  -- comment comment comment comment' comment comment comment. \n" +
					 "-- comment comment comment comment' comment comment comment. \n" +
					 "-- comment comment comment comment' comment comment comment. \n" +
					 " \n" +
					 "-- ############################################# \n" +
					 "-- ##                                         ## \n" +
					 "-- ##              Stuff                      ## \n" +
					 "-- ##                                         ## \n" +
					 "alter table participants drop constraint r_05;   -- make sure you recreate this foreign key after inserting data! \n" +
					 "drop table organizations;\n" +
					 "@include.sql\n" +
			     "\n" +
			     "select * from bla;";

		p.setScript(sql);
		assertEquals(4, p.getSize());
		String verb = SqlUtil.getSqlVerb(p.getCommand(1));
		assertEquals("drop", verb.toLowerCase());
		String s = p.getCommand(0);
		String clean = SqlUtil.makeCleanSql(s, false, false, '\'', false, true);
		assertEquals("alter table participants drop constraint r_05", clean);
		s = p.getCommand(2);
		assertEquals("@include.sql", s);

		// Now test with Windows linefeeds
		sql = StringUtil.replace(sql, "\n", "\r\n");
		p.setScript(sql);
		assertEquals(4, p.getSize());
		verb = SqlUtil.getSqlVerb(p.getCommand(1));
		assertEquals("drop", verb.toLowerCase());
		s = p.getCommand(0);
		clean = SqlUtil.makeCleanSql(s, false, false, '\'', false, true);
		assertEquals("alter table participants drop constraint r_05", clean);
		s = p.getCommand(2);
		assertEquals("@include.sql", s);


		sql = "SELECT distinct t.KEY \r\n" +
					"FROM translation t, content_folder f \r\n" +
					"WHERE t.key = f.folder_name \r\n" +
					"--AND   LANGUAGE = 'en' \r\n" +
					";\r\n" +
					"\r\n" +
					"WBDIFF -sourceprofile=\"CMTS\" \r\n" +
					"       -file=c:/temp/test.xml \r\n" +
					"       -includeindex=false \r\n" +
					"       -includeforeignkeys=false \r\n" +
					"       -includeprimarykeys=false \r\n" +
					";\r\n";
		p = new ScriptParser(sql);
		assertEquals(2, p.getSize());
	}

	@Test
	public void testCommentWithQuote()
	{
		try
		{
			String sql = "select 42 from dummy;\n" +
				"/* arthur's comment */\n" +
				"create table test ( \n" +
				"   my_col integer, \n" +
				"            -- Zaphod's comment\n" +
				"   col2 varchar(10)  -- Tricia's comment\n" +
				");\n" +
				"\n" +
				"select 43 from dual;";
			ScriptParser p = new ScriptParser(sql);
//			int count = p.getSize();
//			for (int i=0; i < count; i++)
//			{
//				System.out.println(p.getCommand(i) + "\n####################\n");
//			}
			assertEquals(3, p.getSize());
			assertEquals("select 42 from dummy", p.getCommand(0));
			assertEquals("select 43 from dual", p.getCommand(2));
			assertTrue(p.getCommand(1).startsWith("/* arthur's comment */\ncreate table test ( "));

			sql = "select 'a' from dual; -- comment'\nselect 'b' from dual;";
			p = new ScriptParser(sql);
			assertEquals(2, p.getSize());

			int pos = sql.indexOf('\n') + 1;
			int index = p.getCommandIndexAtCursorPos(pos);
			assertEquals(1, index);
			String cmd = p.getCommand(index);
			assertEquals("-- comment'\nselect 'b' from dual", cmd);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testIdioticQuoting()
	{
		String sql = "SELECT * FROM [Some;Table];DELETE FROM [Other;Table];";
		ScriptParser parser = new ScriptParser(ParserType.SqlServer);
		parser.setScript(sql);
		int count = parser.getSize();
		assertEquals(2, count);
		assertEquals("SELECT * FROM [Some;Table]", parser.getCommand(0).trim());

    parser = new ScriptParser(sql, ParserType.SqlServer);
		count = parser.getSize();
		assertEquals(2, count);
		assertEquals("SELECT * FROM [Some;Table]", parser.getCommand(0).trim());

		sql = "SELECT '[SomeTable];' FROM dual;DELETE FROM \"[Other];Table\";";
		parser = new ScriptParser(ParserType.SqlServer);
		parser.setScript(sql);
		count = parser.getSize();
		assertEquals(2, count);
		assertEquals("SELECT '[SomeTable];' FROM dual", parser.getCommand(0).trim());
		assertEquals("DELETE FROM \"[Other];Table\"", parser.getCommand(1).trim());

		sql = "SELECT * FROM [Some;Table];DELETE FROM [Other;Table];";
    parser = new ScriptParser(ParserType.Standard);
		parser.setScript(sql);
		count = parser.getSize();
		assertEquals(4, count);
		assertEquals("SELECT * FROM [Some", parser.getCommand(0).trim());
		assertEquals("Table]", parser.getCommand(1).trim());
		assertEquals("DELETE FROM [Other", parser.getCommand(2).trim());
		assertEquals("Table]", parser.getCommand(3).trim());
	}

	@Test
	public void testAlternateLineComment()
	{
		String sql =  "# this is a non-standard comment;\n" +
									"select * from test1;\n"+
									"# another non-standard comment;\n"+
									"select * from test2;\n" +
									"-- standard comment;\n"+
									"select * from test3;\n";
    ScriptParser parser = new ScriptParser(ParserType.MySQL);
		parser.setScript(sql);

		int count = parser.getSize();
		assertEquals("Wrong statement count", 3, count);

		sql =  "-- this is a non-standard comment;\n" +
									"select * from test1;\n"+
									"-- another non-standard comment;\n"+
									"select * from test2;\n" +
									"-- standard comment;\n"+
									"select * from test3;\n";
    parser = new ScriptParser(ParserType.Standard);
		parser.setScript(sql);
		count = parser.getSize();
		assertEquals("Wrong statement count.", 3, count);
	}

	@Test
	public void testUnicodeComments()
	{
		String sql = "-- \u32A5\u0416\u32A5\u0416\u2013\u2021\u00e6\u00b3\u00a8\u00e9\u2021\u0160\n" +
									"select * from test;\n"+
									"-- \u32A5\u0416\u32A5\u0416\u2013\u2021\u00e6\u00b3\u00a8\u00e9\u2021\u0160\n"+
									"select * from test2;\n";

    ScriptParser parser = new ScriptParser(sql);

		int count = parser.getSize();
		assertEquals("Wrong statement count", count, 2);
		int pos = sql.indexOf("from test2");
		int index = parser.getCommandIndexAtCursorPos(pos);
		assertEquals(1, index);

		pos = sql.indexOf("from test;");
		index = parser.getCommandIndexAtCursorPos(pos);
		assertEquals(0, index);
	}

	@Test
	public void testMerge()
	{
		String sql = "MERGE INTO T085_ITEMSALE_DAY itd \n" +
             "USING \n" +
             "( \n" +
             "  select itm.c060_itemid, \n" +
             "         sto.c015_storeid, \n" +
             "         impd.receiptdate, \n" +
             "         impd.sum_qty_piece, \n" +
             "         impd.sum_turnover \n" +
             "  from imp_itemsale_day impd \n" +
             "    JOIN t060_items itm ON itm.c060_orig_item_nr = impd.itemid \n" +
             "    JOIN t015_stores sto ON sto.c015_orig_store_nr = impd.storeid \n" +
             ") i ON (i.c060_itemid = itd.C085_ITEMID AND i.c015_storeid = itd.C085_STOREID AND i.receiptdate = itd.C085_RECEIPTDATE) \n" +
             "WHEN MATCHED THEN \n" +
             "UPDATE \n" +
             "  SET itd.C085_SUM_QTY_PIECE = i.sum_qty_piece, \n" +
             "      itd.C085_SUM_TURNOVER = i.sum_turnover \n" +
             "WHEN NOT MATCHED THEN \n" +
             "INSERT \n" +
             "( \n" +
             "  C085_STOREID, \n" +
             "  C085_ITEMID, \n" +
             "  C085_RECEIPTDATE, \n" +
             "  C085_SUM_QTY_PIECE, \n" +
             "  C085_SUM_TURNOVER \n" +
             ") \n" +
             "VALUES \n" +
             "( \n" +
             "  i.c015_storeid, \n" +
             "  i.c060_itemid, \n" +
             "  i.receiptdate, \n" +
             "  i.sum_qty_piece, \n" +
             "  i.sum_turnover \n" +
             ") \n" +
             "; \n" +
             " \n" +
             "MERGE INTO T086_ITEMSALE_WEEK itw \n" +
             "USING \n" +
             "( \n" +
             "  select sto.c015_storeid, \n" +
             "         itm.c060_itemid, \n" +
             "         to_number(substr(impw.CAL_WEEK,1,4)) as cal_year, \n" +
             "         to_number(substr(impw.CAL_WEEK,5,2)) as cal_week, \n" +
             "         impw.SUM_QTY_PIECE, \n" +
             "         impw.SUM_TURNOVER \n" +
             "  from imp_itemsale_week impw \n" +
             "    JOIN t060_items itm ON itm.c060_orig_item_nr = impw.itemid \n" +
             "    JOIN t015_stores sto ON sto.c015_orig_store_nr = impw.storeid \n" +
             ") i ON (itw.C086_STOREID = i.c015_storeid \n" +
             "        AND itw.C086_ITEMID = i.c060_itemid \n" +
             "        AND itw.C086_CAL_WEEK = i.cal_week \n" +
             "        AND itw.C086_CAL_YEAR = i.cal_year \n" +
             "        ) \n" +
             "WHEN MATCHED THEN \n" +
             "UPDATE \n" +
             "  set itw.C086_SUM_QTY_PIECE = i.SUM_QTY_PIECE, \n" +
             "      itw.C086_SUM_TURNOVER = i.SUM_TURNOVER \n" +
             "WHEN NOT MATCHED THEN \n" +
             "INSERT \n" +
             "( \n" +
             "  C086_STOREID, \n" +
             "  C086_ITEMID, \n" +
             "  C086_CAL_WEEK, \n" +
             "  C086_CAL_YEAR, \n" +
             "  C086_SUM_QTY_PIECE, \n" +
             "  C086_SUM_TURNOVER \n" +
             ") \n" +
             "VALUES \n" +
             "( \n" +
             "  i.c015_storeid, \n" +
             "  i.c060_itemid, \n" +
             "  i.cal_week, \n" +
             "  i.cal_year, \n" +
             "  i.SUM_QTY_PIECE, \n" +
             "  i.SUM_TURNOVER \n" +
             ") ";

		int pos = sql.indexOf("MERGE INTO T086_ITEMSALE_WEEK");
		ScriptParser p = new ScriptParser(sql);
		int count = p.getSize();
		assertEquals(2, count);
		int index = p.getCommandIndexAtCursorPos(pos + 5);
		assertEquals(1, index);
	}

	@Test
	public void testTrailingWhitespace()
		throws Exception
	{
		String sql =
			"1\n" +
			"/ \n" +
			"\n" +
			"-- some comment \n" +
			"2\n" +
			"/\n";
		ScriptParser p = new ScriptParser(sql);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		int count = p.getSize();
//		for (int i=0; i < count; i++)
//		{
//			System.out.println(p.getCommand(i) + "\n####################\n");
//		}
		assertEquals(2, count);
		assertEquals("1", p.getCommand(0));
		assertEquals("-- some comment \n2", p.getCommand(1));
	}

	@Test
	public void testPgScript()
	{
		String script =
			"create table one (id integer)\n" +
			"/?\n" +
			"\n" +
			"create table two (id integer)\n" +
			"/?\n" +
			"\n" +
			"do $$ \n" +
			"declare \n" +
			"    selectrow record; \n" +
			"begin \n" +
			"  for selectrow in \n" +
			"      select 'ALTER TABLE '|| t.table_name || ' ADD COLUMN foo integer NULL' as script\n" +
			"      from information_schema.tables t \n" +
			"      where t.table_schema = 'stuff' \n" +
			"  loop \n" +
			"    execute selectrow.script;\n" +
			"  end loop\n" +
			"end;\n" +
			"$$\n" +
			"/?\n";

		ScriptParser parser = new ScriptParser(script, ParserType.Postgres);
		parser.setAlternateDelimiter(new DelimiterDefinition("/?"));
		int count = parser.getStatementCount();
		assertEquals(3, count);
		assertEquals("create table one (id integer)", parser.getCommand(0));
		assertEquals("create table two (id integer)", parser.getCommand(1));
//		System.out.println(parser.getCommand(2));
		assertFalse(parser.getCommand(2).contains("/?"));
		assertTrue(parser.getCommand(2).trim().endsWith("$$"));

		script =
			"create table one (id integer)\n" +
			"GO\n" +
			"\n" +
			"create table two (id integer)\n" +
			"GO\n" +
			"\n" +
			"do $$ \n" +
			"declare \n" +
			"    selectrow record; \n" +
			"begin \n" +
			"  for selectrow in \n" +
			"      select 'ALTER TABLE '|| t.table_name || ' ADD COLUMN foo integer NULL' as script\n" +
			"      from information_schema.tables t \n" +
			"      where t.table_schema = 'stuff' \n" +
			"  loop \n" +
			"    execute selectrow.script;\n" +
			"  end loop\n" +
			"end;\n" +
			"$$\n" +
			"GO\n";

		parser = new ScriptParser(script, ParserType.Postgres);
		parser.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		count = parser.getStatementCount();
		assertEquals(3, count);
		assertEquals("create table one (id integer)", parser.getCommand(0));
		assertEquals("create table two (id integer)", parser.getCommand(1));
		assertFalse(parser.getCommand(2).contains("GO"));
		assertTrue(parser.getCommand(2).trim().endsWith("$$"));
	}

	@Test
	public void testDynamicDelimiter()
		throws Exception
	{
		String sql =
			"create or replace procedure foo \n" +
			"is \n" +
			"begin \n" +
			"  for rec in (select id from foo) loop\n" +
			"     delete from bar where id = rec.id;\n" +
			"  end loop;\n" +
			"end;\n" +
			"/\n" +
			"\n" +
			"create table bar (id integer not null primary key);\n";
		ScriptParser parser = new ScriptParser(ParserType.Oracle);
		parser.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		parser.setScript(sql);
		int size = parser.getSize();
		assertEquals(2, size);

		String create =
			"create or replace procedure foo \n" +
			"is \n" +
			"begin \n" +
			"  for rec in (select id from foo) loop\n" +
			"     delete from bar where id = rec.id;\n" +
			"  end loop;\n" +
			"end;";

		assertEquals(create, parser.getCommand(0).trim());
		assertEquals(DelimiterDefinition.DEFAULT_ORA_DELIMITER, parser.getDelimiterUsed(0));
		assertEquals("create table bar (id integer not null primary key)", parser.getCommand(1).trim());
		assertEquals(DelimiterDefinition.STANDARD_DELIMITER, parser.getDelimiterUsed(1));

		sql =
			"select * \n" +
			"from foo\n" +
			"/\n" +
			"delete from bar \n" +
			"where id = 42\n" +
			"/\n";

		parser.setScript(sql);
		size = parser.getSize();
		assertEquals(2, size);

		String script =
			"create type footype as object (id integer);\n" +
			"/\n" +
			"create or replace procedure foo \n" +
			"is \n" +
			"begin \n" +
			"  for rec in (select id from foo) loop\n" +
			"     delete from bar where id = rec.id;\n" +
			"  end loop;\n" +
			"end;\n" +
			"/\n" +
			"\n" +
			"create view v_foo\n" +
			"as\n" +
			"select *\n" +
			"from foo;\n" +
			"\n" +
			"insert into bar (c1, c2)\n" +
			"values (1,2)\n" +
			";";

		parser.setScript(script);
		size = parser.getSize();
		assertEquals(4, size);
		assertTrue(parser.getCommand(0).startsWith("create type footype"));
		assertEquals(DelimiterDefinition.DEFAULT_ORA_DELIMITER, parser.getDelimiterUsed(0));
		assertTrue(parser.getCommand(1).startsWith("create or replace procedure"));
		assertEquals(DelimiterDefinition.DEFAULT_ORA_DELIMITER, parser.getDelimiterUsed(1));
		assertTrue(parser.getCommand(2).startsWith("create view v_foo"));
		assertEquals(DelimiterDefinition.STANDARD_DELIMITER, parser.getDelimiterUsed(2));
		assertTrue(parser.getCommand(3).startsWith("insert into bar"));
		assertEquals(DelimiterDefinition.STANDARD_DELIMITER, parser.getDelimiterUsed(3));

		script =
			"whenever sqlerror continue;\n" +
			"\n" +
			"set serveroutput on;\n" +
			"\n" +
			"declare\n" +
			"  l_count integer;\n" +
			"begin\n" +
			"  l_count := 42;\n" +
			"end;\n" +
			"/\n" +
			"\n" +
			"begin\n" +
			"  execute immediate 'drop table foo';\n" +
			"end;\n" +
			"/\n" +
			"\n" +
			"select count(*) from dual;";

		parser.setScript(script);
		size = parser.getSize();
		assertEquals(5, size);
		assertTrue(parser.getCommand(0).startsWith("whenever"));
		assertEquals(DelimiterDefinition.STANDARD_DELIMITER, parser.getDelimiterUsed(0));
		assertTrue(parser.getCommand(1).startsWith("set serveroutput"));
		assertEquals(DelimiterDefinition.STANDARD_DELIMITER, parser.getDelimiterUsed(1));
		assertTrue(parser.getCommand(2).startsWith("declare"));
		assertEquals(DelimiterDefinition.DEFAULT_ORA_DELIMITER, parser.getDelimiterUsed(2));
		assertTrue(parser.getCommand(3).startsWith("begin"));
		assertEquals(DelimiterDefinition.DEFAULT_ORA_DELIMITER, parser.getDelimiterUsed(3));
		assertTrue(parser.getCommand(4).startsWith("select count(*)"));
		assertEquals(DelimiterDefinition.STANDARD_DELIMITER, parser.getDelimiterUsed(4));

		script =
			"drop procedure foo;\n" +
			"\n" +
			"create procedure foo\n" +
			"is\n" +
			"   l_value integer;\n" +
			"begin\n" +
			"\n" +
			"   l_value := 42;\n" +
			"\n" +
			"end;\n" +
			"/\n";
		parser.setScript(script);
		parser.setEmptyLineIsDelimiter(true);
		size = parser.getSize();
		assertEquals(2, size);
	}
	
}
