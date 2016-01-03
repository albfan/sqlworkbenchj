/*
 * SQLTokenMarkerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Thomas Kellerer
 */
public class SQLTokenMarkerTest
	extends WbTestCase
{
	public SQLTokenMarkerTest()
	{
		super("SQLTokenMarkerTest");
	}

	@Test
	public void testMultiLineComment()
		throws Exception
	{
		SyntaxDocument doc = new SyntaxDocument();

		SQLTokenMarker marker = new AnsiSQLTokenMarker();
		doc.setTokenMarker(marker);

		String sql = "/* first line comment \n" +
			"second line */\n" +
			"SELECT x FROM thetable;\n" +
			"-- line comment\n" +
			"SELECT x from y;\n" +
			"/* \n" +
			" block comment \n\n" +
			"*/\n" +
			"select x from y;";

		doc.insertString(0, sql, null);

		Segment lineContent = new Segment();
		int lineCount = doc.getDefaultRootElement().getElementCount();
//			assertEquals(9, lineCount);
//			assertEquals(9, marker.getLineCount());

		// Expected token IDs
		int[][] expectedTokens = new int[][]
		{
			{Token.COMMENT1 }, // First line opens the block comment
			{Token.COMMENT1, Token.NULL }, // Second line continues the block comment and closes it
			{Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL },
			{Token.COMMENT2 }, // Line comment
			{Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL },
			{Token.COMMENT1 }, // Second block comment
			{Token.COMMENT1 },
			{Token.NULL },
			{Token.COMMENT1, Token.NULL },
			{Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL }
		};

		for (int lineIndex = 0; lineIndex < lineCount; lineIndex ++)
		{
			getLineText(doc, lineIndex, lineContent);
			Token token = marker.markTokens(lineContent, lineIndex);
			verifyLine(expectedTokens, token, lineIndex);
		}
	}

	@Test
	public void testMultiLineLiteral()
		throws Exception
	{
		SyntaxDocument doc = new SyntaxDocument();

		SQLTokenMarker marker = new AnsiSQLTokenMarker();
		doc.setTokenMarker(marker);

		String sql =
			"SELECT x FROM thetable\n" +
			"WHERE desc = 'this is a \n" +
			" multiline \n" +
			"string literal' \n" +
			"AND name = 'arthur'\n" +
			"AND col = 5\n" +
			"AND filename IS NOT NULL \n" +
			"AND   http_status IN ('200')";

		// Expected token IDs
		int[][] expectedTokens = new int[][]
		{
			{Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL }, // SELECT ...
			{Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL, Token.OPERATOR, Token.NULL, Token.LITERAL1 }, // WHERE ...
			{Token.LITERAL1 }, // multiline
			{Token.LITERAL1, Token.NULL }, // string literal'
			{Token.KEYWORD1, Token.NULL, Token.OPERATOR, Token.NULL, Token.LITERAL1 }, // AND name = ...
			{Token.KEYWORD1, Token.NULL, Token.OPERATOR, Token.NULL, Token.LITERAL1 }, // AND col = ...
			{Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL} , // AND filename ...
			{Token.KEYWORD1, Token.NULL, Token.KEYWORD1, Token.NULL, Token.NULL, Token.LITERAL1, Token.NULL} // AND htpp ...
		};

		doc.insertString(0, sql, null);

		Segment lineContent = new Segment();
		int lineCount = doc.getDefaultRootElement().getElementCount();
		assertEquals(8, lineCount);
		assertEquals(8, marker.getLineCount());

		for (int lineIndex = 0; lineIndex < lineCount; lineIndex ++)
		{
			getLineText(doc, lineIndex, lineContent);
			Token token = marker.markTokens(lineContent, lineIndex);
//				printTokenLine(lineContent, token);
			verifyLine(expectedTokens, token, lineIndex);
		}

		// Test access in the middle of the text
		for (int lineIndex = 2; lineIndex < lineCount; lineIndex++)
		{
			getLineText(doc, lineIndex, lineContent);
			Token token = marker.markTokens(lineContent, lineIndex);
			verifyLine(expectedTokens, token, lineIndex);
		}
	}

	private void verifyLine(int[][] expectedLines, Token token, int lineIndex)
	{
		if (lineIndex < 0 || lineIndex > expectedLines.length) fail("Wrong line index");
		int[] expectedTokens = expectedLines[lineIndex];
		int tokenIndex = 0;
		while (token != null)
		{
			if (tokenIndex >= expectedTokens.length)
			{
				fail("Line " + lineIndex + " has " + (tokenIndex + 1) + " tokens, but only " + expectedTokens.length + " tokens defined for test.");
			}
			else
			{
				assertEquals("Wrong token at line: " + lineIndex + ", token: " + tokenIndex +
					" expected: " + Token.typeString(expectedTokens[tokenIndex]) + " got: " + Token.typeString(token.id), expectedTokens[tokenIndex], token.id);
			}
			token = token.next;
			tokenIndex ++;
		}
	}

	private void printTokenLine(Segment line, Token tokens)
	{
		while (tokens != null)
		{
			int length = tokens.length;
			line.count = length;
			System.out.print("[" + tokens.typeString() + "]" + line.toString());
			line.offset += length;
			tokens = tokens.next;
		}
		System.out.println("");
	}

	private void getLineText(SyntaxDocument doc, int lineIndex, Segment lineContent)
		throws BadLocationException
	{
		Element lineElement = doc.getDefaultRootElement().getElement(lineIndex);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		doc.getText(start, end - start - 1, lineContent);
	}
}
