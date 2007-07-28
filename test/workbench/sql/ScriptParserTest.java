/*
 * ScriptParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import java.util.Iterator;
import junit.framework.*;
import workbench.TestUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class ScriptParserTest extends TestCase
{
	
	public ScriptParserTest(String testName)
	{
		super(testName);
	}

	public void testEmptyStatement()
	{
		try
		{
			// Check if a cursorposition at the far end of the statement is detected properly
			String sql = "select 42 from dual;\n\nselect * \nfrom table\n;;\n";	
			ScriptParser p = new ScriptParser();
			p.allowEmptyLineAsSeparator(false);
			p.setScript(sql);
			assertEquals(2, p.getSize());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testEndPosition()
	{
		try
		{
			// Check if a cursorposition at the far end of the statement is detected properly
			String sql = "select 42 from dual;\n\nselect * \nfrom table\n;";	
			ScriptParser p = new ScriptParser();
			p.allowEmptyLineAsSeparator(false);
			p.setScript(sql);
			int pos = sql.lastIndexOf(";");
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

	public void testCursorPosInCommand()
	{
		try
		{
			String script = "select 42 from dual;\n\nselect x\n        from y\n        \n        \n        ;";
			int pos = script.length() - 3;
			ScriptParser p = new ScriptParser();
			p.allowEmptyLineAsSeparator(false);
			p.setScript(script);
			int index = p.getCommandIndexAtCursorPos(pos);
			assertEquals(2, p.getSize());
			assertEquals(1, index);
			int sqlPos = p.getIndexInCommand(index, pos);
			String sql = p.getCommand(index);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testEmptyLines()
	{
		try
		{
			String sql = "select a,b,c\r\nfrom test\r\nwhere x = 1";
			ScriptParser p = new ScriptParser();
			p.allowEmptyLineAsSeparator(true);
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
	public void testSingleLineDelimiter()
	{
		String sql = "DROP\n" + 
			           "/\n" + 
			           "CREATE\n" +
								 "/\n";
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
						 "/\r\n";
			
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

	public void testAlternateFileParsing()
	{
		try
		{
			TestUtil util = new TestUtil("alternateFileParsing");
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
			ScriptParser p = new ScriptParser(0); 
			
			p.setDelimiter(new DelimiterDefinition("/", true));
			p.setSupportOracleInclude(false);
			p.setCheckForSingleLineCommands(false);
			p.setCheckEscapedQuotes(false);
			
			p.setFile(scriptFile);
			p.startIterator();
			int size = 0;
			while (p.hasNext())
			{
				size ++;
				String sql = p.getNextCommand();
				if (size == 2)
				{
					assertEquals("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent')", sql);
				}
			}
			assertEquals("Wrong number of statements", 5, size);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
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
	
	public void testMsGO()
	{
		String sql = "SELECT id \n" + 
								 "FROM person GO\n" + 
								 "  GO  \n" + 
								 " \n" + 
								 " \n" + 
								 "select * \n" + 
								 "from country \n" + 
								 "  GO \n\n ";		
		try
		{
			ScriptParser p = new ScriptParser(sql);
			// Test if the automatic detection of the MS SQL delimiter works
			p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
			int size = p.getSize();
			assertEquals("Wrong number of statements", 2, size);
			//System.out.println("sql=" + p.getCommand(0));
			assertEquals("Wrong statement returned", "SELECT id \nFROM person GO", p.getCommand(0));
			sql = "SELECT id \r\n" + 
						 "FROM person GO\r\n" + 
						 "  GO  \r\n" + 
						 " \r\n" + 
						 "select * \r\n" + 
						 "from country \r\n" + 
						 "  GO \r\n";		
			p.setScript(sql);
			size = p.getSize();
			assertEquals("Wrong number of statements", 2, size);
			assertEquals("Wrong statement returned", "SELECT id \r\nFROM person GO", p.getCommand(0));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
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
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
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
			
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

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
	
	public void testFileParsing()
	{
		try
		{
			int counter = 500;
			File script = createScript(counter, "\n");
			ScriptParser p = new ScriptParser(100);
			p.setFile(script);
			int count = 0;
			Iterator itr = p.getIterator();
			while (itr.hasNext())
			{
				String sql = (String)itr.next();
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
			count = 0;
			itr = p.getIterator();
			while (itr.hasNext())
			{
				String sql = (String)itr.next();
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
	
	public void testMutliStatements()
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
					 "-- comment comment comment comment comment comment comment. \n" + 
					 "-- comment comment comment comment comment comment comment. \n" + 
					 "-- comment comment comment comment comment comment comment. \n" + 
					 "-- comment comment comment comment comment comment comment. \n" + 
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
		p.setSupportOracleInclude(true);
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
	
	
}
