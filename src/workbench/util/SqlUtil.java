package workbench.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

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
	
	public static void main(String args[])
	{
		//String script = "select 'testing'';''', test.spalte1 from test;\r\n                ;\r\nupdate test set blba='xx';";
		String script = "select 'testing'';''', test.spalte1 from test;";
		List commands = getCommands(script, ";");
		for (int i=0; i < commands.size(); i++)
		{
			System.out.println(commands.get(i).toString());
			System.out.println("-----");
		}
	}
	
}
