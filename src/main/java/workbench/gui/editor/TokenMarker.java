/*
 * TokenMarker.java - Generic token marker
 * Copyright (C) 1998, 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */
package workbench.gui.editor;

import java.util.ArrayList;

import javax.swing.text.Segment;

/**
 * A token marker that splits lines of text into tokens. Each token carries
 * a length field and an indentification tag that can be mapped to a color
 * for painting that token.<p>
 *
 * For performance reasons, the linked list of tokens is reused after each
 * line is tokenized. Therefore, the return value of <code>markTokens</code>
 * should only be used for immediate painting. Notably, it cannot be
 * cached.
 *
 * @author Slava Pestov, Thomas Kellerer
 * @see Token
 */
public abstract class TokenMarker
{
	/**
	 * Stores the first {@link Token} for each line that
	 * has been tokenized
	 */
	protected ArrayList<Token> lineStartTokens = new ArrayList<>(250);

	/**
	 * The number of lines in the model being tokenized. This can be
	 * less than the length of the <code>lineInfo</code> array.
	 */
	protected int length;

	/**
	 * Creates a new <code>TokenMarker</code>. This DOES NOT create
	 * a lineInfo array; an initial call to <code>insertLines()</code>
	 * does that.
	 */
	protected TokenMarker()
	{
	}

	public Token getLastTokenInLine(int lineIndex)
	{
		Token start = getFirstTokenInLine(lineIndex);

		if (start == null) return null;

		while (start != null)
		{
			if (start.next == null)
			{
				return start;
			}
			start = start.next;
		}
		return null;
	}

	private Token findTokenForPosition(int line, int positionInLine)
	{
		Token tok = getFirstTokenInLine(line);
		if (tok == null) return null;
		int pos = 0;
		while (tok != null)
		{
			if (pos <= positionInLine && positionInLine <= pos + tok.length) return tok;
			pos += tok.length;
			tok = tok.next;
			if (pos > positionInLine) return null;
		}
		return null;
	}

	public boolean isStringLiteralAt(int line, int positionInLine)
	{
		Token tok = findTokenForPosition(line, positionInLine);
		if (tok == null) return false;
		if (tok.isLiteral())
		{
			char pending = getPendingLiteralChar();
			return pending == 0;
		}
		return false;
	}

	public Token getFirstTokenInLine(int lineIndex)
	{
		if (lineIndex < 0 || lineIndex >= length)
		{
			return null;
		}
		return lineStartTokens.get(lineIndex);
	}

	public Token getToken(Segment line, int lineIndex)
	{
		Token token = getFirstTokenInLine(lineIndex);
		if (token == null)
		{
			token = markTokens(line, lineIndex);
		}
		return token;
	}

	protected Token getPreviousLineToken(int lineIndex)
	{
		Token token = null;
		while (lineIndex > 0)
		{
			lineIndex--;
			token = getLastTokenInLine(lineIndex);
			if (token != null)
			{
				break;
			}
		}
		return token;
	}

	/**
	 * Clears the tokens for the specified line.
	 * This is to try to give the GC a hint to quickly free up
	 * the memory allocated by the Tokens.  When loading large
	 * files, the Token instances are consuming the most memory
	 */
	protected synchronized void clearLine(int lineIndex)
	{
		Token first = getFirstTokenInLine(lineIndex);
		if (first == null) return;

		while (first.next != null)
		{
			first = first.next;
			first.next = null;
		}
		lineStartTokens.set(lineIndex, null);
	}

	/**
	 * A wrapper for the lower-level <code>markTokensImpl</code> method
	 * that is called to split a line up into tokens.
	 * @param line The line
	 * @param lineIndex The line number
	 */
	public synchronized Token markTokens(Segment line, int lineIndex)
	{
		if (lineIndex >= length) return null;

		Token prev = getPreviousLineToken(lineIndex);

		lineStartTokens.set(lineIndex, null);

		markTokensImpl(prev, line, lineIndex);

		// tell the last token if it has a pending literal character (" or ')
		Token t = getLastTokenInLine(lineIndex);
		if (t != null) t.setPendingLiteralChar(getPendingLiteralChar());

		return lineStartTokens.get(lineIndex);
	}

	/**
	 * An abstract method that splits a line up into tokens. It
	 * should parse the line, and call <code>addToken()</code> to
	 * add syntax tokens to the token list. Then, it should return
	 * the initial token type for the next line.<p>
	 *
	 * For example if the current line contains the start of a
	 * multiline comment that doesn't end on that line, this method
	 * should return the comment token type so that it continues on
	 * the next line.
	 *
	 * @param lastToken The initial token type for this line
	 * @param line The line to be tokenized
	 * @param lineIndex The index of the line in the document, starting at 0
	 */
	protected abstract void markTokensImpl(Token lastToken, Segment line, int lineIndex);

	public abstract char getPendingLiteralChar();

	public void reset()
	{
		for (int i=0; i < lineStartTokens.size(); i++)
		{
			clearLine(i);
		}
		lineStartTokens = new ArrayList<>(250);
		length = 0;
	}

	/**
	 * Informs the token marker that lines have been inserted into
	 * the document.
	 * @param index The first line number
	 * @param lines The number of lines
	 */
	public void insertLines(int index, int lines)
	{
		if (lines <= 0)	return;
		length += lines;

		// Expand the array, so that the subsequent for-next loop
		// does not re-allocated the internal array each time
		lineStartTokens.ensureCapacity(length);

		// now make sure that every index is valid
		int len = index + lines;
		for (int i = index; i < len; i++)
		{
			lineStartTokens.add(index, null);
		}
	}

	/**
	 * Informs the token marker that line have been deleted from
	 * the document. This removes the lines in question from the
	 * <code>lineInfo</code> array.
	 * @param index The first line number
	 * @param lines The number of lines
	 */
	public void deleteLines(int index, int lines)
	{
		if (lines <= 0)	return;
		int end = index + lines;
		length -= lines;
		for (int i=index; i < end; i++)
		{
			clearLine(index);
			lineStartTokens.remove(index);
		}
	}

	/**
	 * Returns the number of lines in this token marker.
	 */
	public int getLineCount()
	{
		return length;
	}

	/**
	 * Adds a token to the token list.
	 *
	 * @param length The length of the token
	 * @param id The id of the token
	 */
	protected Token addToken(int lineIndex, int length, byte id)
	{
		if (length == 0) return null;

		Token newToken = new Token(length, id);
		Token firstToken = lineStartTokens.get(lineIndex);
		if (firstToken == null)
		{
			lineStartTokens.set(lineIndex, newToken);
		}
		else
		{
			while (firstToken.next != null)
			{
				firstToken = firstToken.next;
			}
			firstToken.next = newToken;
		}
		return newToken;
	}

}
