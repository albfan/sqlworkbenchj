package workbench.util;

import java.lang.Character;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import workbench.storage.DataStore;

public class SqlUtil
{

	/** Creates a new instance of SqlUtil */
	private SqlUtil()
	{
	}

	public static String getSqlVerb(String aStatement)
	{
		StringTokenizer tok = new StringTokenizer(aStatement.trim());
		return tok.nextToken(" \t");
	}

	public static List getCommands(String aScript, String aDelimiter)
	{
		if (aScript == null || aScript.trim().length() == 0) return Collections.EMPTY_LIST;
		ArrayList result = new ArrayList();
		int count, pos, scriptLen, cmdNr, lastPos, delimitLen;
		boolean quoteOn;
		ArrayList emptyList, commands;
		String value, ls_OldDelimit, delimit;
		int oldPos;
		String currChar;

		aScript = aScript.trim();
		// Handle MS SQL GO's
		aScript = StringUtil.replace(aScript, StringUtil.LINE_TERMINATOR + "GO" + StringUtil.LINE_TERMINATOR, StringUtil.LINE_TERMINATOR);
		pos = aScript.indexOf(aDelimiter);
		if (pos == -1 || pos == aScript.length() - 1)
		{
			result.add(aScript);
			return result;
		}
		quoteOn = false;
		cmdNr = 0;
		scriptLen = aScript.length();
		delimit = aDelimiter.trim().toUpperCase();
		delimitLen = delimit.length();
		lastPos = 0;

		for (pos = 0; pos < scriptLen; pos++)
		{

			currChar = aScript.substring(pos, pos + 1);
			if (currChar.equals("\'") || currChar.equals("\""))
			{
				quoteOn = !quoteOn;
			}

			if (!quoteOn)
			{
				if (delimitLen > 1)
				{
					currChar = aScript.substring(pos, pos + delimitLen).toUpperCase();
				}

				if ((currChar.equals(delimit) || (pos == scriptLen)))
				{
					if (pos == scriptLen)
					{
						value = aScript.substring(lastPos, pos).trim();
						if (value.substring(value.length() - 1, value.length()).equals(";"))
						{
							value = value.substring(0, value.length() - 2);
						}
					}
					else
					{
						value = aScript.substring(lastPos, pos).trim();
					}
					if (value.length() > 0)
					{
						result.add(value);
					}
					lastPos = pos + delimitLen;
				}
			}

		}
		if (lastPos < pos)
		{
			value = aScript.substring(lastPos).trim();
			result.add(value);
		}
		return result;
	}

	/**
	 *	Returns a literal which can be used directly in a SQL statement.
	 *	This method will quote character datatypes and convert
	 *	Date datatypes to the correct format.
	 */
	public static String getLiteral(Object aValue)
	{
		if (aValue == null) return "NULL";
		if (aValue == DataStore.NULL_VALUE) return "NULL";

		if (aValue instanceof String)
		{
			// Single quotes in a String must be "quoted"...
			String realValue = StringUtil.replace((String)aValue, "'", "''");
			return "'" + realValue + "'";
		}
		else if (aValue instanceof Date)
		{
			return "'" + aValue.toString() + "'";
		}
		else
		{
			return aValue.toString();
		}

	}

	/**
	 * Return the list of tables which are in the FROM list of the given SQL statement.
	 */
	public static List getTables(String aSql)
	{
		boolean inQotes = false;
		boolean fromFound = false;
		String orgSql = makeCleanSql(aSql);
		aSql = aSql.toUpperCase();
		
		final String FROM = " FROM ";
		int fromPos = aSql.indexOf(FROM);
		if (fromPos == -1) return Collections.EMPTY_LIST;
		
		int quotePos = aSql.indexOf('\'');
		int pos;
		if (quotePos != -1 && quotePos < fromPos)
		{
			while (!fromFound)
			{
				pos = skipQuotes(aSql, quotePos + 1);
				fromPos = aSql.indexOf(FROM, pos);
				if (fromPos == -1) break;
				quotePos = aSql.indexOf('\'', pos);
				fromFound = (quotePos == -1 || (quotePos > fromPos));
			}
		}
		if (fromPos == -1) return Collections.EMPTY_LIST;
		int fromEnd = aSql.indexOf(" WHERE ");
		if (fromEnd == -1) fromEnd = aSql.indexOf(" ORDER ");
		if (fromEnd == -1) fromEnd = aSql.indexOf(" GROUP ");
		if (fromEnd == -1) fromEnd = aSql.length();
		String fromList = orgSql.substring(fromPos + FROM.length(), fromEnd);
		StringTokenizer tok = new StringTokenizer(fromList, ",");
		ArrayList result = new ArrayList();
		while (tok.hasMoreTokens())
		{
			String table = tok.nextToken().trim();
			pos = table.indexOf(' ');
			if (pos != -1)
			{
				table = table.substring(0, pos);
			}
			result.add(table);
		}
			
		return result;
	}

	private static String makeCleanSql(String aSql)
	{
		int count = aSql.length();
		aSql = aSql.trim();
		StringBuffer newSql = new StringBuffer(count);
		for (int i=0; i < count; i++)
		{
			char c = aSql.charAt(i);
			if (Character.isWhitespace(c) || Character.isISOControl(c) )
			{
				newSql.append(' ');
			}
			else
			{
				newSql.append(c);
			}
		}
		return newSql.toString();
	}
	
	private static int skipQuotes(String aString, int aStartpos)
	{
		char c = aString.charAt(aStartpos);
		while (c != '\'')
		{
			aStartpos ++;
			c = aString.charAt(aStartpos);
		}
		return aStartpos + 1;
	}

	public static void main(String args[])
	{
		//String script = "select 'testing'';''', test.spalte1 from test;\r\n                ;\r\nupdate test set blba='xx';";
		/*
		String script = "select 'testing'';''', test.spalte1 from test;";
		List commands = getCommands(script, ";");
		for (int i=0; i < commands.size(); i++)
		{
			System.out.println(commands.get(i).toString());
			System.out.println("-----");
		}
		*/
		String sql = "select *,'hallo FROM' from testing t, person p where x='1'";
		List tables = getTables(sql);
		for (int i=0; i < tables.size(); i++)
		{
			System.out.println("table=" + tables.get(i));
		}
	}

}
