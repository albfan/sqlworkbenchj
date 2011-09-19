/*
 * StringUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of utility methods around String handling.
 *
 * @author Thomas Kellerer
 */
public class StringUtil
{
	public static final String REGEX_CRLF = "((\r\n)|(\n\r)|\r|\n)";
	public static final Pattern PATTERN_CRLF = Pattern.compile(REGEX_CRLF);
	public static final Pattern PATTERN_NON_LF = Pattern.compile("(\r\n)|(\n\r)|(\r)");

	public static final String LINE_TERMINATOR = System.getProperty("line.separator");
	public static final String EMPTY_STRING = "";

	public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
	public static final String ISO_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String ISO_TZ_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS z";

	public static final SimpleDateFormat ISO_TIMESTAMP_FORMATTER = new SimpleDateFormat(ISO_TIMESTAMP_FORMAT);
	public static final SimpleDateFormat ISO_TZ_TIMESTAMP_FORMATTER = new SimpleDateFormat(ISO_TZ_TIMESTAMP_FORMAT);

	public static StringBuilder emptyBuffer() { return new StringBuilder(0); }

	private static final char[] hexDigit = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

	public static String getCurrentTimestampWithTZString()
	{
		synchronized (ISO_TZ_TIMESTAMP_FORMATTER)
		{
			return ISO_TZ_TIMESTAMP_FORMATTER.format(now());
		}
	}

	/**
	 * Checks if the given string is a valid pattern for a SimpleDateFormat.
	 *
	 * @param pattern
	 * @return the exception's message if an error occurs, null if everything is OK.
	 */
	@SuppressWarnings("ResultOfObjectAllocationIgnored")
	public static String isDatePatternValid(String pattern)
	{
		if (isBlank(pattern)) return null;
		try
		{
			new SimpleDateFormat(pattern);
		}
		catch (Exception e)
		{
			return ExceptionUtil.getDisplay(e);
		}
		return null;
	}

	private static java.util.Date now()
	{
		return new java.util.Date(System.currentTimeMillis());
	}

	public static String getPathSeparator()
	{
		return System.getProperty("path.separator");
	}

	public static int hashCode(CharSequence val)
	{
		int len = val.length();
		int hash = 0;

		for (int i = 0; i < len; i++)
		{
			hash = 31*hash + val.charAt(i);
		}
		return hash;
	}

	/**
	 * Replace various combinations of linefeeds in the input string with \n.
	 *
	 * @param input
	 * @return input string with only \n linefeeds
	 * @see #PATTERN_NON_LF
	 */
	public static String makePlainLinefeed(String input)
	{
		Matcher m = PATTERN_NON_LF.matcher(input);
		return m.replaceAll("\n");
	}

	public static boolean endsWith(CharSequence s, String end)
	{
		if (s == null) return false;
		if (isEmptyString(end)) return false;
		int len = s.length();
		if (len == 0) return false;
		if (len < end.length()) return false;
		String last = s.subSequence(len - end.length(), len).toString();
		return end.equals(last);
	}

	public static boolean endsWith(CharSequence s, char c)
	{
		if (s == null) return false;
		int len = s.length();
		if (len == 0) return false;
		return s.charAt(len - 1) == c;
	}

	public static boolean lineStartsWith(CharSequence text, int lineStartPos, String compareTo)
	{
		if (isEmptyString(compareTo)) return false;
		if (isEmptyString(text)) return false;

		int textLength = text.length();
		int len = compareTo.length();

		// skip whitespace at the beginning
		int pos = findFirstNonWhitespace(text, lineStartPos);
		if (pos > lineStartPos)
		{
			lineStartPos = pos;
		}

		for (int i=0; i < len; i++)
		{
			char thisChar = 0;
			char otherChar = compareTo.charAt(i);
			if (lineStartPos + i < textLength)
			{
				thisChar = text.charAt(lineStartPos + i);
			}
			if (thisChar != otherChar) return false;
		}
		return true;
	}

	/**
	 * Returns the length of the line without any line ending characters
	 */
	public static int getRealLineLength(String line)
	{
		int len = line.length();
		if (len == 0) return 0;

		char c = line.charAt(len - 1);

		while ((len > 0) && (c == '\r' || c == '\n'))
		{
			len--;
			if (len > 0) c = line.charAt(len - 1);
		}
		return len;
	}

	public static boolean isWhitespace(CharSequence s)
	{
		if (s == null) return false;
		int len = s.length();
		if (len == 0) return false;
		int pos = 0;
		while (pos < len)
		{
			char c = s.charAt(pos);
			if (!Character.isWhitespace(c)) return false;
			pos++;
		}
		return true;
	}

