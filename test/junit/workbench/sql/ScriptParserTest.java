/*
 * ScriptParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import static org.junit.Assert.*;
import org.junit.Test;

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
		ScriptParser p = new ScriptParser(sql);
		p.setDelimiter(DelimiterDefinition.STANDARD_DELIMITER);
		p.setCheckForSingleLineCommands(false);
		p.setSupportOracleInclude(false);
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
			"wbexport  -type=text -delimiter=';' -quoteChar=\"'\" -sourceTable=test -file=test.txt; \n" +
      "wbimport  -type=text;";
		
		ScriptParser p = new ScriptParser(sql);
		int count = 0;
		while (p.getNextCommand() != null)
		{
			count ++;
		}
		assertEquals(2, count);
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
		int commandsInFile = 0;
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
			parser = new ScriptParser(500);
			parser.setCheckForSingleLineCommands(true);
			parser.setFile(f, "UTF-8");
			assertEquals(scriptSize, parser.getScriptLength());
			parser.startIterator();

			commandsInFile = 0;
			while (parser.hasNext())
			{
				String sql = "insert into address (id, street) \nvalues \n(" + commandsInFile + ", ' \u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153')";
				String command = parser.getNextCommand();
				assertEquals(sql, command.trim());
				commandsInFile++;
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
	{
		try
		{
			// Check if a cursorposition at the far end of the statement is detected properly
			String sql = "command1\n;;command2\n;\n";
			ScriptParser p = new ScriptParser();
			p.setEmptyLineIsDelimiter(false);
			p.setScript(sql);
//			assertEquals(2, p.getSize());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testCursorInEmptyLine()
	{
		try
		{
			String sql = "\nselect 42\nfrom dual;\nselect * \nfrom table\n;";
			ScriptParser p = new ScriptParser();
			p.setEmptyLineIsDelimiter(false);
			p.setScript(sql);
			int index = p.getCommandIndexAtCursorPos(0);
			assertEquals("Wrong statement index", 0, index);
			assertEquals(2, p.getSize());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEndPosition()
	{
		try
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
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testCursorPosInCommand()
	{
		try
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
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEmptyLines()
	{
		try
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
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Test
	public void testSingleLineDelimiter()
	{
		String sql = "DROP\n" +
			           "/ \n" +
			           "CREATE\n" +
								 "/ \n";
		try
		{
			ScriptParser p = new ScriptParser();
			p.setAlternateDelimiter(new DelimiterDefinition("/", true));
			p.setCheckForSingleLineCommands(false);
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
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testAlternateFileParsing()
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.prepareEnvironment();

		File scriptFile = new File(util.getBaseDir(), "testscript.sql");
		PrintWriter writer = new PrintWriter(new FileWriter(scriptFile));
		writer.println("-- test script");
		writer.println("CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100))");
		writer.println("/");
		writer.println("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent')");
		writer.println("/");
		writer.println("insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect')");
		writer.println("/");
		writer.println("insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox')");
		writer.println("/");
		writer.println("commit");
		writer.println("/");
		writer.close();

		// Make sure the iterating parser is used, by setting
		// a very low max file size
		ScriptParser p = new ScriptParser(10);

		p.setDelimiter(new DelimiterDefinition("/", true));
		p.setSupportOracleInclude(false);
		p.setCheckForSingleLineCommands(false);
		p.setCheckEscapedQuotes(false);

		p.setFile(scriptFile);
		p.startIterator();
		int size = 0;
		while (p.hasNext())
		{
			String sql = p.getNextCommand();
			if (sql == null) break;
			if (StringUtil.isBlank(sql)) break;
			size ++;

			if (size == 2)
			{
				assertEquals("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent')", sql);
			}
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
		try
		{
			ScriptParser p = new ScriptParser(sql);
			int size = p.getSize();
			assertEquals("Wrong number of statements", 2, size);
			assertEquals("Wrong statement returned", "SELECT id,';' \nFROM person", p.getCommand(0));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testMsGO()
	{
		String sql = "SELECT id \n" +
								 "FROM person GO\n" +
								 "  GO  \n" +
								 " \n" +
								 " \n" +
								 "select * \n" +
								 "from country \n" +
								 "  GO";
		try
		{
			ScriptParser p = new ScriptParser(sql);
			// Test if the automatic detection of the MS SQL delimiter works
			p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
			p.setCheckForSingleLineCommands(false);
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
			p.setAlternateDelimiter(new DelimiterDefinition("RUN", true));
			size = p.getSize();
			assertEquals("Wrong number of statements", 2, size);
			assertEquals("SET QUOTED_IDENTIFIER ON", p.getCommand(0));
			assertEquals("SET ANSI_NULLS ON", p.getCommand(1));

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
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
		ScriptParser p = new ScriptParser(sql);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		p.setCheckForSingleLineCommands(false);
		int size = p.getSize();
		assertEquals(2, size);
	}

	@Test
	public void testAlternateDelimiter()
	{
		String sql = "SELECT id \n" +
								 "FROM person \n" +
								 "# \n" +
								 " \n" +
								 " \n" +
								 "select * \n" +
								 "from country \n" +
								 "#";
		try
		{
			ScriptParser p = new ScriptParser(sql);
			p.setAlternateDelimiter(new DelimiterDefinition("#", true));
			int size = p.getSize();
			assertEquals("Wrong number of statements", 2, size);

			p.setAlternateDelimiter(new DelimiterDefinition("./", false));
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
			p.setAlternateDelimiter(new DelimiterDefinition("/", true));
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
			p.setAlternateDelimiter(new DelimiterDefinition("/", false));
			size = p.getSize();
			String expected = sql.substring(0, sql.lastIndexOf('/') - 1).trim();
			String cmd = p.getCommand(0);
//			System.out.println("--- sql ---\n" + cmd + "\n----- expected -----\n" + expected + "\n-----------");
			assertEquals(expected, cmd);

			p.setScript(sql);
			p.setAlternateDelimiter(new DelimiterDefinition("/", true));
			size = p.getSize();
			cmd = p.getCommand(0);
//			System.out.println("--- sql ---\n" + cmd + "\n----- expected -----\n" + expected + "\n-----------");
			assertEquals(expected, cmd);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
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
			ScriptParser p = new ScriptParser(sql);
			p.setCheckForSingleLineCommands(true);
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
			p = new ScriptParser(sql);
			p.setCheckForSingleLineCommands(true);
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
		BufferedWriter out = new BufferedWriter(new FileWriter(script));
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
		out.close();
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

		ScriptParser p = new ScriptParser(sql);
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

		p.setSupportOracleInclude(true);
		p.setScript(sql);
		assertEquals(4, p.getSize());
		String verb = SqlUtil.getSqlVerb(p.getCommand(1));
		assertEquals("drop", verb.toLowerCase());
		String s = p.getCommand(0);
		String clean = SqlUtil.makeCleanSql(s, false, false, '\'');
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
		clean = SqlUtil.makeCleanSql(s, false, false, '\'');
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
	public void testSingleLineStatements()
	{
		try
		{
			String sql = "set nocount on\ndeclare @x int\nselect 123;";
			ScriptParser p = new ScriptParser(sql);
			p.setCheckForSingleLineCommands(true);
			assertEquals(3, p.getSize());

			sql = "declare @x int\nset nocount on\nselect 123;";
			p = new ScriptParser(sql);
			p.setCheckForSingleLineCommands(true);
			assertEquals(3, p.getSize());
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
    ScriptParser parser = new ScriptParser(sql);
		parser.setSupportIdioticQuotes(true);
		int count = parser.getSize();
		assertEquals(2, count);
		assertEquals("SELECT * FROM [Some;Table]", parser.getCommand(0).trim());

		sql = "SELECT '[SomeTable];' FROM dual;DELETE FROM \"[Other];Table\";";
    parser = new ScriptParser(sql);
		parser.setSupportIdioticQuotes(true);
		count = parser.getSize();
		assertEquals(2, count);
		assertEquals("SELECT '[SomeTable];' FROM dual", parser.getCommand(0).trim());
		assertEquals("DELETE FROM \"[Other];Table\"", parser.getCommand(1).trim());

		sql = "SELECT * FROM [Some;Table];DELETE FROM [Other;Table];";
    parser = new ScriptParser(sql);
		parser.setSupportIdioticQuotes(false);
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
    ScriptParser parser = new ScriptParser(sql);
		parser.setAlternateLineComment("#");
		int count = parser.getSize();
		assertEquals("Wrong statement count", 3, count);

		sql =  "-- this is a non-standard comment;\n" +
									"select * from test1;\n"+
									"-- another non-standard comment;\n"+
									"select * from test2;\n" +
									"-- standard comment;\n"+
									"select * from test3;\n";
    parser = new ScriptParser(sql);
		parser.setAlternateLineComment("");
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
		String cmd = parser.getCommand(index);
//		System.out.println("cmd=" + cmd);
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
}
