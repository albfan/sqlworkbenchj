/*
 * StringUtil.java
 *
 * Created on December 2, 2001, 9:35 PM
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.Character;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *	@author  workbench@kellerer.org
 */
public class StringUtil
{
	public static Pattern PATTERN_CRLF = Pattern.compile("(\r\n|\n\r|\r|\n)");

	public static final String LINE_TERMINATOR = System.getProperty("line.separator");
	public static final String PATH_SEPARATOR = System.getProperty("path.separator");
	public static final StringBuffer EMPTY_STRINGBUFFER = new StringBuffer("");
	public static final String EMPTY_STRING = "";
	
	public static final String replace(String aString, String aValue, String aReplacement)
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

	public static final String getStartingWhiteSpace(final String aLine)
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

	public static final List stringToList(String aString, String aDelimiter)
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

	public static final String makeJavaString(String aString)
	{
		StringBuffer result = new StringBuffer("String sql=");
		BufferedReader reader = new BufferedReader(new StringReader(aString));
		boolean first = true;
		try
		{
			String line = reader.readLine();
			while (line != null)
			{
				if (first) first = false;
				else result.append("           ");
				result.append('"');
				result.append(line);
				result.append(" \\n\"");
				//result.append();
				line = reader.readLine();
				if (line != null)
				{
					result.append(" + \n");
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
			int start = l.indexOf('"');
			int end = l.lastIndexOf('"');
			if (start > -1)
			{
				l = l.substring(start + 1, end);
			}
			Matcher m = newline.matcher(l);
			l = m.replaceAll("");
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
		if (input == null) return null;
		if (input.length() == 0) return EMPTY_STRING;
		if (input.indexOf('"') < 0) return input;
		int first=0;
		int len = input.length();
		String result = input.trim();
		while (result.charAt(first) == '"' && first < len) first++;
		int last = result.length() - 1;
		while (result.charAt(last) == '"' && last > first) last--;
		result = result.substring(first, last + 1);
		return result;
	}
	
	public static final String capitalize(String aString)
	{
		StringBuffer result = new StringBuffer(aString);
		char ch = aString.charAt(0);
		result.setCharAt(0, Character.toUpperCase(ch));
		return result.toString();
	}

	public static String getStackTrace(Throwable th)
	{
		StringWriter writer = new StringWriter(500);
		PrintWriter pw = new PrintWriter(writer);
		th.printStackTrace(pw);
		return writer.getBuffer().toString();
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

		int i, j, k, l ;

		i = s.indexOf("&");
		if (i > -1)
		{
			j = s.indexOf(";");
			if (j > i)
			{
				// ok this is not most optimized way to
				// do it, a StringBuffer would be better,
				// this is left as an exercise to the reader!
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
	
	public static List split(String aString, String delim, boolean singleDelimiter, String quoteChars, boolean keepQuotes)
	{
		WbStringTokenizer tok = new WbStringTokenizer(delim, singleDelimiter, quoteChars, keepQuotes);
		tok.setSourceString(aString);
		
		List result = new ArrayList();
		String token = null;
		while (tok.hasMoreTokens())
		{
			token = tok.nextToken();
			if (token != null) result.add(token);
		}
		return result;
	}
	
	public static void main(String args[])
	{
		//String test = "String sql = \"SELECT column \\n\" + \n\"  FROM test \\r\" + \n\"  WHERE x= 10\"; ";
		//System.out.println(cleanJavaString(test));
		//String test = "Profile name\"";
		String test = "spool -t type \t-f \"file name.sql\" -b tablename;select * from test where name='test';";
		//split(test, " \t", false, "\"'", true);
		//System.out.println("---");
		//test = "spool -t type \t-f \"file name.sql\" -b tablename./select * from test where name='test'./";
		test = "spool /type=text /file=\"file name.sql\" /table=tablename";
		List result = split(test, "/", false, "\"'", true);
		System.out.println("--");
		for (int i=0; i < result.size(); i++)
		{
			System.out.println("value=" + (String)result.get(i));
		}
	}
}