	public static String rtrim(String s)
	{
		if (s == null) return s;
		int pos = s.length();
		if (pos == 0) return s;

		char last = s.charAt(pos - 1);
		if (!Character.isWhitespace(last)) return s;

		while (pos > 0 && Character.isWhitespace(s.charAt(pos - 1)))
		{
			pos --;
		}

		return s.substring(0, pos);
	}

	public static int indexOf(CharSequence value, char c)
	{
		return indexOf(value, c, 1);
	}

	public static int indexOf(CharSequence value, char c, int occurance)
	{
		if (value == null) return -1;
		if (occurance <= 0) occurance = 1;
		int numFound = 0;

		for (int i=0; i < value.length(); i++)
		{
			if (value.charAt(i) == c)
			{
				numFound++;
				if (numFound == occurance) return i;
			}
		}
		return -1;
	}

	public static void trimTrailingWhitespace(StringBuilder value)
	{
		if (value == null || value.length() == 0) return;
		int len = value.length();
		int pos = len - 1;
		char c = value.charAt(pos);
		if (!Character.isWhitespace(c)) return;

		while (Character.isWhitespace(c))
		{
			pos --;
			c = value.charAt(pos);
		}
		value.delete(pos + 1, len);
	}

	public static boolean isMixedCase(String s)
	{
		return !isUpperCase(s) && !isLowerCase(s);
	}

	public static boolean isLowerCase(String s)
	{
		if (s == null) return false;
		int l = s.length();
		for (int i = 0; i < l; i++)
		{
			char c = s.charAt(i);
			if (Character.isUpperCase(c)) return false;
		}
		return true;
	}

	public static boolean isUpperCase(String s)
	{
		if (s == null) return false;
		int l = s.length();
		for (int i = 0; i < l; i++)
		{
			char c = s.charAt(i);
			if (Character.isLowerCase(c)) return false;
		}
		return true;
	}

	public static boolean arraysEqual(String[] one, String[] other)
	{
		if (one == null && other != null) return false;
		if (one != null && other == null) return false;
		if (one.length != other.length) return false;
		for (int i = 0; i < one.length; i++)
		{
			if (!equalString(one[i],other[i])) return false;
		}
		return true;
	}

	public static boolean hasOpenQuotes(String data, char quoteChar)
	{
		if (isEmptyString(data)) return false;
		int chars = data.length();
		boolean inQuotes = false;
		for (int i = 0; i < chars; i++)
		{
			if (data.charAt(i) == quoteChar) inQuotes = !inQuotes;
		}
		return inQuotes;
	}

	/**
	 * Capitalize the first word of the passed String.
	 * (write the first character in uppercase, the rest in lower case)
	 * This does not loop through the entire string to capitalize every word.
	 */
	public static String capitalize(String word)
	{
		if (word == null) return null;
		if (word.length() == 0) return word;
		StringBuilder s = new StringBuilder(word.toLowerCase());
		char c = s.charAt(0);
		s.setCharAt(0, Character.toUpperCase(c));
		return s.toString();
	}

	/**
	 * Remove all characters  from the input string that might not be allowed in a filename.
	 * @param input the value to be used as a filename
	 * @return input value without any characters that might not be allowed for a filename converted to lowercase
	 */
	public static String makeFilename(String input)
	{
		if (input == null) return null;
		if (input.equals("..")) return "__";
		if (input.equals(".")) return "_";
		return input.replaceAll("[\t:\\\\/\\?\\*\\|<>\"'%\u00A7\\^&\u0000]", "").toLowerCase();
	}

	/**
	 * Replacement for StringBuilder.lastIndexOf() which does
	 * a lot of object creation and copying to achieve this.
	 * 
	 * This implementation should be a lot faster for StringBuilder
	 * and StringBuffer, and will basically be the same for String
	 * objects.
	 *
	 * @param s the string to search in
	 * @param c the character to look for
	 * @return -1 if c was not found, the position of c in s otherwise
	 */
	public static int lastIndexOf(CharSequence s, char c)
	{
		int len = s.length();
		if (s == null || len == 0) return -1;

		for (int i=(len - 1); i > 0; i--)
		{
			if (s.charAt(i) == c) return i;
		}
		return -1;
	}

	/*
	 * Null safe string replacement.
	 *
	 * This is a wrapper around String.replace(String, String) but does not throw an
	 * exception if one of the parameters is null.
	 *
	 * @param haystack the string in which to replace. If null, null is returned
	 * @param needle the string to search for. If null, haystack is returned.
	 * @param replacement the replacement. If null haystack is returned
	 */
	public static String replace(String haystack, String needle, String replacement)
	{
		if (replacement == null) return haystack;
		if (needle == null) return haystack;
		if (haystack == null) return null;

		return haystack.replace(needle, replacement);
	}

