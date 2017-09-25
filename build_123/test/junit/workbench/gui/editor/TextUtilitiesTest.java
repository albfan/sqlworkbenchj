/*
 * TextUtilitiesTest.java
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

import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TextUtilitiesTest
	extends WbTestCase
{

	public TextUtilitiesTest()
	{
		super("TextUtilitiesTest");
	}

	@Test
	public void testFindMatchingBracket()
		throws Exception
	{
		String text = " this is { ( a test ) } ";
		SyntaxDocument doc = new SyntaxDocument();
		doc.insertString(0, text, null);

		int pos = TextUtilities.findMatchingBracket(doc, 0);
		assertEquals(-1, pos);

		int closingPos = text.indexOf('}');
		int openPos = text.indexOf('{');
		pos = TextUtilities.findMatchingBracket(doc, openPos);
		assertEquals(closingPos, pos);
		pos = TextUtilities.findMatchingBracket(doc, closingPos);
		assertEquals(openPos, pos);

		closingPos = text.indexOf(')');
		openPos = text.indexOf('(');
		pos = TextUtilities.findMatchingBracket(doc, openPos);
		assertEquals(closingPos, pos);
		pos = TextUtilities.findMatchingBracket(doc, closingPos);
		assertEquals(openPos, pos);
	}

	@Test
	public void testFindWordStart()
	{
		String line = "this is a test";
		int start = TextUtilities.findWordStart(line, 4);
		assertEquals(0, start);

		start = TextUtilities.findWordStart(line, 6);
		assertEquals(5, start);

		start = TextUtilities.findWordStart("test", 6);
		assertEquals(0, start);

		start = TextUtilities.findWordStart("test", 4);
		assertEquals(0, start);

		start = TextUtilities.findWordStart("test", 5);
		assertEquals(0, start);

		start = TextUtilities.findWordStart(line, 14);
		assertEquals(10, start);

		line = "xyz   ";
		start = TextUtilities.findWordStart(line, line.length() - 1);
		assertEquals(line.indexOf('z') + 1, start);
	}

	@Test
	public void testFindWordEnd()
	{
		String line = "this is a test";
		int end = TextUtilities.findWordEnd(line, 2);
		assertEquals(4, end);

		end = TextUtilities.findWordEnd(line, 0);
		assertEquals(4, end);

		end = TextUtilities.findWordEnd(line, 5);
		assertEquals(7, end);

		end = TextUtilities.findWordEnd(line, 10);
		assertEquals(14, end);

		line = "   (test)";
		end = TextUtilities.findWordEnd(line, 0);
		assertEquals(line.indexOf('('), end);

		line = "   test";
		end = TextUtilities.findWordEnd(line, 0);
		assertEquals(line.indexOf('t'), end);

	}
}
