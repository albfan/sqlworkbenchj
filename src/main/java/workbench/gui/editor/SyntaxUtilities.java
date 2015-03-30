package workbench.gui.editor;

/*
 * SyntaxUtilities.java - Utility functions used by syntax colorizing
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;

import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * Class with several utility functions used by jEdit's syntax colorizing
 * subsystem.
 *
 * @author Slava Pestov
 */
public class SyntaxUtilities
{

	public static int findMatch(Segment line, String needle, int startAt, boolean ignoreCase)
	{
		char[] haystack = line.array;
		int needleLen = needle.length();

		int searchPos = 0;
		int textLength = line.offset + line.count;
		if (textLength > haystack.length)
		{
			// for some reason it happens that the textLength is calculated too big
			LogMgr.logDebug("SyntaxUtilities.findMatch()", "textLength=" + textLength + ", line.offset=" + line.offset + ", line.count=" + line.count + ", haystack.length=" + haystack.length + ", needle=" + needle + " (length=" + needle.length() + ")");
			textLength = haystack.length;
		}

		for (int textPos = line.offset + startAt; textPos < textLength; textPos++)
		{
			char c1 = haystack[textPos];
			char c2 = needle.charAt(searchPos);

			if (ignoreCase)
			{
				c1 = Character.toUpperCase(c1);
				c2 = Character.toUpperCase(c2);
			}

			if (c1 == c2)
			{
				searchPos++;
				if (searchPos == needleLen)
				{
					return (textPos + 1) - needleLen - line.offset;
				}
			}
			else
			{
				textPos -= searchPos;
				searchPos = 0;
			}
		}
		return -1;
	}

	/**
	 * Checks if a subregion of a <code>Segment</code> is equal to a
	 * character array.
	 * @param ignoreCase True if case should be ignored, false otherwise
	 * @param text The segment
	 * @param offset The offset into the segment
	 * @param match The character array to match
	 */
	public static boolean regionMatches(boolean ignoreCase, Segment text, int offset, char[] match)
	{
		int length = offset + match.length;
		char[] textArray = text.array;
		if (length > text.offset + text.count)
		{
			return false;
		}
		for (int i = offset,  j = 0; i < length; i++, j++)
		{
			char c1 = textArray[i];
			char c2 = match[j];
			if (ignoreCase)
			{
				c1 = Character.toUpperCase(c1);
				c2 = Character.toUpperCase(c2);
			}
			if (c1 != c2)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the default style table. This can be passed to the
	 * <code>setStyles()</code> method of <code>SyntaxDocument</code>
	 * to use the default syntax styles.
	 */
	public static SyntaxStyle[] getDefaultSyntaxStyles()
	{
		SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

		Settings sett = Settings.getInstance();

		styles[Token.COMMENT1] = new SyntaxStyle(sett.getColor("workbench.editor.color.comment1", Color.GRAY),true,false);
		styles[Token.COMMENT2] = new SyntaxStyle(sett.getColor("workbench.editor.color.comment2", Color.GRAY),true,false);
		styles[Token.KEYWORD1] = new SyntaxStyle(sett.getColor("workbench.editor.color.keyword1", Color.BLUE),false,false);
		styles[Token.KEYWORD2] = new SyntaxStyle(sett.getColor("workbench.editor.color.keyword2", Color.MAGENTA),false,false);
		styles[Token.KEYWORD3] = new SyntaxStyle(sett.getColor("workbench.editor.color.keyword3", new Color(0x009600)),false,false);
		styles[Token.LITERAL1] = new SyntaxStyle(sett.getColor("workbench.editor.color.literal1", new Color(0x650099)),false,false);
		styles[Token.LITERAL2] = new SyntaxStyle(sett.getColor("workbench.editor.color.literal2", new Color(0x650099)),false,false);
		styles[Token.DATATYPE] = new SyntaxStyle(sett.getEditorDatatypeColor(),false,false);
		styles[Token.OPERATOR] = new SyntaxStyle(sett.getColor("workbench.editor.color.operator", Color.BLACK),false,false);
		styles[Token.INVALID] = new SyntaxStyle(sett.getColor("workbench.editor.color.invalid", Color.RED),false,true);

		return styles;
	}


	/**
	 * Paints the specified line onto the graphics context. Note that this
	 * method munges the offset and count values of the segment.
	 * @param line The line segment
	 * @param tokens The token list for the line
	 * @param styles The syntax style list
	 * @param expander The tab expander used to determine tab stops. May
	 * be null
	 * @param gfx The graphics context
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @param addwidth Additional spacing to be added to the line width
	 * @return The x co-ordinate, plus the width of the painted string
	 */
	public static int paintSyntaxLine(Segment line, Token tokens, SyntaxStyle[] styles, TabExpander expander, Graphics gfx, int x, int y, int addwidth)
	{
		if (tokens == null) return x;

		Font defaultFont = gfx.getFont();
		Color defaultColor = gfx.getColor();

		while (true)
		{
			if (tokens == null)
			{
				gfx.setColor(defaultColor);
				gfx.setFont(defaultFont);
				break;
			}

			if (tokens.id == Token.NULL)
			{
				gfx.setColor(defaultColor);
				gfx.setFont(defaultFont);
			}
			else
			{
				styles[tokens.id].setGraphicsFlags(gfx, defaultFont);
			}
			line.count = tokens.length;
			x = Utilities.drawTabbedText(line, x, y, gfx, expander, addwidth);
			line.offset += tokens.length;

			tokens = tokens.next;
		}

		return x;
	}

	// private members
	private SyntaxUtilities() {}
}