	/**
	 * Replace all occurances of a search string in the passed StringBuilder.
	 *
	 * This is faster than using String.replace() as that uses regular expressions internally
	 *
	 * @param haystack
	 * @param needle
	 * @param replacement
	 * @return
	 */
	public static StringBuilder replaceBuffer(StringBuilder haystack, String needle, String replacement)
	{

		int pos = haystack.indexOf(needle);
		if (pos == -1)
		{
			return haystack;
		}

		StringBuilder result = new StringBuilder(haystack.length() + 10);

		int lastpos = 0;
		int len = needle.length();
		while (pos != -1)
		{
			result.append(haystack.substring(lastpos, pos));
			result.append(replacement);
			lastpos = pos + len;
			pos = haystack.indexOf(needle, lastpos);
		}
		if (lastpos < haystack.length())
		{
			result.append(haystack.substring(lastpos));
		}
		return result;
	}

	private static final int[] limits =
	{
		9,99,999,9999,99999,999999,9999999,99999999,999999999,Integer.MAX_VALUE
	};

	/**
	 * Returns the number of Digits of the value
	 *
	 * @param x the value to check
	 * @return the number of digits that x consists of
	 */
	public static int numDigits(int x )
	{
		for (int i = 0; i < limits.length; i++)
		{
			if ( x <= limits[i])
			{
				return i+1;
			}
		}
		return limits.length + 1;
	}

	/**
	 * Find the longest line in the give string and return its length.
	 * Up to maxLines lines are evaluated.
	 *
	 * @param text
	 * @param maxLines
	 * @return the length of the longest line
	 */
	public static String getLongestLine(String text, int maxLines)
	{
		if (isEmptyString(text)) return "";
		Matcher m = PATTERN_CRLF.matcher(text);

		int lastpos = 0;
		int linecount = 0;
		int maxlen = 0;
		int linestart = 0;
		int lineend = 0;

		while (m.find(lastpos))
		{
			linecount ++;
			int pos = m.start();
			int len = pos - lastpos;
			if (len > maxlen)
			{
				linestart = lastpos;
				lineend = pos;
				maxlen = len;
			}
			lastpos = pos + 1;
			if (linecount >= maxLines) break;
		}

		if (m.hitEnd())
		{
			int len = text.length() - lastpos;
			if (len > maxlen)
			{
				maxlen = len;
				linestart = lastpos;
				lineend = text.length();
			}
		}
		if (linestart > 0 && lineend <= text.length())
		{
			return text.substring(linestart, lineend);
		}
		return text;
	}

	/**
	 * Check if the given string is a number.
	 *
	 * This is done by parsing the String using Double.parseDouble() and catching all possible exceptions.
	 *
	 * @param value
	 * @return
	 */
	public static boolean isNumber(String value)
	{
		try
		{
			 Double.parseDouble(value);
			 return true;
		}
		catch (Throwable e)
		{
			return false;
		}
	}

	/**
	 * Checks if the given parameter is not empty and deos not only exist of whitespace
	 *
	 * @param value
	 * @return true if at least one non-whitespace character is returned
	 * @see #isBlank(java.lang.CharSequence)
	 */
	public static boolean isNonBlank(CharSequence value)
	{
		return !isBlank(value);
	}

	/**
	 * Checks if the given parameter is "empty",
	 * i.e: either null, length == 0 or contains only whitespace
	 */
	public static boolean isBlank(CharSequence value)
	{
		if (isEmptyString(value)) return true;
		return isWhitespace(value);
	}

	/**
	 * Checks if the given string value is not empty (!= null && length() > 0).
	 *
	 * Whitespaces are considered "not empty".
	 *
	 * @param value
	 * @see #isEmpty(CharSequence)
	 */
	public static boolean isNonEmpty(CharSequence value)
	{
		return !isEmptyString(value);
	}

	/**
	 * Checks if the given String is null or has a zero length.
	 * A String containing only whitespaces is not considered empty.
	 *
	 * @param value the String to test
	 * @return true if the String is empty (or null)
	 */
	public static boolean isEmptyString(CharSequence value)
	{
		if (value == null) return true;
		if (value.length() == 0) return true;
		return false;
	}

	public static char getFirstNonWhitespace(CharSequence line)
	{
		int pos = findFirstNonWhitespace(line);
		if (pos > -1 && pos < line.length())
		{
			return line.charAt(pos);
		}
		return 0;
	}

	/**
	 * Return the position of the first non-whitespace character in the String
	 * @param line
	 * @return the position of the first whitespace or the length of the string
	 */
	public static int findFirstNonWhitespace(CharSequence line)
	{
		return findFirstNonWhitespace(line, 0);
	}

