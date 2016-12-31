/*
 * BracketCompleterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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

	@Test
	public void testGetOpeningChar()
	{
		BracketCompleter compl = new BracketCompleter("()[]\"\"''");
		char opening = compl.getOpeningChar('\'');
		assertEquals('\'', opening);

		opening = compl.getOpeningChar('"');
		assertEquals('"', opening);

		opening = compl.getOpeningChar(')');
		assertEquals('(', opening);

		opening = compl.getOpeningChar(']');
		assertEquals('[', opening);

		opening = compl.getOpeningChar('(');
		assertEquals(0, opening);

	}
}
