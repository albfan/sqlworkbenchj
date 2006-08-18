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
import junit.framework.*;
import java.util.List;
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
			List l = p.getCommands();
			assertEquals("Not enough commands", 3, l.size());
			
			String c = (String)l.get(0);
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
				out.write("insert into test_table\n(col1, col2, col3, col4)\nvalues ('1','2''',3,' a very long \n quoted text');\n\n");
			}
			out.close();
			ScriptParser p = new ScriptParser(1000);
			p.setFile(script);
			int count = 0;
			p.getIterator();
			ScriptCommandDefinition def = p.getNextCommand();
			while (def != null)
			{
				String sql = def.getSQL();
				assertNotNull("No SQL returned " + count, sql);
				String verb = SqlUtil.getSqlVerb(sql);
				assertEquals("Wrong statement retrieved", "insert", verb.toLowerCase());
				count ++;
				def = p.getNextCommand();
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
		List l = p.getCommands();
		assertEquals(3, l.size());
		assertEquals("select * from template_field_label", l.get(1));

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
		l = p.getCommands();
		assertEquals(4, l.size());
		String verb = SqlUtil.getSqlVerb((String)l.get(1));
		assertEquals("drop", verb.toLowerCase());
		String s = (String)l.get(0);
		String clean = SqlUtil.makeCleanSql(s, false, false, '\'');
		assertEquals("alter table participants drop constraint r_05", clean);

		s = (String)l.get(2);
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
		l = p.getCommands();
		assertEquals(2, l.size());
	}

	
}
