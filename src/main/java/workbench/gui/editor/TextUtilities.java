package workbench.gui.editor;

/*
 * TextUtilities.java - Utility functions used by the text area classes
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import workbench.resource.Settings;

import workbench.util.StringUtil;

/**
 * Class with several utility functions used by the text area component.
 * @author Slava Pestov
 * @version $Id: TextUtilities.java,v 1.6 2007/10/19 18:06:50 thomas Exp $
 */
public class TextUtilities
{

	/**
	 * Returns the offset of the bracket matching the one at the
	 * specified offset of the document, or -1 if the bracket is
	 * unmatched (or if the character is not a bracket).
	 *
	 * @param doc The document
	 * @param offset The offset
	 * @exception BadLocationException If an out-of-bounds access
	 * was attempted on the document text
	 */
	public static int findMatchingBracket(Document doc, int offset)
		throws BadLocationException
	{
		if (doc.getLength() == 0) return -1;

		char c = doc.getText(offset,1).charAt(0);
		char cprime; // corresponding character
		boolean backwards; // true = back, false = forward

		switch (c)
		{
			case '(':
				cprime = ')';
				backwards = false;
				break;
			case ')':
				cprime = '(';
				backwards = true;
				break;
			case '[':
				cprime = ']';
				backwards = false;
				break;
			case ']':
				cprime = '[';
				backwards = true;
				break;
			case '{':
				cprime = '}';
				backwards = false;
				break;
			case '}':
				cprime = '{';
				backwards = true;
				break;
			default:
				return -1;
		}

		int count;

		// How to merge these two cases is left as an exercise
		// for the reader.

		// Go back or forward
		if (backwards)
		{
			// Count is 1 initially because we have already
			// `found' one closing bracket
			count = 1;

			// Get text[0,offset-1];
			String text = doc.getText(0,offset);

			// Scan backwards
			for (int i = offset - 1; i >= 0; i--)
			{
				// If text[i] == c, we have found another
				// closing bracket, therefore we will need
				// two opening brackets to complete the
				// match.
				char x = text.charAt(i);
				if (x == c)
					count++;

				// If text[i] == cprime, we have found a
				// opening bracket, so we return i if
				// --count == 0
				else if(x == cprime)
				{
					if (--count == 0)
						return i;
				}
			}
		}
		else
		{
			// Count is 1 initially because we have already
			// `found' one opening bracket
			count = 1;

			// So we don't have to + 1 in every loop
			offset++;

			// Number of characters to check
			int len = doc.getLength() - offset;

			// Get text[offset+1,len];
			String text = doc.getText(offset,len);

			// Scan forwards
			for (int i = 0; i < len; i++)
			{
				// If text[i] == c, we have found another
				// opening bracket, therefore we will need
				// two closing brackets to complete the
				// match.
				char x = text.charAt(i);

				if(x == c)
					count++;

				// If text[i] == cprime, we have found an
				// closing bracket, so we return i if
				// --count == 0
				else if(x == cprime)
				{
					if (--count == 0)
						return i + offset;
				}
			}
		}

		// Nothing found
		return -1;
	}

	/**
	 * Locates the start of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 */
	public static int findWordStart(String line, int pos)
	{
		return findWordStart(line, pos, Settings.getInstance().getEditorNoWordSep());
	}

	public static int findWordStart(String line, int pos, String wordCharacters)
	{
		if (pos >= line.length())
		{
			pos = line.length();
		}

		if (pos < 1) return 0;

		char ch = line.charAt(pos - 1);


		if (Character.isWhitespace(ch))
		{
			return findWhitespaceBackwards(line, pos - 1);
		}

		boolean selectNoLetter = (!Character.isLetterOrDigit(ch) 	&& wordCharacters.indexOf(ch) == -1);

		int wordStart = 0;
		for (int i = pos - 1; i >= 0; i--)
		{
			ch = line.charAt(i);
			if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) && wordCharacters.indexOf(ch) == -1))
			{
				wordStart = i + 1;
				break;
			}
		}

		return wordStart;
	}

	private static int findWhitespaceBackwards(CharSequence line, int startPos)
	{
		if (line == null) return -1;
		int len = line.length();
		if (len == 0) return -1;
		if (startPos < 1) return -1;

		int pos = startPos;

		char c = line.charAt(pos);
		while (pos > 1)
		{
			if (c > ' ') return pos + 1;
			pos --;
			c = line.charAt(pos);
		}
		return -1;
	}

	/**
	 * Locates the end of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 */
	public static int findWordEnd(String line, int pos)
	{
		return findWordEnd(line, pos, Settings.getInstance().getEditorNoWordSep());
	}

	public static int findWordEnd(String line, int pos, String wordCharacters)
	{
		if(pos >= line.length()) return line.length();
		char ch = line.charAt(pos);

		if (Character.isWhitespace(ch))
		{
			return StringUtil.findFirstNonWhitespace(line, pos, false);
		}

		boolean selectNoLetter = (!Character.isLetterOrDigit(ch) && wordCharacters.indexOf(ch) == -1);

		int wordEnd = line.length();
		for (int i = pos; i < line.length(); i++)
		{
			ch = line.charAt(i);
			if (selectNoLetter ^ (!Character.isLetterOrDigit(ch) && wordCharacters.indexOf(ch) == -1))
			{
				wordEnd = i;
				break;
			}
		}
		return wordEnd;
	}
}
