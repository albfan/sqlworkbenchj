/*
 * StringUtil.java
 *
 * Created on December 2, 2001, 9:35 PM
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.Character;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 *	@author  workbench@kellerer.org
 */
public class StringUtil
{
	public static final String LINE_TERMINATOR = System.getProperty("line.separator");
	public static final String PATH_SEPARATOR = System.getProperty("path.separator");
	
	public static String replace(String aString, String aValue, String aReplacement)
	{
		if (aReplacement == null) return aString;

		int pos = aString.indexOf(aValue);
		if (pos == -1) return aString;

		StringBuffer temp = new StringBuffer(aString.length());

		int lastpos = 0;
		int len = aValue.length();
		while (pos != -1)
		{
			temp.append(aString.substring(lastpos, pos));
			temp.append(aReplacement);
			lastpos = pos + len;
			pos = aString.indexOf(aValue, lastpos);
		}
		if (lastpos < aString.length())
		{
			temp.append(aString.substring(lastpos));
		}
		return temp.toString();
	}

	public static String getStartingWhiteSpace(final String aLine)
	{
		if (aLine == null) return null;
		int pos = 0;
		int len = aLine.length();
		if (len == 0) return "";

		char c = aLine.charAt(pos);
		while (c <= ' ' && pos < len)
		{
			pos ++;
			c = aLine.charAt(pos);
		}
		String result = aLine.substring(0, pos);
		return result;
	}

	public static int getIntValue(String aValue)
	{
		return getIntValue(aValue, 0);
	}
	public static int getIntValue(String aValue, int aDefault)
	{
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

	public static List stringToList(String aString, String aDelimiter)
	{
    if (aString == null || aString.length() == 0) return Collections.EMPTY_LIST;
		LineTokenizer tok = new LineTokenizer(aString, aDelimiter);
		ArrayList result = new ArrayList(tok.countTokens());
		while (tok.hasMoreTokens())
		{
			result.add(tok.nextToken());
		}
		return result;
	}
	public static String makeJavaString(String aString)
	{
		StringBuffer result = new StringBuffer("String sql=");
		BufferedReader reader = new BufferedReader(new StringReader(aString));
		try
		{
			String line = reader.readLine();
			while (line != null)
			{
				result.append('"');
				result.append(line);
				result.append(" \"");
				//result.append();
				line = reader.readLine();
				if (line != null)
				{
					result.append(" + \r");
				}
			}
			result.append(';');
		}
		catch (Exception e)
		{
			result.append("(Error)");
		}
		finally
		{
			try { reader.close(); } catch (Exception e) {}
		}
		return result.toString();
	}

	public static String capitalize(String aString)
	{
		StringBuffer result = new StringBuffer(aString);
		char ch = aString.charAt(0);
		result.setCharAt(0, Character.toUpperCase(ch));
		return result.toString();
	}
	
	public static void main(String args[])
	{
		System.getProperties().list(System.out);
	}
}
