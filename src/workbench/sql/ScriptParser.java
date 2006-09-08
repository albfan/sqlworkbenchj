/*
 * ScriptParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.EncodingUtil;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;


/**
 * A class to parse a SQL script and return the individual commands
 * in the script. The actual parsing is done by using an instance
 * of {@link IteratingScriptParser}
 *
 * @author  support@sql-workbench.net
 */
public class ScriptParser
	implements Iterator
{

	private String originalScript = null;
	private ArrayList commands = null;
	private String delimiter = ";";
	private String alternateDelimiter;
	private int currentIteratorIndex = -42;
	private boolean checkEscapedQuotes = true;
	private IteratingScriptParser iteratingParser = null;
	private boolean emptyLineIsSeparator = false;
	private boolean supportOracleInclude = true;
	private boolean checkSingleLineCommands = true;
	private int maxFileSize;
	
	public ScriptParser()
	{
		this(Settings.getInstance().getInMemoryScriptSizeThreshold());
	}
	/** Create a ScriptParser
	 *
	 *	The actual script needs to be specified with setScript()
	 *  The delimiter will be evaluated dynamically
	 */
	public ScriptParser(int fileSize)
	{
		maxFileSize = fileSize;
	}

	/**
	 *	Initialize a ScriptParser from a file.
	 *	The delimiter will be evaluated dynamically
	 */
	public ScriptParser(File f)
		throws IOException
	{
		this(f, null);
	}
	
	/**
	 *	Initialize a ScriptParser from a file.
	 *	The delimiter will be evaluated dynamically
	 */
	public ScriptParser(File f, String encoding)
		throws IOException
	{
		setFile(f, encoding);
	}

	public void setFile(File f)
		throws IOException
	{
		setFile(f, null);
	}
	
	/**
	 * Define the source file for this ScriptParser.
	 * Depending on the size the file might be read into memory or not
	 */
	public void setFile(File f, String encoding)
		throws IOException
	{
		if (!f.exists()) throw new FileNotFoundException(f.getName() + " not found");
		
		if (f.length() < this.maxFileSize)
		{
			this.readScriptFromFile(f, encoding);
			this.findDelimiterToUse();
		}
		else
		{
			this.iteratingParser = new IteratingScriptParser(f, encoding);
			configureParserInstance(this.iteratingParser);
		}
	}
	
	public void readScriptFromFile(File f)
		throws IOException
	{
		this.readScriptFromFile(f, null);
	}
	
	public void readScriptFromFile(File f, String encoding)
		throws IOException
	{
		BufferedReader in = null;
		StringBuffer content = null;
		try
		{
			content = new StringBuffer((int)f.length());
			in = EncodingUtil.createBufferedReader(f, encoding);
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
			content = new StringBuffer(0);
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

	public void allowEmptyLineAsSeparator(boolean flag)
	{
		this.emptyLineIsSeparator = flag;
		if (this.iteratingParser != null)
		{
			this.iteratingParser.allowEmptyLineAsSeparator(this.emptyLineIsSeparator);
		}
	}

	public void setCheckForSingleLineCommands(boolean flag)
	{
		this.checkSingleLineCommands = flag;
		if (this.iteratingParser != null)
		{
			this.iteratingParser.setCheckForSingleLineCommands(flag);
		}
	}
	
	public void setSupportOracleInclude(boolean flag)
	{
		this.supportOracleInclude = flag;
		if (this.iteratingParser != null)
		{
			this.iteratingParser.setSupportOracleInclude(flag);
		}
	}
	
	/**
	 *	Define the script to be parsed.
	 *	The delimiter to be used will be checked automatically
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

	/**
	 * Define the delimiter to be used in case it's not a semicolon
	 */
	public void setDelimiter(String delim)
	{
		this.delimiter = delim;
		if (this.iteratingParser != null) this.iteratingParser.setDelimiter(delim);
	}

	/**
	 *	Try to find out which delimiter should be used for the current script.
	 *	First it will check if the script ends with the alternate delimiter
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
		else if (cleanSql.matches("(?i).*[\\n\\r|\\n]GO$"))
		{
			this.delimiter = "GO";
		}
	}

	/**
	 * Return the index from the overall script mapped to the 
	 * index inside the specified command. For a single command
	 * script scriptCursorLocation will be the same as 
	 * the location inside the dedicated command.
	 * @param commandIndex the index for the command to check
	 * @param cursorPos the index in the overall script
	 * @return the relative index inside the command
	 */
	public int getIndexInCommand(int commandIndex, int cursorPos)
	{
		int start = this.getStartPosForCommand(commandIndex);
		return (cursorPos - start);
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
		if (count == 0) return -1;
		for (int i=0; i < count - 1; i++)
		{
			ScriptCommandDefinition b = (ScriptCommandDefinition)this.commands.get(i);
			ScriptCommandDefinition next = (ScriptCommandDefinition)this.commands.get(i + 1);
			if (b.getStartPositionInScript() <= cursorPos && b.getEndPositionInScript() >= cursorPos) return i;
			if (cursorPos > b.getEndPositionInScript() && cursorPos < next.getEndPositionInScript()) return i + 1;
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
		int start = b.getStartPositionInScript();
		return start;
	}

	/**
	 * Get the starting offset in the original script for the command indicated by index
	 */
	public int getEndPosForCommand(int index)
	{
		if (this.commands == null) this.parseCommands();
		if (index < 0 || index >= this.commands.size()) return -1;
		ScriptCommandDefinition b = (ScriptCommandDefinition)this.commands.get(index);
		return b.getEndPositionInScript();
	}

	/**
	 * Find the position in the original script for the next start of line
	 */
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

	/**
	 * Return the command at the given index position.
	 */
	public String getCommand(int index)
	{
		if (this.commands == null) this.parseCommands();
		if (index < 0 || index >= this.commands.size()) return null;
		ScriptCommandDefinition c = (ScriptCommandDefinition)this.commands.get(index);
		return originalScript.substring(c.getStartPositionInScript(), c.getEndPositionInScript());
	}

	public int getSize() 
	{
		if (this.commands == null) this.parseCommands();
		return this.commands.size();
	}

	/**
	 * Return an Iterator which allows to iterate over 
	 * the commands from the script. The Iterator
	 * will return objects of type {@link ScriptCommandDefinition}
	 */
	public Iterator getIterator()
	{
		this.currentIteratorIndex = 0;
		if (this.iteratingParser == null && this.commands == null)
		{
			this.parseCommands();
		}
		return this;
	}
	
	public void done()
	{
		if (this.iteratingParser != null)
		{
			this.iteratingParser.done();
		}
		this.currentIteratorIndex = -42;
	}

	/**
	 * Check for quote characters that are escaped using a 
	 * backslash. If turned on (flag == true) the following
	 * SQL statement would be valid (different to the SQL standard):
	 * <pre>INSERT INTO myTable (column1) VALUES ('Arthurs\'s house');</pre>
	 * but the following Script would generate an error: 
	 * <pre>INSERT INTO myTable (file_path) VALUES ('c:\');</pre>
	 * because the last quote would not bee seen as a closing quote
	 */
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

	private void configureParserInstance(IteratingScriptParser p)
	{
		p.setSupportOracleInclude(this.supportOracleInclude);
		p.setCheckForSingleLineCommands(this.checkSingleLineCommands);
		p.allowEmptyLineAsSeparator(this.emptyLineIsSeparator);
		p.setCheckEscapedQuotes(this.checkEscapedQuotes);
		p.setDelimiter(this.delimiter);
	}
	
	/**
	 *	Parse the given SQL Script into a List of single SQL statements.
	 */
	private void parseCommands()
	{
		this.commands = new ArrayList();
		IteratingScriptParser p = new IteratingScriptParser();
		configureParserInstance(p);
		p.setScript(this.originalScript);

		ScriptCommandDefinition c = null; 
		int index = 0;
		
		while ((c = p.getNextCommand()) != null)
		{
			c.setIndexInScript(index);
			index++;
			this.commands.add(c);
		}
	}

	/**
	 *	Check if more commands are present. 
	 */
	public boolean hasNext()
	{
		if (this.currentIteratorIndex == -42) throw new IllegalStateException("Iterator not initialized");
		if (this.iteratingParser != null)
		{
			return this.iteratingParser.hasMoreCommands();
		}
		else
		{
			return this.currentIteratorIndex < this.commands.size();
		}
	}

	/**
	 * Return the next SQL command from the script. 
	 * This is delegated to {@link #getNextCommand()}
	 * @return a String object representing the SQL command
	 * @throws IllegalStateException if the Iterator has not been initialized using {@link #getIterator()}
	 * @see IteratingScriptParser#getNextCommand()
	 * @see #getNextCommand()
	 */
	public Object next()
		throws NoSuchElementException
	{
		if (this.currentIteratorIndex == -42) throw new NoSuchElementException("Iterator not initialized");
		return getNextCommand();
	}

	/**
	 * Return the next {@link ScriptCommandDefinition} from the script. 
	 * This is delegated to {@link #getNextCommand()}
	 * @throws IllegalStateException if the Iterator has not been initialized using {@link #getIterator()}
	 * @see IteratingScriptParser#getNextCommand()
	 * @see #next()
	 */
	public String getNextCommand()
		throws NoSuchElementException
	{
		if (this.currentIteratorIndex == -42) throw new NoSuchElementException("Iterator not initialized");
		ScriptCommandDefinition command = null;
		String result = null;
		if (this.iteratingParser != null)
		{
			command = this.iteratingParser.getNextCommand();
			if (command == null) return null;
			result = command.getSQL();
		}
		else
		{
			command = (ScriptCommandDefinition)this.commands.get(this.currentIteratorIndex);
			result = this.originalScript.substring(command.getStartPositionInScript(), command.getEndPositionInScript());
			this.currentIteratorIndex ++;
		}

		return result;
	}

	/**
	 * Not implemented, as removing commands is not possible.
	 * A call to this method simply does nothing.
	 */
	public void remove()
	{
	}

}
