/*
 * ScriptParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import junit.framework.*;
import java.util.List;
import workbench.TestUtil;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

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

	public void testSingleLineDelimiter()
	{
//		String sql = "-- test it\r\n" + 
//			           "DROP PROCEDURE test\r\n" + 
//			           "/\r\n" + 
//			           "CREATE OR REPLACE PROCEDURE test\r\n" + 
//								 "IS \r\n" + 
//								 "BEGIN \\n" + 
//								 "  SELECT a / b FROM someTable;\r\n" + 
//								 "END;\r\n" +
//								 "/\r\n";

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
			
			assertEquals("Delimiter returned", false, p.getCommand(0).trim().endsWith("/"));
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
			TestUtil util = new TestUtil();
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
			ScriptParser p = new ScriptParser(100); 
			
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
				p.getNextCommand();
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
			//p.setAlternateDelimiter(new DelimiterDefinition("./", false));
			p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
			int size = p.getSize();
			assertEquals("Wrong number of statements", 2, size);
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
	
	public void testFileParsing()
	{
		try
		{
			File tempdir = new File(System.getProperty("java.io.tmpdir"));
			File script = new File(tempdir, "largefile.sql");
			BufferedWriter out = new BufferedWriter(new FileWriter(script));
			int counter = 5000;
			for (int i = 0; i < counter; i++)
			{
				out.write("--- test command\n");
				out.write("insert into test_table\n(col1, col2, col3, col4)\nvalues ('1','2''',3,' a two line \n; quoted text');\n\n");
			}
			out.close();
			ScriptParser p = new ScriptParser(500);
			p.setFile(script);
			int count = 0;
			Iterator itr = p.getIterator();
			while (itr.hasNext())
			{
				String sql = (String)itr.next();
				assertNotNull("No SQL returned at " + count, sql);
				String verb = SqlUtil.getSqlVerb(sql);
				assertEquals("Wrong statement retrieved", "insert", verb.toLowerCase());
				count ++;
			}
			p.done();
			assertEquals("Wrong number of statements", counter, count);
			script.delete();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}
	
	public void testParser()
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
		
		sql = "SELECT distinct t.KEY \n" + 
					"FROM translation t, content_folder f \n" + 
					"WHERE t.key = f.folder_name \n" + 
					"--AND   LANGUAGE = 'en' \n" + 
					"; \n" + 
					" \n" + 
					"WBDIFF -sourceprofile=\"CMTS\" \n" + 
					"       -file=c:/temp/test.xml \n" + 
					"       -includeindex=false \n" + 
					"       -includeforeignkeys=false \n" + 
					"       -includeprimarykeys=false \n" + 
					";       ";			
		p = new ScriptParser(sql);
		assertEquals(2, p.getSize());
	}

	
	
}
