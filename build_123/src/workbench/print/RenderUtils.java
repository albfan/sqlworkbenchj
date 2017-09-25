package workbench.print;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

import workbench.util.StringUtil;

/**
 * Utility class to wrap a string into lines for printing purposes.
 *
 * @author Jim Menard, <a href="mailto:jimm@io.com">jimm@io.com</a>
 * @author Thomas Kellerer (changes, enhancements)
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
	 * This code assumes <var>str</var> is not <code>null</code>.
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

		List<String> strings = new ArrayList<>(lines.size());
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
		int width = fm.stringWidth(line);

		while (len > 0 && width > maxWidth)
		{
			// Guess where to split the line.
			// Look for the next space before or after the guess.
			int guess = len * maxWidth / width;
			String before = line.substring(0, guess).trim();

			width = fm.stringWidth(before);
			int pos;
			if (width > maxWidth) // Too long
			{
				pos = findBreakBefore(line, guess);
			}
			else
			{
				// Too short or possibly just right
				pos = findBreakAfter(line, guess);
				if (pos != -1)
				{
					// Make sure this doesn't make us too long
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
			width = fm.stringWidth(line);
		}

		if (len > 0)
		{
			list.add(line);
		}
	}

	private static final String WORD_BOUNDARIES = ";,-=";

	/**
	 * Returns the index of the first whitespace or word boundary character in <var>line</var>
	 * that is at or before <var>start</var>.
	 *
	 * Returns -1 if no such character is found.
	 *
	 * <p/>
	 * @param line    a string
	 * @param start   where to start looking
	 * @see #WORD_BOUNDARIES
	 */
	private static int findBreakBefore(String line, int start)
	{
		for (int i = start; i >= 0; --i)
		{
			char c = line.charAt(i);
			if (Character.isWhitespace(c) || WORD_BOUNDARIES.indexOf(c) > -1)
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the first whitespace character or word boundary character  in <var>line</var>
	 * that is at or after <var>start</var>.
	 *
	 * Returns -1 if no such character is found.
	 * <p/>
	 * @param line   a string
	 * @param start  where to start looking
	 * @see #WORD_BOUNDARIES
	 */
	private static int findBreakAfter(String line, int start)
	{
		int len = line.length();
		for (int i = start; i < len; ++i)
		{
			char c = line.charAt(i);
			if (Character.isWhitespace(c) || WORD_BOUNDARIES.indexOf(c) > -1)
			{
				return i;
			}
		}
		return -1;
	}

}
