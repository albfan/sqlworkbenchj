/*
 * RenderUtils.java
 *
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
package workbench.print;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

import workbench.util.StringUtil;

/**
 * Globally available utility classes, mostly for string manipulation.
 *
 * @author Jim Menard, <a href="mailto:jimm@io.com">jimm@io.com</a>
 *
 * Taken from: http://www.java2s.com/Code/Java/2D-Graphics-GUI/WrapstringaccordingtoFontMetrics.htm
 */
public class RenderUtils
{
	/**
	 * Returns an array of strings, one for each line in the string after it has
	 * been wrapped to fit lines of <var>maxWidth</var>. Lines end with any of
	 * cr, lf, or cr lf. A line ending at the end of the string will not output a
	 * further, empty string.
	 * <p>
	 * This code assumes <var>str</var> is not
	 * <code>null</code>.
	 * <p/>
	 * @param str       the string to split
	 * @param fm        needed for string width calculations
	 * @param maxWidth  the max line width, in points
	 * <p/>
	 * @return a non-empty list of strings
	 */
	public static List<String> wrap(String str, FontMetrics fm, int maxWidth)
	{
		List<String> lines = StringUtil.getLines(str);
		if (lines.isEmpty())
		{
			return lines;
		}

		List<String> strings = new ArrayList<String>();
		for (String line : lines)
		{
			wrapLineInto(line, strings, fm, maxWidth);
		}
		return strings;
	}

	/**
	 * Given a line of text and font metrics information, wrap the line and add the new line(s) to <var>list</var>.
	 * <p/>
	 * @param line      a line of text
	 * @param list      an output list of strings
	 * @param fm        font metrics
	 * @param maxWidth  maximum width of the line(s)
	 */
	private static void wrapLineInto(String line, List<String> list, FontMetrics fm, int maxWidth)
	{
		int len = line.length();
		int width;

		while (len > 0 && (width = fm.stringWidth(line)) > maxWidth)
		{
			// Guess where to split the line. Look for the next space before
			// or after the guess.
			int guess = len * maxWidth / width;
			String before = line.substring(0, guess).trim();

			width = fm.stringWidth(before);
			int pos;
			if (width > maxWidth) // Too long
			{
				pos = findBreakBefore(line, guess);
			}
			else
			{ // Too short or possibly just right
				pos = findBreakAfter(line, guess);
				if (pos != -1)
				{ // Make sure this doesn't make us too long
					before = line.substring(0, pos).trim();
					if (fm.stringWidth(before) > maxWidth)
					{
						pos = findBreakBefore(line, guess);
					}
				}
			}
			if (pos == -1)
			{
				pos = guess; // Split in the middle of the word
			}
			list.add(line.substring(0, pos).trim());
			line = line.substring(pos).trim();
			len = line.length();
		}
		if (len > 0)
		{
			list.add(line);
		}
	}

	/**
	 * Returns the index of the first whitespace character or '-' in <var>line</var>
	 * that is at or before <var>start</var>.
	 *
	 * Returns -1 if no such character is found.
	 *
	 * <p/>
	 * @param line    a string
	 * @param start   where to star looking
	 */
	public static int findBreakBefore(String line, int start)
	{
		for (int i = start; i >= 0; --i)
		{
			char c = line.charAt(i);
			if (Character.isWhitespace(c) || c == '-')
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the first whitespace character or '-' in <var>line</var>
	 * that is at or after <var>start</var>.
	 *
	 * Returns -1 if no such character is found.
	 * <p/>
	 * @param line   a string
	 * @param start  where to star looking
	 */
	public static int findBreakAfter(String line, int start)
	{
		int len = line.length();
		for (int i = start; i < len; ++i)
		{
			char c = line.charAt(i);
			if (Character.isWhitespace(c) || c == '-')
			{
				return i;
			}
		}
		return -1;
	}

}
