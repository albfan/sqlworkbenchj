/*
 * Token.java - Generic token
 * Copyright (C) 1998, 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */
package workbench.gui.editor;

/**
 * A linked list of tokens. Each token has three fields - a token
 * identifier, which is a byte value that can be looked up in the
 * array returned by <code>SyntaxDocument.getColors()</code>
 * to get a color value, a length value which is the length of the
 * token in the text, and a pointer to the next token in the list.
 *
 * @author Slava Pestov
 */
public class Token
{
	/**
	 * Normal text token id. This should be used to mark
	 * normal text.
	 */
	public static final byte NULL = 0;

	/**
	 * Comment 1 token id. This can be used to mark a comment.
	 */
	public static final byte COMMENT1 = 1;

	/**
	 * Comment 2 token id. This can be used to mark a comment.
	 */
	public static final byte COMMENT2 = 2;


	/**
	 * Literal 1 token id. This can be used to mark a string
	 * literal (eg, C mode uses this to mark "..." literals)
	 */
	public static final byte LITERAL1 = 3;

	/**
	 * Literal 2 token id. This can be used to mark an object
	 * literal (eg, Java mode uses this to mark true, false, etc)
	 */
	public static final byte LITERAL2 = 4;

	/**
	 * Label token id. This can be used to mark labels
	 * (eg, C mode uses this to mark ...: sequences)
	 */
	public static final byte DATATYPE = 5;

	/**
	 * Keyword 1 token id. This can be used to mark a
	 * keyword. This should be used for general language
	 * constructs.
	 */
	public static final byte KEYWORD1 = 6;

	/**
	 * Keyword 2 token id. This can be used to mark a
	 * keyword. This should be used for preprocessor
	 * commands, or variables.
	 */
	public static final byte KEYWORD2 = 7;

	/**
	 * Keyword 3 token id. This can be used to mark a
	 * keyword. This should be used for data types.
	 */
	public static final byte KEYWORD3 = 8;

	/**
	 * Operator token id. This can be used to mark an
	 * operator. (eg, SQL mode marks +, -, etc with this
	 * token type)
	 */
	public static final byte OPERATOR = 9;

	/**
	 * Invalid token id. This can be used to mark invalid
	 * or incomplete tokens, so the user can easily spot
	 * syntax errors.
	 */
	public static final byte INVALID = 10;

	/**
	 * The total number of defined token ids.
	 */
	public static final byte ID_COUNT = 11;

	/**
	 * The first id that can be used for internal state
	 * in a token marker.
	 */
	public static final byte INTERNAL_FIRST = 100;

	/**
	 * The last id that can be used for internal state
	 * in a token marker.
	 */
	public static final byte INTERNAL_LAST = 126;

	private char pendingChar = 0;

	/**
	 * The length of this token.
	 */
	public int length;

	/**
	 * The id of this token.
	 */
	public byte id;

	/**
	 * The next token in the linked list.
	 */
	public Token next;

	/**
	 * Creates a new token.
	 * @param length The length of the token
	 * @param id The id of the token
	 */
	public Token(int length, byte id)
	{
		this.length = length;
		this.id = id;
	}

	public boolean isLiteral()
	{
		return isLiteral(this.id);
	}

	public boolean isMultiline()
	{
		return this.pendingChar != 0;
	}

	public void setPendingLiteralChar(char c)
	{
		this.pendingChar = c;
	}

	public char getPendingLiteralChar()
	{
		return this.pendingChar;
	}

	public static boolean isLiteral(int tokenId)
	{
		return (tokenId == LITERAL1 || tokenId == LITERAL2);
	}

	public String typeString()
	{
		return typeString(this.id);
	}

	public static String typeString(int id)
	{
		String type = "NULL";
		switch (id)
		{
			case COMMENT1:
				type = "C1";
				break;
			case COMMENT2:
				type = "C2";
				break;
			case LITERAL1:
				type = "L1";
				break;
			case LITERAL2:
				type = "L2";
				break;
			case KEYWORD1:
				type = "K1";
				break;
			case KEYWORD2:
				type = "K2";
				break;
			case KEYWORD3:
				type = "K3";
				break;
			case OPERATOR:
				type = "OP";
				break;
			case NULL:
				type = "NL";
				break;
			default:
				type = "OT";
				break;
		}
		return type;
	}

	/**
	 * Returns a string representation of this token.
	 */
	@Override
	public String toString()
	{
		return "[type=" + typeString() + ",length=" + length + "]";
	}
}