	/**
	 * Return the position of the first non-whitespace character in the String
	 * @param line
	 * @param startPos the position where to start searching
	 * @return the position of the first whitespace or the length of the string
	 */
	public static int findFirstNonWhitespace(final CharSequence line, int startPos)
	{
		if (line == null) return -1;
		int len = line.length();
		if (len == 0) return -1;
		if (startPos >= len) return -1;

		int pos = startPos;

		char c = line.charAt(pos);
		while (pos < len - 1)
		{
			if (c > ' ') return pos;
			pos ++;
			c = line.charAt(pos);
		}
		return -1;
	}

	public static String getStartingWhiteSpace(final String line)
	{
		if (line == null) return null;
		int pos = findFirstNonWhitespace(line);
		if (pos <= 0) return null;
		String result = line.substring(0, pos);
		return result;
	}

	/**
	 * Parse the given value as a double.
	 * If the value cannot be parsed, the default value will be returned.
	 *
	 * @param value  the string to parse
	 * @param defaultValue the value to be returned if something goes wrong.
	 * @return the parse value of the input.
	 */
	public static double getDoubleValue(String value, double defaultValue)
	{
		if (value == null) return defaultValue;

		double result = defaultValue;
		try
		{
			result = Double.parseDouble(value.trim());
		}
		catch (NumberFormatException e)
		{
			// Ignore
		}
		return result;
	}

	public static int getIntValue(String aValue)
	{
		return getIntValue(aValue, 0);
	}

	public static int getIntValue(String aValue, int aDefault)
	{
		if (aValue == null) return aDefault;

		int result = aDefault;
		try
		{
			result = Integer.parseInt(aValue.trim());
		}
		catch (NumberFormatException e)
		{
			// Ignore
		}
		return result;
	}

	public static long getLongValue(String aValue, long aDefault)
	{
		if (aValue == null) return aDefault;

		long result = aDefault;
		try
		{
			result = Long.parseLong(aValue.trim());
		}
		catch (NumberFormatException e)
		{
			// Ignore
		}
		return result;
	}

	/**
	 * Checks if both Strings are equal considering null values.
	 * A null String and an empty String (length==0 or all whitespace) are
	 * considered equal as well (because both are "empty")
	 * @see #isBlank(java.lang.CharSequence)
	 */
	public static boolean equalStringOrEmpty(String one, String other)
	{
		if (isBlank(one) && isBlank(other)) return true;
		return equalString(one, other);
	}

	/**
	 * Checks if both Strings are equal considering null values.
	 * A null String and an empty String (length==0 or all whitespace) are
	 * considered equal as well (because both are "empty")
	 * @see #isBlank(java.lang.CharSequence)
	 */
	public static boolean equalStringOrEmpty(String one, String other, boolean ignoreCase)
	{
		if (isBlank(one) && isBlank(other)) return true;
		return compareStrings(one, other, ignoreCase) == 0;
	}

	public static boolean equalString(String one, String other)
	{
		return compareStrings(one, other, false) == 0;
	}

	/**
	 * @param value1 the first String, maybe null
	 * @param value2 the second String, maybe null
	 * @return 0 if both are null
	 */
	public static int compareStrings(String value1, String value2, boolean ignoreCase)
	{
		if (value1 == null && value2 == null) return 0;
		if (value1 == null) return -1;
		if (value2 == null) return 1;
		if (ignoreCase) return value1.compareToIgnoreCase(value2);
		return value1.compareTo(value2);
	}

	public static boolean equalStringIgnoreCase(String one, String other)
	{
		if (one == null && other == null) return true;
		if (one == null || other == null) return false;
		return one.equalsIgnoreCase(other);
	}

	public static List<String> stringToList(String aString)
	{
		return stringToList(aString, ",");
	}

	public static List<String> stringToList(String aString, String aDelimiter)
	{
		return stringToList(aString, aDelimiter, false, false);
	}

	public static List<String> stringToList(String aString, String aDelimiter, boolean removeEmpty)
	{
		return stringToList(aString, aDelimiter, removeEmpty, false);
	}

	public static List<String> stringToList(String aString, String aDelimiter, boolean removeEmpty, boolean trimEntries)
	{
		return stringToList(aString, aDelimiter, removeEmpty, trimEntries, false, false);
	}

	public static List<String> stringToList(String aString, String aDelimiter, boolean removeEmpty, boolean trimEntries, boolean checkBrackets)
	{
		return stringToList(aString, aDelimiter, removeEmpty, trimEntries, checkBrackets, false);
	}

