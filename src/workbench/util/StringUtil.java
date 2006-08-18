/*
 * StringUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.LogMgr;

/**
 *
 *	@author  support@sql-workbench.net
 */
public class StringUtil
{
	public static final String REGEX_CRLF = "(\r\n|\n\r|\r|\n)";
	public static final Pattern PATTERN_CRLF = Pattern.compile(REGEX_CRLF);

	public static final String LINE_TERMINATOR = System.getProperty("line.separator");
	public static final String PATH_SEPARATOR = System.getProperty("path.separator");
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final StringBuffer EMPTY_STRINGBUFFER = new StringBuffer("");
	public static final String EMPTY_STRING = "";

	public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
	public static final String ISO_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SS";
	public static final String ISO_TZ_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SS z";
	
	//public static final SimpleDateFormat ISO_DATE_FORMATTER = new SimpleDateFormat(ISO_DATE_FORMAT);
	public static final SimpleDateFormat ISO_TIMESTAMP_FORMATTER = new SimpleDateFormat(ISO_TIMESTAMP_FORMAT);
	public static final SimpleDateFormat ISO_TZ_TIMESTAMP_FORMATTER = new SimpleDateFormat(ISO_TZ_TIMESTAMP_FORMAT);

	public static final StringBuffer emptyBuffer() { return new StringBuffer(0); }
	
	public static final String getCurrentTimestampWithTZString()
	{
		return ISO_TZ_TIMESTAMP_FORMATTER.format(now());
	}

	private static final java.util.Date now()
	{
		return new java.util.Date(System.currentTimeMillis());
	}

