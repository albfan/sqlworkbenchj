/*
 * DefaultScriptParserTest.java
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

import junit.framework.TestCase;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.StringUtil;

/**
 *
 * @author thomas
 */
public class DefaultScriptParserTest extends TestCase
{
	
	public DefaultScriptParserTest(String testName)
	{
		super(testName);
	}

  public void testGetNextCommand()
  {
    String sql = "-- test select\nselect * from dual;\nupdate bla set x = 5;";
    DefaultScriptParser parser = new DefaultScriptParser(sql);

		String cmd = null;
		while ((cmd = parser.getNextCommand()) != null)
		{
			System.out.println("cmd=" + cmd);
		}
		
  }
	
}
