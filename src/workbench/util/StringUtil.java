/*
 * StringUtil.java
 *
 * Created on December 2, 2001, 9:35 PM
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
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
	public static final Pattern PATTERN_CRLF = Pattern.compile("(\r\n|\n\r|\r|\n)");

	public static final Pattern PATTERN_EMPTY_LINE = Pattern.compile("$(\r\n|\n\r|\r|\n)");
	
	public static final String LINE_TERMINATOR = System.getProperty("line.separator");
	public static final String PATH_SEPARATOR = System.getProperty("path.separator");
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final StringBuffer EMPTY_STRINGBUFFER = new StringBuffer("");
	public static final String EMPTY_STRING = "";

	public static final StringBuffer replaceToBuffer(String aString, String aValue, String aReplacement)
	{
		return replaceToBuffer(null, aString, aValue, aReplacement);
	}
	
	public static final StringBuffer replaceToBuffer(StringBuffer target, String aString, String aValue, String aReplacement)
	{
		if (target == null)
		{
			target = new StringBuffer((int)(aString.length() * 1.1));
		}
		
		if (aReplacement == null) 
		{
			target.append(aString);
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
			target.append(aReplacement);
			lastpos = pos + len;
			pos = aString.indexOf(aValue, lastpos);
		}
		if (lastpos < aString.length())
		{
			target.append(aString.substring(lastpos));
		}
		return target;
	}
	
	public static final String replace(String aString, String aValue, String aReplacement)
	{
		if (aReplacement == null) return aString;

		int pos = aString.indexOf(aValue);
		if (pos == -1) return aString;

		StringBuffer temp = replaceToBuffer(aString, aValue, aReplacement);
		
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
		WbStringTokenizer tok = new WbStringTokenizer(aString, aDelimiter);
		ArrayList result = new ArrayList(150);
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

	public static final String trimEmptyLines(String input)
	{
		if (input == null) return null;
		Matcher m = PATTERN_EMPTY_LINE.matcher(input);
		return m.replaceAll("");
	}
	public static final String trimQuotes(String input)
	{
		//System.out.println("toTrim=" + input);
		if (input == null) return null;
		if (input.length() == 0) return EMPTY_STRING;
		
		String result = input.trim();
		int first = 0;
		int len = result.length();
		if (len == 0) return EMPTY_STRING;

		char firstChar = result.charAt(0);
		char lastChar = result.charAt(len - 1);
		
		if ( (firstChar == '"' && lastChar == '"') ||
		     (firstChar == '\'' && lastChar == '\''))
		{
			result = result.substring(1, len - 1);
		}
		//System.out.println("trimmed=>" + result + "<" );
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

	public static boolean stringToBool(String aString)
	{
		if (aString == null) return false;
		return ("y".equalsIgnoreCase(aString) || "yes".equalsIgnoreCase(aString) || "1".equals(aString) || "true".equalsIgnoreCase(aString));
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

	public static final String LIST_DELIMITER = "----------- WbStatement -----------";

	public static ArrayList readStringList(String aFilename)
		throws IOException
	{
		File f = new File(aFilename);
		if (!f.exists()) throw new FileNotFoundException(aFilename);
	  Reader in = new FileReader(f);
		return readStringList(in);
	}

	public static ArrayList readStringList(InputStream aStream)
		throws IOException
	{
		Reader in = new InputStreamReader(aStream);
		return readStringList(in);
	}
	
	public static ArrayList readStringList(Reader aReader)
		throws IOException
	{
		ArrayList result = new ArrayList(25);
		long start,end;
		BufferedReader in = new BufferedReader(aReader, 65536);
		StringBuffer content = new StringBuffer(500);
		try
		{
			String line = in.readLine();
			while(line != null)
			{
				if (line.equals(LIST_DELIMITER))
				{
					result.add(content.toString());
					content = new StringBuffer(500);
				}
				else
				{
					content.append(line);
					content.append('\n');
				}
				line = in.readLine();
			}
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		if (content.length() > 0)
		{
			result.add(content.toString());
		}
		return result;
	}

	public static void writeStringList(List aList, String aFilename)
		throws IOException
	{
		Writer out = new FileWriter(aFilename);
		writeStringList(aList, out, true);
	}
	
	public static void writeStringList(List aList, OutputStream out)
		throws IOException
	{
		Writer w = new OutputStreamWriter(out);
		writeStringList(aList, w, false);
	}	
	
	public static void writeStringList(List aList, Writer aWriter, boolean closeStream)
		throws IOException
	{
		if (aList == null) return;
		BufferedWriter out = null;
		try
		{
			out = new BufferedWriter(aWriter);
			for (int i=0; i < aList.size(); i++)
			{
				String content = (String)aList.get(i);
				if (content != null && content.trim().length() > 0)
				{
					out.write(content.trim());
					out.write(LINE_TERMINATOR);
					out.write(LIST_DELIMITER);
					out.write(LINE_TERMINATOR);
				}
			}
			out.flush();
		}
		finally
		{
			if (closeStream)
			{
				try { out.close(); } catch (Throwable th) {}
			}
		}
	}

	public static void main(String args[])
	{
		try
		{
			String test = "\"";
			test = test + "\\\\";
			test = test + "\"\"";
			
			System.out.println("result=>" + trimQuotes(test) + "<");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}