	public static Comparator getCaseInsensitiveComparator()
	{
		return new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				if (o1 instanceof String && o2 instanceof String)
				{
					String value1 = (String)o1;
					String value2 = (String)o2;
					return value1.compareToIgnoreCase(value2);
				}
				return 0;
			}
		};
	}

	public static final boolean arraysEqual(String[] one, String[] other)
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
	
	public static final boolean hasOpenQuotes(String data, char quoteChar)
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
	
	public static final StringBuffer replaceToBuffer(String aString, String aValue, String aReplacement)
	{
		return replaceToBuffer(null, aString, aValue, aReplacement);
	}

	/**
	 * Capitalize a single word.
	 * (write the first character in uppercase, the rest in lower case)
	 * This does not loop through the entire string to capitalize every word.
	 */
	public static final String capitalize(String word)
	{
		if (word == null) return null;
		if (word.length() == 0) return word;
		StringBuffer s = new StringBuffer(word.toLowerCase());
		char c = s.charAt(0);
		s.setCharAt(0, Character.toUpperCase(c));
		return s.toString();
	}
	
	public static final String makeFilename(String input)
	{
		return input.replaceAll("[\t\\:\\\\/\\?\\*\\|<>\"'\\{\\}$%§\\[\\]\\^|\\&]", "").toLowerCase();
	}

	public static final StringBuffer replaceToBuffer(StringBuffer target, String aString, String aValue, String aReplacement)
	{
		if (target == null)
		{
			target = new StringBuffer((int)(aString.length() * 1.1));
		}

		int pos = aString.indexOf(aValue);
		if (pos == -1)
		{
			target.append(aString);
			return target;
		}


		int lastpos = 0;
		int len = aValue.length();
		while (pos != -1)
		{
			target.append(aString.substring(lastpos, pos));
			if (aReplacement != null) target.append(aReplacement);
			lastpos = pos + len;
			pos = aString.indexOf(aValue, lastpos);
		}
		if (lastpos < aString.length())
		{
			target.append(aString.substring(lastpos));
		}
		return target;
	}

	public static final String leftString(String aString, int count, boolean includeDots)
	{
		if (aString == null) return null;
		if (aString.length() <= count) return aString;
		if (includeDots)
		{
			return aString.substring(0, count) + "...";
		}
		else
		{
			return aString.substring(0, count);
		}

	}

	public static final String replace(String aString, String aValue, String aReplacement)
	{
		if (aReplacement == null) return aString;

		int pos = aString.indexOf(aValue);
		if (pos == -1) return aString;

		StringBuffer temp = replaceToBuffer(aString, aValue, aReplacement);

		return temp.toString();
	}

	public static boolean isNumber(String value)
	{
		try
		{
			 Double.parseDouble(value);
			 return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public static final boolean isEmptyString(String value)
	{
		if (value == null) return true;
		if (value.length() == 0) return true;
		return false;
	}
	
	public static final String getStartingWhiteSpace(final String aLine)
	{
		if (aLine == null) return null;
		int pos = 0;
		int len = aLine.length();
		if (len == 0) return "";

		char c = aLine.charAt(pos);
		while (c <= ' ' && pos < len - 1)
		{
			pos ++;
			c = aLine.charAt(pos);
		}
		String result = aLine.substring(0, pos);
		return result;
	}

	public static String cleanNonPrintable(String aValue)
	{
		if (aValue == null) return null;
		int len = aValue.length();
		StringBuffer result = new StringBuffer(len);
		for (int i=0; i < len; i++)
		{
			char c = aValue.charAt(i);
			if (c > 32)
			{
				result.append(c);
			}
			else
			{
				result.append(' ');
			}
		}
		return result.toString();
	}

	public static double getDoubleValue(String aValue, double aDefault)
	{
		if (aValue == null) return aDefault;

		double result = aDefault;
		try
		{
			result = Double.parseDouble(aValue);
		}
		catch (NumberFormatException e)
		{
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
			result = Integer.parseInt(aValue);
		}
		catch (NumberFormatException e)
		{
		}
		return result;
	}

	public static final boolean equalString(String one, String other)
	{
		if (one == null && other == null) return true;
		if (one == null && other != null) return false;
		if (one != null && other == null) return false;
		return one.equals(other);
	}

	public static final boolean equalStringIgnoreCase(String one, String other)
	{
		if (one == null && other == null) return true;
		if (one == null && other != null) return false;
		if (one != null && other == null) return false;
		return one.equalsIgnoreCase(other);
	}

	public static final List stringToList(String aString, String aDelimiter)
	{
		return stringToList(aString, aDelimiter, false, false);
	}

	public static final List stringToList(String aString, String aDelimiter, boolean removeEmpty)
	{
		return stringToList(aString, aDelimiter, removeEmpty, false);
	}
	
	public static final List stringToList(String aString, String aDelimiter, boolean removeEmpty, boolean trimEntries)
	{
		return stringToList(aString, aDelimiter, removeEmpty, trimEntries, false);
	}
	/**
	 * Parses the given String and creates a List containing the elements
	 * of the string that are separated by <tt>aDelimiter</aa>
	 *
	 * @param aString the value to be parsed
	 * @param aDelimiter the delimiter to user
	 * @param removeEmpty flag to remove empty entries
	 * @param trimEntries flag to trim entries (will be applied beore checking for empty entries)
   * @param checkBrackets flag to check for quoted delimiters
	 * @return A List of Strings
	 */
	public static final List stringToList(String aString, String aDelimiter, boolean removeEmpty, boolean trimEntries, boolean checkBrackets)
	{
		if (aString == null || aString.length() == 0) return Collections.EMPTY_LIST;
		WbStringTokenizer tok = new WbStringTokenizer(aString, aDelimiter);
		tok.setDelimiterNeedsWhitspace(false);
		tok.setCheckBrackets(checkBrackets);
		ArrayList result = new ArrayList(150);
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

	public static final String[] toArray(Collection c)
	{
		if (c == null) return null;
		if (c.size() == 0) return new String[0];
		Iterator itr = c.iterator();
		int i = 0;
		String[] result = new String[c.size()];
		while (itr.hasNext())
		{
			Object o = itr.next();
			if (o != null)
			{
				result[i] = o.toString();
			} 
			i++;
		}
		return result;
	}
	
	/**
	 * Create a String from the given list, where the elements are delimited
	 * with the supplied delimiter
	 * @return The elements of the list as a String
	 * @param aList The list to process
	 * @param aDelimiter The delimiter to use
	 */
	public static final String listToString(List aList, char aDelimiter)
	{
		return listToString(aList, aDelimiter, false);
	}
	
	public static final String listToString(List aList, char aDelimiter, boolean quoteEntries)
	{
		if (aList == null || aList.size() == 0) return "";
		int count = aList.size();
		int numElements = 0;
		StringBuffer result = new StringBuffer(count * 50);
		for (int i=0; i < count; i++)
		{
			Object o = aList.get(i);
			if (o == null) continue;
			if (numElements > 0)
			{
				result.append(aDelimiter);
			}
			if (quoteEntries) result.append('"');
			result.append(o.toString());
			if (quoteEntries) result.append('"');
			numElements ++;
		}
		return result.toString();
	}


	public static final String makeJavaString(String sql, String prefix, boolean includeNewLines)
	{
		if (sql == null) return "";
		if (prefix == null) prefix = "";
		StringBuffer result = new StringBuffer(sql.length() + prefix.length() + 10);
		result.append(prefix);
		if (prefix.endsWith("=")) result.append(" ");
		int k = result.length();
		StringBuffer indent = new StringBuffer(k);
		for (int i=0; i < k; i++) indent.append(' ');
		BufferedReader reader = new BufferedReader(new StringReader(sql));
		boolean first = true;
		try
		{
			String line = reader.readLine();
			while (line != null)
			{
				line = replace(line, "\"", "\\\"");
				if (first) first = false;
				else result.append(indent);
				result.append('"');
				if (line.endsWith(";"))
				{
					line = line.substring(0, line.length() - 1);
				}
				result.append(line);

				line = reader.readLine();
				if (line != null)
				{
					if (includeNewLines)
					{
						result.append(" \\n\"");
					}
					else
					{
						result.append(" \"");
					}
					result.append(" + \n");
				}
				else
				{
					result.append("\"");
				}
			}
			result.append(';');
		}
		catch (Exception e)
		{
			result.append("(Error when creating Java code, see logfile for details)");
			LogMgr.logError("StringUtil.makeJavaString()", "Error creating Java String", e);
		}
		finally
		{
			try { reader.close(); } catch (Exception e) {}
		}
		return result.toString();
	}

	
	public static final String cleanJavaString(String aString)
	{
		if (isEmptyString(aString)) return "";
		Pattern newline = Pattern.compile("\\\\n|\\\\r");
		String lines[] = PATTERN_CRLF.split(aString);
		StringBuffer result = new StringBuffer(aString.length());
		int count = lines.length;
		for (int i=0; i < count; i ++)
		{
			//String l = (String)lines.get(i);
			String l = lines[i];
			if (l == null) continue;
			if (l.trim().startsWith("//"))
			{
				l = l.replaceFirst("//", "--");
			}
			else
			{
				l = l.trim();
				//if (l.startsWith("\"")) start = 1;
				int start = l.indexOf("\"");
				int end = l.lastIndexOf("\"");
				if (end == start) start = 1;
				if (end == 0) end = l.length() - 1;
				if (start > -1) start ++;
				if (start > -1 && end > -1)
				{
					l = l.substring(start, end);
				}
			}
			Matcher m = newline.matcher(l);
			l = m.replaceAll("");
			l = replace(l,"\\\"", "\"");
			result.append(l);
			if (i < count - 1) result.append('\n');
		}
		return result.toString();
	}

	public static final String trimQuotes(String input)
	{
		if (input == null) return null;
		if (input.length() == 0) return EMPTY_STRING;
		if (input.length() == 1) return input;

		String result = input.trim();
		int len = result.length();
		if (len == 0) return EMPTY_STRING;
		if (len == 1) return input;

		char firstChar = result.charAt(0);
		char lastChar = result.charAt(len - 1);

		if ( (firstChar == '"' && lastChar == '"') ||
		     (firstChar == '\'' && lastChar == '\''))
		{
			return result.substring(1, len - 1);
		}
		return input;
	}

	public static final String escapeXML(String s)
	{
		StringBuffer result = null;

		for(int i = 0, max = s.length(), delta = 0; i < max; i++)
		{
			char c = s.charAt(i);
			String replacement = null;

			switch (c)
			{
				case '&': replacement = "&amp;"; break;
				case '<': replacement = "&lt;"; break;
				case '\r': replacement = "&#13;"; break;
				case '\n': replacement = "&#10;"; break;
				case '>': replacement = "&gt;"; break;
				case '"': replacement = "&quot;"; break;
				case '\'': replacement = "&apos;"; break;
				case (char)0: replacement = ""; break;
			}

			if (replacement != null)
			{
				if (result == null)
				{
					result = new StringBuffer(s);
				}
				result.replace(i + delta, i + delta + 1, replacement);
				delta += (replacement.length() - 1);
			}
		}
		if (result == null)
		{
			return s;
		}
		return result.toString();

	}

	public static final String escapeHTML(String s)
	{
		if (s == null) return null;
		StringBuffer sb = new StringBuffer(s.length() + 100);
		int n = s.length();
		for (int i = 0; i < n; i++)
		{
			char c = s.charAt(i);
			switch (c)
			{
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '&': sb.append("&amp;"); break;
				case '"': sb.append("&quot;"); break;
				case 'à': sb.append("&agrave;");break;
				case 'À': sb.append("&Agrave;");break;
				case 'â': sb.append("&acirc;");break;
				case 'Â': sb.append("&Acirc;");break;
				case 'ä': sb.append("&auml;");break;
				case 'Ä': sb.append("&Auml;");break;
				case 'å': sb.append("&aring;");break;
				case 'Å': sb.append("&Aring;");break;
				case 'æ': sb.append("&aelig;");break;
				case 'Æ': sb.append("&AElig;");break;
				case 'ç': sb.append("&ccedil;");break;
				case 'Ç': sb.append("&Ccedil;");break;
				case 'é': sb.append("&eacute;");break;
				case 'É': sb.append("&Eacute;");break;
				case 'è': sb.append("&egrave;");break;
				case 'È': sb.append("&Egrave;");break;
				case 'ê': sb.append("&ecirc;");break;
				case 'Ê': sb.append("&Ecirc;");break;
				case 'ë': sb.append("&euml;");break;
				case 'Ë': sb.append("&Euml;");break;
				case 'ï': sb.append("&iuml;");break;
				case 'Ï': sb.append("&Iuml;");break;
				case 'ô': sb.append("&ocirc;");break;
				case 'Ô': sb.append("&Ocirc;");break;
				case 'ö': sb.append("&ouml;");break;
				case 'Ö': sb.append("&Ouml;");break;
				case 'ø': sb.append("&oslash;");break;
				case 'Ø': sb.append("&Oslash;");break;
				case 'ß': sb.append("&szlig;");break;
				case 'ù': sb.append("&ugrave;");break;
				case 'Ù': sb.append("&Ugrave;");break;
				case 'û': sb.append("&ucirc;");break;
				case 'Û': sb.append("&Ucirc;");break;
				case 'ü': sb.append("&uuml;");break;
				case 'Ü': sb.append("&Uuml;");break;
				case '®': sb.append("&reg;");break;
				case '©': sb.append("&copy;");break;
				case '€': sb.append("&euro;"); break;

				// be carefull with this one (non-breaking whitee space)
				//case ' ': sb.append("&nbsp;");break;

				default:  sb.append(c); break;
			}
		}
		return sb.toString();
	}

	public static final String unescapeHTML(String s)
	{
		String [][] escape =
		{
			{  "&lt;"     , "<" } ,
			{  "&gt;"     , ">" } ,
			{  "&amp;"    , "&" } ,
			{  "&quot;"   , "\"" } ,
			{  "&agrave;" , "à" } ,
			{  "&Agrave;" , "À" } ,
			{  "&acirc;"  , "â" } ,
			{  "&auml;"   , "ä" } ,
			{  "&Auml;"   , "Ä" } ,
			{  "&Acirc;"  , "Â" } ,
			{  "&aring;"  , "å" } ,
			{  "&Aring;"  , "Å" } ,
			{  "&aelig;"  , "æ" } ,
			{  "&AElig;"  , "Æ" } ,
			{  "&ccedil;" , "ç" } ,
			{  "&Ccedil;" , "Ç" } ,
			{  "&eacute;" , "é" } ,
			{  "&Eacute;" , "É" } ,
			{  "&egrave;" , "è" } ,
			{  "&Egrave;" , "È" } ,
			{  "&ecirc;"  , "ê" } ,
			{  "&Ecirc;"  , "Ê" } ,
			{  "&euml;"   , "ë" } ,
			{  "&Euml;"   , "Ë" } ,
			{  "&iuml;"   , "ï" } ,
			{  "&Iuml;"   , "Ï" } ,
			{  "&ocirc;"  , "ô" } ,
			{  "&Ocirc;"  , "Ô" } ,
			{  "&ouml;"   , "ö" } ,
			{  "&Ouml;"   , "Ö" } ,
			{  "&oslash;" , "ø" } ,
			{  "&Oslash;" , "Ø" } ,
			{  "&szlig;"  , "ß" } ,
			{  "&ugrave;" , "ù" } ,
			{  "&Ugrave;" , "Ù" } ,
			{  "&ucirc;"  , "û" } ,
			{  "&Ucirc;"  , "Û" } ,
			{  "&uuml;"   , "ü" } ,
			{  "&Uuml;"   , "Ü" } ,
			{  "&nbsp;"   , " " } ,
			{  "&reg;"    , "\u00a9" } ,
			{  "&copy;"   , "\u00ae" } ,
			{  "&euro;"   , "\u20a0" }
		};

		int i, j, k;

		i = s.indexOf("&");
		if (i > -1)
		{
			j = s.indexOf(";");
			if (j > i)
			{
				String temp = s.substring(i , j + 1);
				// search in escape[][] if temp is there
				k = 0;
				while (k < escape.length)
				{
					if (escape[k][0].equals(temp)) break;
					else k++;
				}
				s = s.substring(0 , i) + escape[k][1] + s.substring(j + 1);
				return unescapeHTML(s); // recursive call
			}
		}
		return s;
	}

	public static boolean stringToBool(String aString)
	{
		if (aString == null) return false;
		return ("true".equalsIgnoreCase(aString) || "1".equals(aString) || "y".equalsIgnoreCase(aString) || "yes".equalsIgnoreCase(aString) );
	}

	public static final String getMaxSubstring(String s, int maxLen)
	{
		if (maxLen < 1) return s;
		if (s == null) return null;
		if (s.length() < maxLen) return s;
		return s.substring(0, maxLen - 1) + "...";
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
		StringBuffer buf = new StringBuffer(len + 5);
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
		if (pos > count || pos <= 1) return -1;
		for (int i=pos; i > 0; i--)
		{
			char c = data.charAt(i);
			if (wordBoundaries.indexOf(c) > -1 || Character.isWhitespace(c)) return i;
		}
		return -1;
	}
	
	public static int findFirstWhiteSpace(String data)
	{
		return findFirstWhiteSpace(data, 0);
	}
	
	public static int findFirstWhiteSpace(String data, int start)
	{
		if (start < 0) return -1;
		if (data == null) return -1;
		int count = data.length();
		if (start >= count) return -1;
		for (int i=start; i < count; i++)
		{
			if (Character.isWhitespace(data.charAt(i))) return i;
		}
		return -1;
	}

	public static final String getWordLeftOfCursor(String text, int pos, String wordBoundaries)
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
			if (startOfWord > 0)
			{
				word = text.substring(startOfWord+1, Math.min(pos,len));
			}
			return word;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}	
	
	public static int findPattern(String regex, String data)
	{
		return findPattern(regex, data);
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
		char aChar;
		int len = theString.length();
		if (len == 0) return theString;
		StringBuffer outBuffer = new StringBuffer(len);

		for (int x=0; x<len; )
		{
			aChar = theString.charAt(x++);
			if (aChar == '\\')
			{
				aChar = theString.charAt(x++);
				if (aChar == 'u')
				{
					// Read the xxxx
					int value=0;
					for (int i=0; i<4; i++)
					{
						aChar = theString.charAt(x++);
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
								throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
						}
					}
					outBuffer.append((char)value);
				}
				else
				{
					if (aChar == 't') aChar = '\t';
					else if (aChar == 'r') aChar = '\r';
					else if (aChar == 'n') aChar = '\n';
					else if (aChar == 'f') aChar = '\f';
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

	public static String escapeUnicode(String value, CharacterRange range)
	{
		return escapeUnicode(value, null, range);
	}
	/*
	 * Converts unicodes to encoded &#92;uxxxx
	 * and writes out any of the characters in specialSaveChars
	 * with a preceding slash.
	 * This has been "borrowed" from the Properties class, because the code
	 * there is not usable from the outside.
	 * Backslash, CR, LF, Tab and FormFeed (\f) will always be replaced.
	 */
	public static String escapeUnicode(String value, String specialSaveChars, CharacterRange range)
	{
		if (value == null) return null;
		int len = value.length();
		StringBuffer outBuffer = new StringBuffer(len*2);

		for(int x=0; x<len; x++)
		{
			char aChar = value.charAt(x);
			switch(aChar)
			{
				case '\\':
					outBuffer.append('\\');
					outBuffer.append('\\');
					break;
				case '\t':
					outBuffer.append('\\');
					outBuffer.append('t');
					break;
				case '\n':
					outBuffer.append('\\');
					outBuffer.append('n');
					break;
				case '\r':
					outBuffer.append('\\');
					outBuffer.append('r');
					break;
				case '\f':
					outBuffer.append('\\');
					outBuffer.append('f');
					break;
				default:
					if (range.isOutsideRange(aChar))
					{
						outBuffer.append('\\');
						outBuffer.append('u');
						outBuffer.append(hexDigit((aChar >> 12) & 0xF));
						outBuffer.append(hexDigit((aChar >>  8) & 0xF));
						outBuffer.append(hexDigit((aChar >>  4) & 0xF));
						outBuffer.append(hexDigit( aChar        & 0xF));
					}
					else if (specialSaveChars != null && specialSaveChars.indexOf(aChar) > -1)
					{
						outBuffer.append('\\');
						outBuffer.append('u');
						outBuffer.append(hexDigit((aChar >> 12) & 0xF));
						outBuffer.append(hexDigit((aChar >>  8) & 0xF));
						outBuffer.append(hexDigit((aChar >>  4) & 0xF));
						outBuffer.append(hexDigit( aChar        & 0xF));
					}
					else
					{
						outBuffer.append(aChar);
					}
			}
		}
		return outBuffer.toString();
	}

	private static char hexDigit(int nibble)
	{
		return hexDigit[(nibble & 0xF)];
	}

	private static final char[] hexDigit = {
		'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
	};

}
