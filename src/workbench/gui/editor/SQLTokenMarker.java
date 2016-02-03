/*
 * SQLTokenMarker.java - Generic SQL token marker
 * Copyright (C) 1999 mike dillon
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */
package workbench.gui.editor;

import javax.swing.text.Segment;

/**
 * SQL token marker.
 *
 * @author mike dillon
 */
public class SQLTokenMarker
	extends TokenMarker
{
	private int offset, lastOffset, lastKeyword;
	protected boolean isMySql;
	protected boolean isMicrosoft;
	protected KeywordMap keywords;
	private char literalChar = 0;

	@Override
	public char getPendingLiteralChar()
	{
		return literalChar;
	}

	private byte getLiteralId(char literal)
	{
		switch (literal)
		{
			case '\'':
				return Token.LITERAL1;
			case '"':
				return Token.LITERAL2;
			case '[':
			case ']':
        if (isMicrosoft)
        {
          return Token.LITERAL2;
        }
        return 0;
      case '`':
        if (isMySql)
        {
          return Token.LITERAL2;
        }
		}
		return 0;
	}

	@Override
	public void markTokensImpl(Token lastToken, Segment line, int lineIndex)
	{
		byte token = Token.NULL;
		this.literalChar = (lastToken == null ? 0 : lastToken.getPendingLiteralChar());

		if (literalChar != 0)
		{
			token = getLiteralId(literalChar);
		}

		// Check for multi line comments.
		if (lastToken != null && lastToken.id == Token.COMMENT1)
		{
			token = Token.COMMENT1;
		}

		char[] array = line.array;
		offset = lastOffset = lastKeyword = line.offset;
		int currentLength = line.count + offset;

		loop:
		for (int i = offset; i < currentLength; i++)
		{
			int i1 = i + 1;
			switch (array[i])
			{
				case '*':
					if (token == Token.COMMENT1 && currentLength - i >= 1 && i1 < array.length && array[i1] == '/')
					{
						token = Token.NULL;
						i++;
						addToken(lineIndex, (i + 1) - lastOffset, Token.COMMENT1);

						// if the comment ends at the end of the current line
						// add a NULL token in order to mark the end of the block comment
						// if there is at least another character on the line, that will
						// "reset" the comment token.
						if (i + 1 >= currentLength)
						{
							Token last = getLastTokenInLine(lineIndex);
							Token dummy = new Token(0, Token.NULL);
							last.next = dummy;
						}
						lastOffset = i + 1;
					}
					else if (token == Token.NULL)
					{
						searchBack(lineIndex, line, i, true);
						addToken(lineIndex, 1, Token.OPERATOR);
						lastOffset = i + 1;
					}
					break;
				case '[':
					if (isMicrosoft && token == Token.NULL)
					{
						searchBack(lineIndex, line, i, true);
						token = Token.LITERAL2;
						literalChar = '[';
						lastOffset = i;
					}
					break;
				case ']':
					if (isMicrosoft && token == Token.LITERAL2 && literalChar == '[')
					{
						token = Token.NULL;
						literalChar = 0;
						addToken(lineIndex, i1 - lastOffset, Token.LITERAL2);
						lastOffset = i + 1;
					}
					break;
				case '.':
				case ',':
				case '(':
				case ')':
					if (token == Token.NULL)
					{
						searchBack(lineIndex, line, i, true);
						addToken(lineIndex, 1, Token.NULL);
						lastOffset = i + 1;
					}
					break;
				case '+':
				case '%':
				case '&':
				case '|':
				case '^':
				case '~':
				case '<':
				case '>':
				case '=':
					if (token == Token.NULL)
					{
						searchBack(lineIndex, line, i, true);
						addToken(lineIndex, 1, Token.OPERATOR);
						lastOffset = i + 1;
					}
					break;
				case ' ':
				case '\t':
				case ';':
					if (token == Token.NULL)
					{
						searchBack(lineIndex, line, i, false);
					}
					break;
				case '/':
					if (token == Token.NULL)
					{
						if (currentLength - i >= 2 && i1 < array.length && array[i1] == '*')
						{
							searchBack(lineIndex, line, i, true);
							token = Token.COMMENT1;
							lastOffset = i;
							i++;
						}
						else
						{
							searchBack(lineIndex, line, i, true);
							addToken(lineIndex, 1, Token.OPERATOR);
							lastOffset = i + 1;
						}
					}
					break;
				case '-':
					if (token == Token.NULL)
					{
						if (currentLength - i >= 2 && i1 < array.length && array[i1] == '-')
						{
							searchBack(lineIndex, line, i, true);
							addToken(lineIndex, currentLength - i, Token.COMMENT2);
							lastOffset = currentLength;
							break loop;
						}
						else
						{
							searchBack(lineIndex, line, i, true);
							addToken(lineIndex, 1, Token.OPERATOR);
							lastOffset = i + 1;
						}
					}
					break;
				case '#':
					if (isMySql && token == Token.NULL)
					{
						if (currentLength - i >= 1)
						{
							searchBack(lineIndex, line, i, true);
							addToken(lineIndex, currentLength - i, Token.COMMENT2);
							lastOffset = currentLength;
							break loop;
						}
					}
					break;
				case '"':
				case '\'':
					if (token == Token.NULL)
					{
						literalChar = array[i];
						token = getLiteralId(literalChar);
						addToken(lineIndex, i - lastOffset, Token.NULL);
						lastOffset = i;
					}
					else if (Token.isLiteral(token) && literalChar == array[i])
					{
						token = Token.NULL;
						addToken(lineIndex, (i + 1) - lastOffset, getLiteralId(literalChar));
						literalChar = 0;
						lastOffset = i + 1;
					}
					break;
				default:
					break;
			}
		}

		if (token == Token.NULL)
		{
			searchBack(lineIndex, line, currentLength, false);
		}

		if (lastOffset != currentLength)
		{
			addToken(lineIndex, currentLength - lastOffset, token);
		}
	}

	private void searchBack(int lineIndex, Segment line, int pos, boolean padNull)
	{
		int len = pos - lastKeyword;
		byte id = keywords.lookup(line, lastKeyword, len);

		if (id != Token.NULL)
		{
			if (lastKeyword != lastOffset)
			{
				addToken(lineIndex, lastKeyword - lastOffset, Token.NULL);
			}
			addToken(lineIndex, len, id);
			lastOffset = pos;
		}

		lastKeyword = pos + 1;
		if (padNull && lastOffset < pos)
		{
			addToken(lineIndex, pos - lastOffset, Token.NULL);
		}
	}
}
