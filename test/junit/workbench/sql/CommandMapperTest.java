/*
 * CommandMapperTest.java
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class CommandMapperTest
	extends TestCase
{
	public CommandMapperTest(String testName)
	{
		super(testName);
	}

	public void testSelectIntoPattern()
	{
		TestUtil util = new TestUtil("testPattern");
		try
		{
			String pgPattern = Settings.getInstance().getProperty("workbench.db.postgresql.selectinto.pattern", null);
			assertNotNull(pgPattern);

			Pattern pg = Pattern.compile(pgPattern, Pattern.CASE_INSENSITIVE);
			String sql = "select * from table";
			Matcher m = pg.matcher(sql);
			assertFalse(m.find());
			
			sql = "wbselectblob blob_column into c:/temp/pic.jpg from mytable";
			m = pg.matcher(sql);
			assertFalse(m.find());			

			sql = "select col1, col2, col3 INTO new_table FROM existing_table";
			m = pg.matcher(sql);
			assertTrue(m.find());			
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		try
		{
			String informixPattern = Settings.getInstance().getProperty("workbench.db.informix_dynamic_server.selectinto.pattern", null);
			assertNotNull(informixPattern);
			
			Pattern ifx = Pattern.compile(informixPattern, Pattern.CASE_INSENSITIVE);
			String sql = "select * from table";
			Matcher m = ifx.matcher(sql);
			assertFalse(m.find());
			
			sql = "wbselectblob blob_column into c:/temp/pic.jpg from mytable";
			m = ifx.matcher(sql);
			assertFalse(m.find());			

			sql = "select col1, col2, col3 FROM existing_table INTO new_table";
			m = ifx.matcher(sql);
			assertTrue(m.find());			
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
}
