/*
 * ScriptParserTest.java
 * JUnit based test
 *
 * Created on May 11, 2006, 9:54 PM
 */

package workbench.sql;

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

	protected void setUp() throws Exception
	{
	}

	protected void tearDown() throws Exception
	{
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
             "* $URL: svn+ssh://nichdexp.nichd.nih.gov/subversion/mtrac/trunk/db/00-release-1.0/01-trac-8-ddl.sql $ \n" + 
             "* $Revision: 1.1 $ \n" + 
             "* Created by Janek K. Claus. \n" + 
             "* $LastChangedBy: clausjan $ \n" + 
             "* $LastChangedDate: 2006-05-05 20:29:15 -0400 (Fri, 05 May 2006) $ \n" + 
             "*/ \n" + 
             "-- This is the initial creation script for the MTrac database. \n" + 
             "-- It assumes the database space and schema have been setup. \n" + 
             "-- Each table can be indivually recreated, if you add foreign keys, make sure that they will be deleted in all \n" + 
             "-- associated tables before dropping. \n" + 
             " \n" + 
             "-- ############################################# \n" + 
             "-- ##                                         ## \n" + 
             "-- ##              Organizations              ## \n" + 
             "-- ##                                         ## \n" + 
             "alter table participants drop constraint r_05;   -- make sure you recreate this foreign key after inserting data! \n" + 
             "drop table organizations;\n" +
						 "@include.sql";
		p.setScript(sql);
		p.setSupportOracleInclude(true);
		l = p.getCommands();
		assertEquals(3, l.size());
		assertEquals("drop table organizations", l.get(1));
		String s = (String)l.get(0);
		String clean = SqlUtil.makeCleanSql(s, false, false, '\'');
		assertEquals("alter table participants drop constraint r_05", clean);

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
