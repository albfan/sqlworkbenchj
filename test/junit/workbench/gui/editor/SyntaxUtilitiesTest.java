/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.editor;

import javax.swing.text.Segment;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SyntaxUtilitiesTest
{

	@Test
	public void testRegionMatchStart()
	{
		String lineText = "this is a line with some text in it.";
		Segment line = new Segment(lineText.toCharArray(), 0, lineText.length());
		int pos = SyntaxUtilities.findMatch(line, "line", true);
		assertEquals(10, pos);

		pos = SyntaxUtilities.findMatch(line, "xline", true);
		assertEquals(-1, pos);

		pos = SyntaxUtilities.findMatch(line, "this", true);
		assertEquals(0, pos);

		pos = SyntaxUtilities.findMatch(line, "it.", true);
		assertEquals(33, pos);

		lineText = "Line 1 Text\nLine 2 foo\nLine 4 bar\n";
		line = new Segment(lineText.toCharArray(), 0, 11);

		pos = SyntaxUtilities.findMatch(line, "foo", true);
		assertEquals(-1, pos);

		line.offset = 12;
		line.count = 10;
		pos = SyntaxUtilities.findMatch(line, "foo", true);
		assertEquals(7, pos);
	}


}
