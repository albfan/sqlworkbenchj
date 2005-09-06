/*
 * StringUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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
	public static final Pattern PATTERN_CRLF = Pattern.compile("(\r\n|\n\r|\r|\n)");

	public static final String LINE_TERMINATOR = System.getProperty("line.separator");
	public static final String PATH_SEPARATOR = System.getProperty("path.separator");
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final StringBuffer EMPTY_STRINGBUFFER = new StringBuffer("");
	public static final String EMPTY_STRING = "";

	public static final SimpleDateFormat ISO_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat ISO_TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");
	public static final SimpleDateFormat ISO_TZ_TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS z");

	/**
	 *	Returns the current date formatted as yyyy-MM-dd
	 */
	public static final String getCurrentDateString()
	{
		return ISO_DATE_FORMATTER.format(now());
	}

	public static final String getCurrentTimestampString()
	{
		return ISO_TIMESTAMP_FORMATTER.format(now());
	}

	public static final String getCurrentTimestampWithTZString()
	{
		return ISO_TZ_TIMESTAMP_FORMATTER.format(now());
	}

	private static final java.util.Date now()
	{
		return new java.util.Date(System.currentTimeMillis());
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
		return input.replaceAll("[\t\\:\\\\/\\?\\*\\|<>\"'\\{\\}$%�\\[\\]\\^|\\&]", "").toLowerCase();
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
	
	/**
	 * 	Parses the given String and creates a List containing the elements
	 *  of the string that are separated by <tt>aDelimiter</aa>
	 * @param aString the separated input to parse
	 * @param aDelimiter the delimiter to user
	 * @param removeEmpty flag to remove empty entries
	 * @param trimEntries flag to trim entries
	 * @return A List of Strings
	 */
	public static final List stringToList(String aString, String aDelimiter, boolean removeEmpty, boolean trimEntries)
	{
    if (aString == null || aString.length() == 0) return Collections.EMPTY_LIST;
		WbStringTokenizer tok = new WbStringTokenizer(aString, aDelimiter);
		ArrayList result = new ArrayList(150);
		while (tok.hasMoreTokens())
		{
			String element = tok.nextToken();
			if (element == null) continue;
			if (removeEmpty && element.trim().length() == 0) continue;
			if (trimEntries)
				result.add(element.trim());
			else
				result.add(element);
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
			result.append(o.toString());
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
		Pattern newline = Pattern.compile("\\\\n|\\\\r");
		if (aString == null || aString.trim().length() == 0) return "";
		List lines = getTextLines(aString);
		StringBuffer result = new StringBuffer(aString.length());
		int count = lines.size();
		for (int i=0; i < count; i ++)
		{
			String l = (String)lines.get(i);
			if ( l == null) continue;
			if (l.trim().startsWith("//"))
			{
				l = l.replaceFirst("//", "--");
			}
			else
			{
				int start = l.indexOf('"');
				int end = l.lastIndexOf('"');
				if (start > -1)
				{
					l = l.substring(start + 1, end);
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

	public static List getTextLines(String aScript)
	{
		ArrayList l = new ArrayList(100);
		getTextLines(l, aScript);
		return l;
	}

	public static void getTextLines(List aList, String aScript)
	{
		if (aScript == null) return;
		if (aScript.length() > 100)
		{
			// the solution with the StringReader performs
			// better on long Strings, for short strings
			// using regex is faster
			readTextLines(aList, aScript);
			return;
		}

		if (aList == null) return;
		aList.clear();
		Matcher m = StringUtil.PATTERN_CRLF.matcher(aScript);
		int start = 0;
		boolean notOne = true;
		while (m.find())
		{
			notOne = false;
			String line = aScript.substring(start, m.start());
			if (line != null)
			{
				aList.add(line.trim());
			}
			start = m.end();
		}

		if (notOne)
			aList.add(aScript);
		else if (start < aScript.length())
			aList.add(aScript.substring(start));
	}

	private static void readTextLines(List aList, String aScript)
	{
		aList.clear();
		if (aScript.indexOf('\n') < 0)
		{
			aList.add(aScript);
			return;
		}

		BufferedReader br = new BufferedReader(new StringReader(aScript));
		String line;
		try
		{
			while ((line = br.readLine()) != null)
			{
				aList.add(line.trim());
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try { br.close(); } catch (Throwable th) {}
		}
	}

	public static final String trimQuotes(String input)
	{
		//System.out.println("toTrim=" + input);
		if (input == null) return null;
		if (input.length() == 0) return EMPTY_STRING;
		if (input.length() == 1) return input;

		String result = input.trim();
		int first = 0;
		int len = result.length();
		if (len == 0) return EMPTY_STRING;
		if (len == 1) return input;

		char firstChar = result.charAt(0);
		char lastChar = result.charAt(len - 1);

		if ( (firstChar == '"' && lastChar == '"') ||
		     (firstChar == '\'' && lastChar == '\''))
		{
			result = result.substring(1, len - 1);
		}
		return result;
	}

	public static final String cleanupUnderscores(String aString, boolean capitalize)
	{
		if (aString == null) return null;
		int pos = aString.indexOf('_');

		int len = aString.length();
		StringBuffer result = new StringBuffer(len);

		if (capitalize)
			result.append(Character.toUpperCase(aString.charAt(0)));
		else
			result.append(aString.charAt(0));

		for (int i=1; i < len; i++)
		{
			char c = aString.charAt(i);
			if (c == '_')
			{
				if (i < len - 1)
				{
					i++;
					c = Character.toUpperCase(aString.charAt(i));
				}
			}
			else
			{
				c = Character.toLowerCase(aString.charAt(i));
			}
			result.append(c);
		}
		return result.toString();
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
				case '�': sb.append("&agrave;");break;
				case '�': sb.append("&Agrave;");break;
				case '�': sb.append("&acirc;");break;
				case '�': sb.append("&Acirc;");break;
				case '�': sb.append("&auml;");break;
				case '�': sb.append("&Auml;");break;
				case '�': sb.append("&aring;");break;
				case '�': sb.append("&Aring;");break;
				case '�': sb.append("&aelig;");break;
				case '�': sb.append("&AElig;");break;
				case '�': sb.append("&ccedil;");break;
				case '�': sb.append("&Ccedil;");break;
				case '�': sb.append("&eacute;");break;
				case '�': sb.append("&Eacute;");break;
				case '�': sb.append("&egrave;");break;
				case '�': sb.append("&Egrave;");break;
				case '�': sb.append("&ecirc;");break;
				case '�': sb.append("&Ecirc;");break;
				case '�': sb.append("&euml;");break;
				case '�': sb.append("&Euml;");break;
				case '�': sb.append("&iuml;");break;
				case '�': sb.append("&Iuml;");break;
				case '�': sb.append("&ocirc;");break;
				case '�': sb.append("&Ocirc;");break;
				case '�': sb.append("&ouml;");break;
				case '�': sb.append("&Ouml;");break;
				case '�': sb.append("&oslash;");break;
				case '�': sb.append("&Oslash;");break;
				case '�': sb.append("&szlig;");break;
				case '�': sb.append("&ugrave;");break;
				case '�': sb.append("&Ugrave;");break;
				case '�': sb.append("&ucirc;");break;
				case '�': sb.append("&Ucirc;");break;
				case '�': sb.append("&uuml;");break;
				case '�': sb.append("&Uuml;");break;
				case '�': sb.append("&reg;");break;
				case '�': sb.append("&copy;");break;
				case '�': sb.append("&euro;"); break;

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
			{  "&agrave;" , "�" } ,
			{  "&Agrave;" , "�" } ,
			{  "&acirc;"  , "�" } ,
			{  "&auml;"   , "�" } ,
			{  "&Auml;"   , "�" } ,
			{  "&Acirc;"  , "�" } ,
			{  "&aring;"  , "�" } ,
			{  "&Aring;"  , "�" } ,
			{  "&aelig;"  , "�" } ,
			{  "&AElig;"  , "�" } ,
			{  "&ccedil;" , "�" } ,
			{  "&Ccedil;" , "�" } ,
			{  "&eacute;" , "�" } ,
			{  "&Eacute;" , "�" } ,
			{  "&egrave;" , "�" } ,
			{  "&Egrave;" , "�" } ,
			{  "&ecirc;"  , "�" } ,
			{  "&Ecirc;"  , "�" } ,
			{  "&euml;"   , "�" } ,
			{  "&Euml;"   , "�" } ,
			{  "&iuml;"   , "�" } ,
			{  "&Iuml;"   , "�" } ,
			{  "&ocirc;"  , "�" } ,
			{  "&Ocirc;"  , "�" } ,
			{  "&ouml;"   , "�" } ,
			{  "&Ouml;"   , "�" } ,
			{  "&oslash;" , "�" } ,
			{  "&Oslash;" , "�" } ,
			{  "&szlig;"  , "�" } ,
			{  "&ugrave;" , "�" } ,
			{  "&Ugrave;" , "�" } ,
			{  "&ucirc;"  , "�" } ,
			{  "&Ucirc;"  , "�" } ,
			{  "&uuml;"   , "�" } ,
			{  "&Uuml;"   , "�" } ,
			{  "&nbsp;"   , " " } ,
			{  "&reg;"    , "\u00a9" } ,
			{  "&copy;"   , "\u00ae" } ,
			{  "&euro;"   , "\u20a0" }
		};

		int i, j, k, l ;

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

	public static List split(String aString, String delim, boolean singleDelimiter, String quoteChars, boolean keepQuotes)
	{
		if (aString == null) return Collections.EMPTY_LIST;
		WbStringTokenizer tok = new WbStringTokenizer(delim, singleDelimiter, quoteChars, keepQuotes);
		tok.setSourceString(aString.trim());

		List result = new ArrayList();
		String token = null;
		while (tok.hasMoreTokens())
		{
			token = tok.nextToken();
			if (token != null) result.add(token);
		}
		return result;
	}

	public static final String getMaxSubstring(String s, int maxLen)
	{
		return getMaxSubstring(s, maxLen, "...");
	}

	public static final String getMaxSubstring(String s, int maxLen, String cont)
	{
		if (s == null) return null;
		if (s.length() < maxLen) return s;
		return s.substring(0, maxLen - 1) + cont;
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
		int index = pos;
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
		int index = pos;
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
		if (pos < 0) return null;
		int len = text.length();
		int end = pos;
		if (pos >= len) 
		{
			end = len - 1;
		}
		if (end < 1) return null;
		
		if (Character.isWhitespace(text.charAt(end-1))) return null;
		
		String word = null;
		int startOfWord = findWordBoundary(text, end-1, wordBoundaries);
		if (startOfWord > 0)
		{
			word = text.substring(startOfWord+1, Math.min(pos,len));
		}

		return word;
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
						outBuffer.append(toHex((aChar >> 12) & 0xF));
						outBuffer.append(toHex((aChar >>  8) & 0xF));
						outBuffer.append(toHex((aChar >>  4) & 0xF));
						outBuffer.append(toHex( aChar        & 0xF));
					}
					else if (specialSaveChars != null && specialSaveChars.indexOf(aChar) > -1)
					{
						outBuffer.append('\\');
						outBuffer.append('u');
						outBuffer.append(toHex((aChar >> 12) & 0xF));
						outBuffer.append(toHex((aChar >>  8) & 0xF));
						outBuffer.append(toHex((aChar >>  4) & 0xF));
						outBuffer.append(toHex( aChar        & 0xF));
					}
					else
					{
						outBuffer.append(aChar);
					}
			}
		}
		return outBuffer.toString();
	}

	public static char toHex(int nibble)
	{
		return hexDigit[(nibble & 0xF)];
	}

	private static final char[] hexDigit = {
		'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
	};

}
