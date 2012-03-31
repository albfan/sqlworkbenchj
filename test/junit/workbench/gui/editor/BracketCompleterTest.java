/*
 * BracketCompleterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.editor;

import org.junit.*;
import static org.junit.Assert.*;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class BracketCompleterTest
	extends WbTestCase
{

	public BracketCompleterTest()
	{
		super("BracketCompleterTest");
	}

	/**
	 * Test of isValidDefinition method, of class BracketCompleter.
	 */
	@Test
	public void testIsValidDefinition()
	{
		assertTrue(BracketCompleter.isValidDefinition(""));
		assertTrue(BracketCompleter.isValidDefinition("()"));
		assertFalse(BracketCompleter.isValidDefinition("()["));
	}

	@Test
	public void testGetMatching()
	{
		BracketCompleter compl = new BracketCompleter("()[]\"\"''");
//		System.out.println("using: " + compl.toString());
		assertEquals(")", compl.getCompletionChar('('));
		assertNull(compl.getCompletionChar('{'));
		assertEquals("]", compl.getCompletionChar('['));
		assertEquals("\"", compl.getCompletionChar('"'));

		compl = new BracketCompleter("");
		assertNull(compl.getCompletionChar(')'));
	}
}