	/**
	 * Parses the given String and creates a List containing the elements
	 * of the string that are separated by <tt>aDelimiter</aa>
	 *
	 * @param aString the value to be parsed
	 * @param aDelimiter the delimiter to user
	 * @param removeEmpty flag to remove empty entries
	 * @param trimEntries flag to trim entries (will be applied beore checking for empty entries)
   * @param checkBrackets flag to check for opening and closing brackets (delimiter inside brackets will not be taken into account)
	 * @return A List of Strings
	 */
	public static List<String> stringToList(String aString, String aDelimiter, boolean removeEmpty, boolean trimEntries, boolean checkBrackets, boolean keepQuotes)
	{
		if (isEmptyString(aString)) return new ArrayList<String>(0);
		WbStringTokenizer tok = new WbStringTokenizer(aString, aDelimiter);
		tok.setDelimiterNeedsWhitspace(false);
		tok.setKeepQuotes(keepQuotes);
		tok.setCheckBrackets(checkBrackets);
		List<String> result = new LinkedList<String>();
		while (tok.hasMoreTokens())
		{
			String element = tok.nextToken();
			if (element == null) continue;
			if (trimEntries) element = element.trim();
			if (removeEmpty && isEmptyString(element)) continue;
			result.add(element);
		}
		return result;
	}

	public static String[] toArray(Collection<String> strings, boolean toUpper)
	{
		if (strings == null) return null;
		if (strings.isEmpty()) return new String[0];

		int i = 0;
		String[] result = new String[strings.size()];
		for (String s : strings)
		{
			result[i++] = (s == null ? null : (toUpper ? s.toUpperCase() : s ) );
		}
		return result;
	}

	/**
	 * Create a String from the given Collection, where the elements are delimited
	 * with the supplied delimiter
	 *
	 * @return The elements of the Collection as a String
	 * @param aList The list to process
	 * @param aDelimiter The delimiter to use
	 */
	public static String listToString(Collection aList, char aDelimiter)
	{
		return listToString(aList, String.valueOf(aDelimiter), false);
	}

	public static String listToString(Collection aList, char aDelimiter, boolean quoteEntries)
	{
		return listToString(aList, String.valueOf(aDelimiter), quoteEntries);
	}
	/**
	 * Create a String from the given list, where the elements are delimited
	 * with the supplied delimiter
	 *
	 * @return The elements of the Collection as a String
	 * @param aList The list to process
	 * @param aDelimiter The delimiter to use
	 * @param quoteEntries if true, all entries are quoted with a double quote
	 */
	public static String listToString(Collection aList, String aDelimiter, boolean quoteEntries)
	{
		return listToString(aList, aDelimiter, quoteEntries, '"');
	}

	public static String listToString(Collection aList, String aDelimiter, boolean quoteEntries, char quote)
	{
		if (aList == null || aList.isEmpty()) return "";
		int numElements = 0;
		StringBuilder result = new StringBuilder(aList.size() * 50);
		for (Object o : aList)
		{
			if (o == null) continue;
			if (numElements > 0)
			{
				result.append(aDelimiter);
			}
			if (quoteEntries) result.append(quote);
			result.append(o.toString());
			if (quoteEntries) result.append(quote);
			numElements ++;
		}
		return result.toString();
	}

	/**
	 * Removes the given quote character from the input string, but only if the
	 * input string starts with that quote character
	 *
	 * @param input the string to "trim"
	 * @param quote the quote character to be used
	 * @return the input string with the quote character removed at the start and end
	 */
	public static String removeQuotes(String input, String quote)
	{
		if (isEmptyString(input)) return input;
		input = input.trim();
		if (input.equals(quote)) return input;
		if (!(input.startsWith(quote) && input.endsWith(quote))) return input;
		return input.substring(quote.length(), input.length() - quote.length());
	}

	/**
	 * Removes single or double quote character from the start and the beginning of a string.
	 * <br/>
	 * Removes the matching quote character at the beginning from the end of the string.
	 * The string is trimmed before testing for the presence of the quotes.
	 *
	 * @param input the string from which the quotes should be removed
	 * @return the input with quotes removed
	 */
	public static String trimQuotes(String input)
	{
		if (isBlank(input)) return input;

		String result = input.trim();
		int len = result.length();

		char firstChar = result.charAt(0);
		char lastChar = result.charAt(len - 1);

		if ( (firstChar == '"' && lastChar == '"') ||
		     (firstChar == '\'' && lastChar == '\'') ||
				 (firstChar == '`' && lastChar == '`') /* workaround the idiotic MySQL quoting */
				 )
		{
			return result.substring(1, len - 1);
		}
		return input;
	}

	public static boolean stringToBool(String aString)
	{
		if (aString == null) return false;
		return ("true".equalsIgnoreCase(aString) || "1".equals(aString) || "y".equalsIgnoreCase(aString) || "yes".equalsIgnoreCase(aString) || "on".equalsIgnoreCase(aString) );
	}

