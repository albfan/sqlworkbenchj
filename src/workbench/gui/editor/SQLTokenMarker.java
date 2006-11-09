package workbench.gui.editor;

/*
 * SQLTokenMarker.java - Generic SQL token marker
 * Copyright (C) 1999 mike dillon
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

import javax.swing.text.Segment;

/**
 * SQL token marker.
 *
 * @author mike dillon
 * @version $Id: SQLTokenMarker.java,v 1.10 2006-11-09 23:05:25 thomas Exp $
 */
public class SQLTokenMarker 
	extends TokenMarker
{
	private int offset, lastOffset, lastKeyword;//, length;
	protected boolean isMySql = false;
	protected KeywordMap keywords;
	private char literalChar = 0;

	public SQLTokenMarker()
	{
	}

	public byte markTokensImpl(byte token, Segment line, int lineIndex)
	{
		char[] array = line.array;
		offset = lastOffset = lastKeyword = line.offset;
		int currentLength = line.count + offset;
loop:
		for(int i = offset; i < currentLength; i++)
		{
			int i1 = i+1;
			switch(array[i])
			{
			case '*':
				if(token == Token.COMMENT1 && currentLength - i >= 1 && array[i1] == '/')
				{
					token = Token.NULL;
					i++;
					addToken((i + 1) - lastOffset,Token.COMMENT1);
					lastOffset = i + 1;
				}
				else if (token == Token.NULL)
				{
					searchBack(line, i);
					addToken(1,Token.OPERATOR);
					lastOffset = i + 1;
				}
				break;
			case '[':
				if(token == Token.NULL)
				{
					searchBack(line, i);
					token = Token.LITERAL1;
					literalChar = '[';
					lastOffset = i;
				}
				break;
			case ']':
				if(token == Token.LITERAL1 && literalChar == '[')
				{
					token = Token.NULL;
					literalChar = 0;
					addToken(i1 - lastOffset,Token.LITERAL1);
					lastOffset = i + 1;
				}
				break;
			case '.': case ',': case '(': case ')':
				if (token == Token.NULL) {
					searchBack(line, i);
					addToken(1, Token.NULL);
					lastOffset = i + 1;
				}
				break;
			case '+': case '%': case '&': case '|': case '^':
			case '~': case '<': case '>': case '=':
				if (token == Token.NULL) {
					searchBack(line, i);
					addToken(1,Token.OPERATOR);
					lastOffset = i + 1;
				}
				break;
			case ' ': case '\t': case ';':
				if (token == Token.NULL) {
					searchBack(line, i, false);
				}
				break;
			case '/':
				if(token == Token.NULL)
				{
					if (currentLength - i >= 2 && array[i1] == '*')
					{
						searchBack(line, i);
						token = Token.COMMENT1;
						lastOffset = i;
						i++;
					}
					else
					{
						searchBack(line, i);
						addToken(1,Token.OPERATOR);
						lastOffset = i + 1;
					}
				}
				break;
			case '-':
				if(token == Token.NULL)
				{
					if (currentLength - i >= 2 && array[i1] == '-')
					{
						searchBack(line, i);
						addToken(currentLength - i,Token.COMMENT2);
						lastOffset = currentLength;
						break loop;
					}
					else
					{
						searchBack(line, i);
						addToken(1,Token.OPERATOR);
						lastOffset = i + 1;
					}
				}
				break;
			case '#':
				if (isMySql && token == Token.NULL)
				{
					if (currentLength - i >= 1)
					{
						searchBack(line, i);
						addToken(currentLength - i, Token.COMMENT2);
						lastOffset = currentLength;
						break loop;
					}
				}
				break;
			case '"': 
			case '\'':
				if(token == Token.NULL)
				{
					token = Token.LITERAL1;
					literalChar = array[i];
					addToken(i - lastOffset,Token.NULL);
					lastOffset = i;
				}
				else if(token == Token.LITERAL1 && literalChar == array[i])
				{
					token = Token.NULL;
					literalChar = 0;
					addToken((i + 1) - lastOffset,Token.LITERAL1);
					lastOffset = i + 1;
				}
				break;
			default:
				break;
			}
		}
		if(token == Token.NULL)
			searchBack(line, currentLength, false);
		if(lastOffset != currentLength)
			addToken(currentLength - lastOffset,token);
		return token;
	}

	private void searchBack(Segment line, int pos)
	{
		searchBack(line, pos, true);
	}

	private void searchBack(Segment line, int pos, boolean padNull)
	{
		int len = pos - lastKeyword;
		byte id = keywords.lookup(line,lastKeyword,len);
		if(id != Token.NULL)
		{
			if(lastKeyword != lastOffset)
				addToken(lastKeyword - lastOffset,Token.NULL);
			addToken(len,id);
			lastOffset = pos;
		}
		lastKeyword = pos + 1;
		if (padNull && lastOffset < pos)
			addToken(pos - lastOffset, Token.NULL);
	}
}