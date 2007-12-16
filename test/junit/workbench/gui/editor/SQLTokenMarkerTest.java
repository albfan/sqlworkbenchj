/*
 * SQLTokenMarkerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import junit.framework.TestCase;

/**
 * @author support@sql-workbench.net
 */
public class SQLTokenMarkerTest
	extends TestCase
{
	public SQLTokenMarkerTest(String testName)
	{
		super(testName);
	}

	public void testMarkTokensImpl()
	{
		try
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
				{Token.KEYWORD1, Token.NULL, Token.KEYWORD2, Token.NULL, Token.OPERATOR, Token.NULL, Token.LITERAL1 }, // WHERE ...
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
				printTokenLine(lineContent, token);
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
		catch (Exception ex)
		{
			ex.printStackTrace();
			fail(ex.getMessage());
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
					" expected: " + Token.typeString(expectedTokens[tokenIndex]) + " got: " + Token.typeString(token.id) + "", expectedTokens[tokenIndex], token.id);
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