	public static String getMaxSubstring(String s, int maxLen, String add)
	{
		if (maxLen < 1) return s;
		if (s == null) return "";
		if (s.length() < maxLen) return s;
		if (add == null)
		{
			return s.substring(0, maxLen);
		}
		else
		{
			return s.substring(0, maxLen) + add;
		}
	}

	public static String getMaxSubstring(String s, int maxLen)
	{
		return getMaxSubstring(s, maxLen, "...");
	}

	public static final String REGEX_SPECIAL_CHARS = "\\[](){}.*+?$^|";

	/**
	 * 	Quote the characters in a String that have a special meaning
	 *  in regular expression.
	 */
	public static String quoteRegexMeta(String str)
	{
		if (str == null) return null;
		if (str.length() == 0)
		{
			return "";
		}
		int len = str.length();
		StringBuilder buf = new StringBuilder(len + 5);
		for (int i = 0; i < len; i++)
		{
			char c = str.charAt(i);
			if (REGEX_SPECIAL_CHARS.indexOf(c) != -1)
			{
				buf.append('\\');
			}
			buf.append(c);
		}
		return buf.toString();
	}

	public static int findPreviousWhitespace(String data, int pos)
	{
		if (data == null) return -1;
		int count = data.length();
		if (pos > count || pos <= 1) return -1;
		for (int i=pos; i > 0; i--)
		{
			if (Character.isWhitespace(data.charAt(i))) return i;
		}
		return -1;
	}

	public static int findWordBoundary(String data, int pos, String wordBoundaries)
	{
		if (wordBoundaries == null) return findPreviousWhitespace(data, pos);
		if (data == null) return -1;
		int count = data.length();
		if (pos > count) return -1;
		if (pos <= 1) return 0;
		for (int i=pos; i > 0; i--)
		{
			char c = data.charAt(i);
			if (wordBoundaries.indexOf(c) > -1 || Character.isWhitespace(c)) return i;
		}
		return 0;
	}

	/**
	 * Find the first non-quoted whitespace in the given String.
	 *
	 * @param data the data in which to search
	 * @return the position of the first whitespace or -1 if no whitespace was found.
	 */
	public static int findFirstWhiteSpace(CharSequence data)
	{
		if (data == null) return -1;
		int count = data.length();
		boolean inQuotes = false;
		for (int i=0; i < count; i++)
		{
			char c = data.charAt(i);
			if (c == '"')
			{
				inQuotes = !inQuotes;
			}
			if (!inQuotes)
			{
				if (Character.isWhitespace(data.charAt(i))) return i;
			}
		}
		return -1;
	}

	public static String findWordLeftOfCursor(String text, int pos)
	{
		if (pos < 0) return null;

		// skip whitespace until a character is found
		try
		{
			int index = pos;
			char c = text.charAt(index);
			while (index > 0 && Character.isWhitespace(c))
			{
				index --;
				c = text.charAt(index);
			}
			if (index > 0)
			{
				return getWordLeftOfCursor(text, index + 1, null);
			}
			return null;
		}
		catch (Exception e)
		{
			return null;
		}
	}


