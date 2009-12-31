/*
 * SearchAndReplaceTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

/**
 *
 * @author Thomas Kellerer
 */
public class SearchAndReplaceTest extends junit.framework.TestCase
{
	
	public SearchAndReplaceTest(String testName)
	{
		super(testName);
	}
	
	public void testCreateSearchPattern()
	{
		String input = "thetext";
		String expression = SearchAndReplace.getSearchExpression(input, false, false, false);
		assertEquals("Wrong expression", "(" + input + ")", expression);
		
		expression = SearchAndReplace.getSearchExpression(input, false, true, false);
		assertEquals("Wrong expression", "\\b(" + input + ")\\b", expression);
		
		expression = SearchAndReplace.getSearchExpression(input, true, true, false);
		assertEquals("Wrong expression", "(?i)\\b(" + input + ")\\b", expression);

		expression = SearchAndReplace.getSearchExpression(input, true, true, true);
		assertEquals("Wrong expression", "(?i)\\b" + input + "\\b", expression);

		expression = SearchAndReplace.getSearchExpression(input, true, false, true);
		assertEquals("Wrong expression", "(?i)" + input, expression);

		expression = SearchAndReplace.getSearchExpression(input, false, false, true);
		assertEquals("Wrong expression", input, expression);
	}

}

