/*
 * ScriptParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author  info@sql-workbench.net
 */
public class ScriptParser
	implements Iterator
{

	private String originalScript = null;
	private ArrayList commands = null;
	private String delimiter = ";";
	private int delimiterLength = -1;
	private String alternateDelimiter;
	private int currentIteratorIndex = 0;
	private boolean checkEscapedQuotes = true;
	private IteratingScriptParser iteratingParser = null;

	/** Create a ScriptParser
	 *
	 *	The actual script needs to be specified with setScript()
	 *  The delimiter will be evaluated dynamically
	 */
	public ScriptParser()
	{
	}

	/**
	 *	Initialize a ScriptParser from a file.
	 *	The delimiter will be evaluated dynamically
	 */
	public ScriptParser(File f)
		throws IOException
	{
		if (!f.exists()) throw new FileNotFoundException(f.getName() + " not found");

		if (f.length() < Settings.getInstance().getInMemoryScriptSizeThreshold())
		{
			this.readScriptFromFile(f);
			this.findDelimiterToUse();
		}
		else
		{
			this.iteratingParser = new IteratingScriptParser(f);
			this.iteratingParser.setCheckEscapedQuotes(this.checkEscapedQuotes);
		}
	}

	public void readScriptFromFile(String filename)
		throws IOException
	{
		File f = new File(filename);
		this.readScriptFromFile(f);
	}

	public void readScriptFromFile(File f)
		throws IOException
	{
		BufferedReader in = null;
		StrBuffer content = null;
		try
		{
			content = new StrBuffer((int)f.length());
			in = new BufferedReader(new FileReader(f));
			String line = in.readLine();
			while (line != null)
			{
				content.append(line);
				content.append('\n');
				line = in.readLine();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("ScriptParser.readFile()", "Error reading file " + f.getAbsolutePath(), e);
			content = new StrBuffer();
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		this.setScript(content.toString());
	}

	/**
	 *	Create a ScriptParser for the given Script.
	 *	The delimiter to be used will be evaluated dynamically
	 */
	public ScriptParser(String aScript)
	{
		this.setScript(aScript);
	}

	/**
	 *	Define the script to be parsed.
	 *	If delim == null, it will be evaluated dynamically.
	 *	First the it will check if the script ends with the alternate delimiter
	 *	if this is not the case, the script will be checked if it ends with GO
	 *	If so, GO will be used (MS SQL Server script style)
	 *	If none of the above is true, ; (semicolon) will be used
	 */
	public void setScript(String aScript)
	{
		if (aScript == null) throw new NullPointerException("SQL script may not be null");
		if (aScript.equals(this.originalScript)) return;
		this.originalScript = aScript;
		this.findDelimiterToUse();
		this.commands = null;
		this.iteratingParser = null;
	}

	public void setDelimiter(String delim)
	{
		this.delimiter = delim;
	}

	/**
	 *	Try to find out which delimiter should be used for the current script.
	 *	First the it will check if the script ends with the alternate delimiter
	 *	if this is not the case, the script will be checked if it ends with GO
	 *	If so, GO will be used (MS SQL Server script style)
	 *	If none of the above is true, ; (semicolon) will be used
	 */
	private void findDelimiterToUse()
	{
		this.delimiter = ";";

		String cleanSql = SqlUtil.makeCleanSql(this.originalScript, false).trim();
		if (this.alternateDelimiter == null)
		{
			this.alternateDelimiter = Settings.getInstance().getAlternateDelimiter();
		}
		if (cleanSql.endsWith(this.alternateDelimiter))
		{
			this.delimiter = this.alternateDelimiter;
		}
		else if (cleanSql.toUpperCase().endsWith("GO"))
		{
			this.delimiter = "GO";
		}
		this.delimiterLength = this.delimiter.length();
	}

	/**
	 *	Return the command index for the command which is located at
	 *	the given index of the current script.
	 */
	public int getCommandIndexAtCursorPos(int cursorPos)
	{
		if (this.commands == null) this.parseCommands();
		if (cursorPos < 0) return -1;
		int count = this.commands.size();
		if (count == 1) return 0;
		for (int i=0; i < count - 1; i++)
		{
			ScriptCommandDefinition b = (ScriptCommandDefinition)this.commands.get(i);
			ScriptCommandDefinition next = (ScriptCommandDefinition)this.commands.get(i + 1);
			if (b.getStartPositionInScript() <= cursorPos && b.getEndPositionInScript() >= cursorPos) return i;
			if (b.getEndPositionInScript() > cursorPos && next.getStartPositionInScript() <= cursorPos) return i+1;
		}
		ScriptCommandDefinition b = (ScriptCommandDefinition)this.commands.get(count - 1);
		if (b.getStartPositionInScript() <= cursorPos && b.getEndPositionInScript() >= cursorPos) return count - 1;
		return -1;
	}

	/**
	 *	Get the starting offset in the original script for the command indicated by index
	 */
	public int getStartPosForCommand(int index)
	{
		if (this.commands == null) this.parseCommands();
		if (index < 0 || index >= this.commands.size()) return -1;
		ScriptCommandDefinition b = (ScriptCommandDefinition)this.commands.get(index);
		return b.getStartPositionInScript();
	}

	/**
	 *	Get the starting offset in the original script for the command indicated by index
	 */
	public int getEndPosForCommand(int index)
	{
		if (this.commands == null) this.parseCommands();
		if (index < 0 || index >= this.commands.size()) return -1;
		ScriptCommandDefinition b = (ScriptCommandDefinition)this.commands.get(index);
		return b.getEndPositionInScript();
	}

	public int findNextLineStart(int pos)
	{
		if (this.originalScript == null) return -1;
		if (pos < 0) return pos;
		int len = this.originalScript.length();
		if (pos >= len) return pos;
		char c = this.originalScript.charAt(pos);
		while (pos < len && (c == '\n' || c == '\r'))
		{
			pos ++;
			c = this.originalScript.charAt(pos);
		}
		return pos;
	}

	public String getCommand(int index)
	{
		if (this.commands == null) this.parseCommands();
		if (index < 0 || index >= this.commands.size()) return null;
		ScriptCommandDefinition c = (ScriptCommandDefinition)this.commands.get(index);
		return c.getSQL();
	}

	/**
	 *	Return the list of commands in the current script.
	 *	The list contains elements of <code>String</code>.
	 *	The commands will be returned without the used delimiter
	 */
	public List getCommands()
	{
		if (this.commands == null) this.parseCommands();
		ArrayList result = new ArrayList(this.commands.size());
		for (int i=0; i < this.commands.size(); i++)
		{
			result.add(this.getCommand(i));
		}
		return result;
	}

	public Iterator getIterator()
	{
		this.currentIteratorIndex = 0;
		if (this.iteratingParser == null && this.commands == null)
		{
			this.parseCommands();
		}
		return this;
	}

	public void setCheckEscapedQuotes(boolean flag)
	{
		this.checkEscapedQuotes = flag;
		if (this.iteratingParser != null)
		{
			this.iteratingParser.setCheckEscapedQuotes(flag);
		}
	}

	public void setAlternateDelimiter(String delim)
	{
		this.alternateDelimiter = delim;
	}

	public String getDelimiter()
	{
		return this.delimiter;
	}

	public String getScript()
	{
		return this.originalScript;
	}

	/**
	 *	Parse the given SQL Script into a List of single SQL statements.
	 *	Returns the index of the statement indicated by the currentCursorPos
	 */
	private void parseCommands()
	{
		this.commands = new ArrayList();
		IteratingScriptParser p = new IteratingScriptParser();
		p.setScript(this.originalScript);
		p.setDelimiter(this.delimiter);
		p.setCheckEscapedQuotes(this.checkEscapedQuotes);

		ScriptCommandDefinition c = null; 
		int index = 0;
		
		while ((c = p.getNextCommand()) != null)
		{
			c.setIndexInScript(index);
			index++;
			this.commands.add(c);
		}
	}

	public boolean hasNext()
	{
		if (this.iteratingParser != null)
		{
			return this.iteratingParser.hasMoreCommands();
		}
		else
		{
			return this.currentIteratorIndex < this.commands.size();
		}
	}

	public Object next()
	{
		Object result = null;
		if (this.iteratingParser != null)
		{
			result = this.iteratingParser.getNextCommand();
		}
		else
		{
			result = this.commands.get(this.currentIteratorIndex);
			this.currentIteratorIndex ++;
		}
		return result;
	}

	public void remove()
	{
	}

	public static void main(String args[])
	{
		try
		{
			String sql = "drop index idx_pa_date\n;\n\ncreate index idx_pa_date on partner_pro_pa (start_date, end_date);\n;\n\ncreate or replace view v_active_pa \nas\nSELECT PARTNER_PRO_ID,\n       PURCHASE_AGREE_NO,\n       START_DATE,\n       END_DATE,\n       STATUS\nFROM PARTNER_PRO_PA\nWHERE end_date >= sysdate\nAND   start_date < sysdate\n;\n\n\nselect distinct PURCHASE_AGREE_NO, cac_cd\nfrom PARTNER_PA_CAC\norder by 1\n;\n\nSELECT count(*)\nFROM rpl_user.partner_pro_pa;\n;\n\nselect * from v_active_pa\n;\n";
			
		  ScriptParser p = new ScriptParser();
			
			//p.readScriptFromFile("d:/projects/jworkbench/testdata/statements.sql");
			//p.readScriptFromFile("d:/projects/jworkbench/testdata/BuildDb_test1.sql");
			p.setScript(sql);
			p.setCheckEscapedQuotes(false);
			long start, end;
			start = System.currentTimeMillis();
			List l = p.getCommands();
			end = System.currentTimeMillis();
			System.out.println("time=" + (end - start));
			for (int i=0; i < l.size(); i++)
			{
				System.out.println(l.get(i) + "\n--------------------------");
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}

		System.out.println("*** Done.");
	}
}