	public static String getWordLeftOfCursor(String text, int pos, String wordBoundaries)
	{
		try
		{
			if (pos < 0) return null;
			int len = text.length();
			int testPos = -1;
			if (pos >= len)
			{
				testPos = len - 1;
			}
			else
			{
				testPos = pos - 1;
			}
			if (testPos < 1) return null;

			if (Character.isWhitespace(text.charAt(testPos))) return null;

			String word = null;
			int startOfWord = findWordBoundary(text, testPos, wordBoundaries);
			if (startOfWord == 0)
			{
				word = text.substring(0, Math.min(pos,len));
			}
			else if (startOfWord > 0)
			{
				word = text.substring(startOfWord+1, Math.min(pos,len));
			}
			return word;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static int findPattern(String regex, String data, int startAt)
	{
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		return findPattern(p, data, startAt);
	}

	public static int findPattern(Pattern p, String data, int startAt)
	{
		if (startAt < 0) return -1;
		Matcher m = p.matcher(data);
		int result = -1;
		if (m.find(startAt)) result = m.start();
		return result;
	}

	public static String decodeUnicode(String theString)
	{
		if (theString == null) return null;

		char aChar;
		int len = theString.length();
		if (len == 0) return theString;
		StringBuilder outBuffer = new StringBuilder(len);

		for (int x=0; x < len ; )
		{
			aChar = theString.charAt(x++);
			if (aChar == '\\' && x < len)
			{
				aChar = theString.charAt(x++);

				if (aChar == 'u')
				{
					// Read the xxxx
					int value = -1;
					int i=0;
					for (i=0; i<4; i++)
					{
						if ( x + i >= len)
						{
							value = -1;
							break;
						}

						aChar = theString.charAt(x + i);
						switch (aChar)
						{
							case '0': case '1': case '2': case '3': case '4':
							case '5': case '6': case '7': case '8': case '9':
								value = (value << 4) + aChar - '0';
								break;
							case 'a': case 'b': case 'c':
							case 'd': case 'e': case 'f':
								value = (value << 4) + 10 + aChar - 'a';
								break;
							case 'A': case 'B': case 'C':
							case 'D': case 'E': case 'F':
								value = (value << 4) + 10 + aChar - 'A';
								break;
							default:
								// Invalid ecape sequence
								value = -1;
								break;
						}
						if (value == -1) break;
					}

					if ( value != -1)
					{
						outBuffer.append((char)value);
					}
					else
					{
						// Invalid encoded unicode character
						// do not convert the stuff, but copy the
						// characters into the result buffer
						outBuffer.append("\\u");
						if (i == 0 && x < len)
						{
							outBuffer.append(aChar);
						}
						else
						{
							for (int k=0; k < i; k++) outBuffer.append(theString.charAt(x+k));
						}
						x++;
					}
					x += i;
				}
				else
				{
					// The character after the backslash was not a 'u'
					// so we are not dealing with a uXXXX value
					// This applies popular "encodings" for non-printable characters
					if (aChar == '\\') aChar = '\\';
					else if (aChar == 't') aChar = '\t';
					else if (aChar == 'r') aChar = '\r';
					else if (aChar == 'n') aChar = '\n';
					else if (aChar == 'f') aChar = '\f';
					else outBuffer.append('\\');
					outBuffer.append(aChar);
				}
			}
			else
			{
				outBuffer.append(aChar);
			}

		}
		return outBuffer.toString();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public static void dump(String value)
	{
		int size = value.length();
		for (int i = 0; i < size; i++)
		{
			int c = value.charAt(i);
			String s = Integer.toHexString(c);
			if (s.length() == 1) System.out.print("0");
			System.out.print(s);
			System.out.print(" ");
		}
		System.out.println("");
	}

	public static String escapeUnicode(String value, CharacterRange range)
	{
		return escapeUnicode(value, range, null);
	}

	/**
	 * Encodes characters to Unicode &#92;uxxxx (or simple escape like \r)
	 *
	 * This has partially been "borrowed" from the Properties class, because the code
	 * there is not usable from the outside.
	 *
	 * Backslash, CR, LF, Tab and FormFeed (\f) will always be replaced.
	 *
	 * @param value the string to be encoded
	 * @param range the CharacterRange which defines the characters to be encoded. If isOutsideRange()
	 *        returns true, the character will be encoded.
	 * @param additionalCharsToEncode additional characters not covered by the range may be null
	 */
	public static String escapeUnicode(String value, CharacterRange range, String additionalCharsToEncode)
	{
		return escapeText(value, 'u', range, additionalCharsToEncode);
	}

	public static String escapeText(String value, char hexChar, CharacterRange range, String additionalCharsToEncode)
	{
		if (value == null) return null;

		int len = value.length();
		StringBuilder outBuffer = new StringBuilder((int)(len*1.5));

		for (int x = 0; x < len; x++)
		{
			char aChar = value.charAt(x);

			switch (aChar)
			{
				case '\\':
					outBuffer.append("\\\\");
					break;
				case '\t':
					outBuffer.append("\\t");
					break;
				case '\n':
					outBuffer.append("\\n");
					break;
				case '\r':
					outBuffer.append("\\r");
					break;
				case '\f':
					outBuffer.append("\\f");
					break;
				default:
					if ((range != null && range.isOutsideRange(aChar)) ||
						(additionalCharsToEncode != null && additionalCharsToEncode.indexOf(aChar) > -1))
					{
						outBuffer.append('\\');
						outBuffer.append(hexChar);
						appendUnicode(outBuffer, aChar);
					}
					else
					{
						outBuffer.append(aChar);
					}
			}
		}
		return outBuffer.toString();
	}

	public static CharSequence getOctalString(int input)
	{
		StringBuilder result = new StringBuilder(3);
		String s = Integer.toOctalString(input);
		int len = s.length();
		if (len == 1)
		{
			result.append("00");
		}
		else if (len == 2)
		{
			result.append('0');
		}
		result.append(s);
		return result;
	}

	public static String padRight(String input, int length)
	{
		return padRight(input, length, ' ');
	}

	public static String padRight(String input, int length, char padChar)
	{
		if (input == null) return null;
		if (input.length() >= length) return input;
		StringBuilder result = new StringBuilder(length);
		result.append(input);
		while (result.length() < length)
		{
			result.append(padChar);
		}
		return result.toString();
	}

	/**
	 * Convert an int value to a String with leading zeros
	 * @param value
	 * @param length
	 * @return
	 */
	public static CharSequence formatInt(int value, int length)
	{
		StringBuilder result = new StringBuilder(length);
		String nr = Integer.toString(value);
		for (int i=0; i < length - nr.length(); i++)
		{
			result.append('0');
		}
		result.append(nr);
		return result;
	}

	public static String formatNumber(int value, int length, boolean fillRight)
	{
		String s = NumberStringCache.getNumberString(value);
		int l = s.length();
		if (l >= length) return s;
		StringBuilder result = new StringBuilder(length);
		if (fillRight)
		{
			result.append(s);
		}
		for (int k = l; k < length; k++)
		{
			result.append(' ');
		}
		if (!fillRight)
		{
			result.append(s);
		}
		return result.toString();
	}

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{[0-9a-zA-Z_\\.\\-]*\\}");

	/**
	 * Replaces "variables" of the form ${some.thing} in the input string.
	 * Any variable name is assumed to be a system property, this means
	 * <tt>${user.home}</tt> will be replaced with the value of System.getProperty("user.home")
	 *
	 * @param input a string that might contain system property variables
	 * @return the string with replaced system properties or <tt>null</tt> if the input was <tt>null</tt>
	 */
	public static String replaceProperties(String input)
	{
		return replaceProperties(System.getProperties(), input);
	}

	public static String replaceProperties(Properties props, String input)
	{
		if (isEmptyString(input)) return input;

		Matcher m = VARIABLE_PATTERN.matcher(input);
		if (m == null)
		{
			return input;
		}

		while (m.find())
		{
			final int start = m.start();
			final int end = m.end();
			final String var = input.substring(start, end);
			final String propName = input.substring(start + 2, end - 1);
			final String propValue = props.getProperty(propName, null);
			if (propValue != null)
			{
				input = input.replace(var, propValue);
				m = VARIABLE_PATTERN.matcher(input);
				if (m == null)
				{
					return input;
				}
			}
		}
		return input;
	}

	public static void appendUnicode(StringBuilder buffer, char c)
	{
		buffer.append(hexDigit(c >> 12));
		buffer.append(hexDigit(c >>  8));
		buffer.append(hexDigit(c >>  4));
		buffer.append(hexDigit(c));
	}

	public static String hexString(int toConvert, int length)
	{
		String hex = Integer.toHexString(toConvert);
		StringBuilder result = new StringBuilder(length);
		for (int i= 0; i < length - hex.length(); i++)
		{
			result.append('0');
		}
		result.append(hex);
		return result.toString();
	}

	public static char hexDigit(int nibble)
	{
		return hexDigit[(nibble & 0xF)];
	}

	public static boolean containsWords(CharSequence toSearch, List<String> searchValues, boolean matchAll, boolean ignoreCase)
	{
		return containsWords(toSearch, searchValues, matchAll, ignoreCase, false);
	}

	/**
	 * Searches for multiples words inside a string.
	 *
	 * @param toSearch the string in which to search
	 * @param searchValues the pattern(s) to search for
	 * @param matchAll if true all patterns must be found
	 *
	 * @return true if searchValues were found
	 */
	public static boolean containsWords(CharSequence toSearch, List<String> searchValues, boolean matchAll, boolean ignoreCase, boolean useRegex)
	{
		if (StringUtil.isBlank(toSearch)) return false;
		if (CollectionUtil.isEmpty(searchValues)) return false;

		for (String search : searchValues)
		{
			String expression = null;
			if (useRegex)
			{
				expression = search;
			}
			else
			{
				expression = "(" + StringUtil.quoteRegexMeta(search) + ")";
			}
			int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
			if (useRegex)
			{
				flags += Pattern.MULTILINE;
			}

			Pattern p = Pattern.compile(expression, flags);
			Matcher m = p.matcher(toSearch);

			boolean found = m.find();

			if (!matchAll && found) return true;
			if (matchAll && !found) return false;
		}
		return matchAll;
	}

	public static String quoteIfNeeded(String input)
	{
		if (isBlank(input)) return input;
		String quote = "\'";
		boolean needQuote = false;
		if (input.indexOf('\'') > -1)
		{
			quote = "\"";
			needQuote = true;
		}
		if (input.indexOf(' ') > -1)
		{
			needQuote = true;
		}
		if (needQuote)
		{
			return quote + input + quote;
		}
		return input;
	}

}
