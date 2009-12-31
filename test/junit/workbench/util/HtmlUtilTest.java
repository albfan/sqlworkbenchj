/*
 * HtmlUtilTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class HtmlUtilTest extends TestCase
{
	
	public HtmlUtilTest(String testName)
	{
		super(testName);
	}

  public void testEscapeHTML()
	{
		try
		{
			String input = "<sometag> sometext";
			String escaped = HtmlUtil.escapeHTML(input);
			assertEquals("&lt;sometag&gt; sometext", escaped);
			
			input = "a &lt; b";
			escaped = HtmlUtil.escapeHTML(input);
			assertEquals("a &amp;lt; b", escaped);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	
}
