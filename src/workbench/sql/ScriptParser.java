/*
 * ScriptParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import workbench.resource.Settings;

import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;


/**
 * A class to parse a SQL script and return the individual commands
 * in the script. The actual parsing is done by using an instance
 * of {@link IteratingScriptParser} or {@link LexerBasedParser}
 *
 * @see IteratingScriptParser
 * @see LexerBasedParser
 * @see ScriptIterator
 *
 * @author  Thomas Kellerer
 */
public class ScriptParser
{
	private String originalScript;
	private List<ScriptCommandDefinition> commands;
	private DelimiterDefinition delimiter = DelimiterDefinition.STANDARD_DELIMITER;
	private DelimiterDefinition alternateDelimiter;
	private int currentIteratorIndex = -42;
	private boolean checkEscapedQuotes;
	private ScriptIterator scriptIterator;
	private boolean emptyLineIsSeparator;
	private boolean supportOracleInclude = true;
	private boolean checkSingleLineCommands;
	private boolean supportIdioticQuotes;
	private boolean returnTrailingWhitesapce;
	private String alternateLineComment;
	private boolean useAlternateDelimiter;
	private ParserType parserType;
	private File source;

	private int maxFileSize;

	public ScriptParser()
	{
		this(Settings.getInstance().getInMemoryScriptSizeThreshold());
	}

	public ScriptParser(ParserType type)
	{
		this(Settings.getInstance().getInMemoryScriptSizeThreshold());
		parserType = type;
	}


	public ScriptParser(String aScript, ParserType type)
	{
		this(Settings.getInstance().getInMemoryScriptSizeThreshold());
		parserType = type;
		this.setScript(aScript);
	}

	public ScriptParser(String aScript)
	{
		this(Settings.getInstance().getInMemoryScriptSizeThreshold());
		this.setScript(aScript);
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
		this(Settings.getInstance().getInMemoryScriptSizeThreshold());
		setFile(f, encoding);
	}

	public void setFile(File f)
		throws IOException
	{
		setFile(f, null);
	}

	public void setParserType(ParserType type)
	{
		this.parserType = type;
	}

	/**
	 * Define the source file for this ScriptParser.
	 * Depending on the size the file might be read into memory or not
	 */
	public final void setFile(File f, String encoding)
		throws IOException
	{
		if (!f.exists()) throw new FileNotFoundException(f.getName() + " not found");

		if (f.length() < this.maxFileSize)
		{
			this.readScriptFromFile(f, encoding);
		}
		else
		{
			scriptIterator = getParserInstance();
			scriptIterator.setFile(f, encoding);
		}
		this.source = f;
	}

	/**
	 * Returns the file that was parsed if available.
	 * May be null (if the parser has been initialized from a String)
	 */
	public WbFile getScriptFile()
	{
		if (this.scriptIterator == null || source == null) return null;
		return new WbFile(this.source);
	}

	public int getScriptLength()
	{
		if (this.scriptIterator != null)
		{
			return this.scriptIterator.getScriptLength();
		}
		if (this.originalScript != null)
		{
			return this.originalScript.length();
		}
		return 0;
	}

	/**
	 * Reads the script from the given file using the provided encoding.
	 *
	 * @param f the file to be read
	 * @param encoding the encoding to be use. May be null
	 * @throws IOException
	 */
	public void readScriptFromFile(File f, String encoding)
		throws IOException
	{
		String content = FileUtil.readFile(f, encoding);
		this.setScript(content == null ? "" : content);
	}

	public void setEmptyLineIsDelimiter(boolean flag)
	{
		this.emptyLineIsSeparator = flag;
	}

	public void setAlternateLineComment(String comment)
	{
		if (StringUtil.isNonBlank(comment) && !comment.trim().equals("--"))
		{
			this.alternateLineComment = comment.trim();
		}
		else
		{
			this.alternateLineComment = null;
		}
	}

	public void setReturnStartingWhitespace(boolean flag)
	{
		this.returnTrailingWhitesapce = flag;
	}

	public void setCheckForSingleLineCommands(boolean flag)
	{
		this.checkSingleLineCommands = flag;
	}

	public void setSupportOracleInclude(boolean flag)
	{
		this.supportOracleInclude = flag;
	}

	public void setSupportIdioticQuotes(boolean flag)
	{
		this.supportIdioticQuotes = flag;
	}

	/**
	 *	Define the script to be parsed.
	 *	The delimiter to be used will be checked automatically
	 *	First the it will check if the script ends with the alternate delimiter
	 *	if this is not the case, the script will be checked if it ends with GO
	 *	If so, GO will be used (MS SQL Server script style)
	 *	If none of the above is true, ; (semicolon) will be used
	 */
	public final void setScript(String script)
	{
		if (script == null) throw new NullPointerException("SQL script may not be null");
		if (script.equals(this.originalScript)) return;
		this.originalScript = script;
		this.commands = null;
		this.scriptIterator = null;
		this.source = null;
	}

	public void setDelimiter(DelimiterDefinition delim)
	{
		this.setDelimiters(delim, null);
	}

	/**
	 * Sets the alternate delimiter. This implies that
	 * by default the semicolon is used, and only if
	 * the alternate delimiter is detected, that will be used.
	 *
	 * If only one delimiter should be used (and no automatic checking
	 * for an alternate delimiter), use {@link #setDelimiter(DelimiterDefinition)}
	 */
	public void setAlternateDelimiter(DelimiterDefinition alt)
	{
		setDelimiters(DelimiterDefinition.STANDARD_DELIMITER, alt);
	}

	/**
	 * Define the delimiters to be used. If the (in-memory) script ends with
	 * the defined alternate delimiter, then the alternate is used, otherwise
	 * the default
	 */
	public void setDelimiters(DelimiterDefinition defaultDelim, DelimiterDefinition alternateDelim)
	{
		this.delimiter = defaultDelim;
		if (alternateDelimiterChanged(alternateDelim))
		{
			this.commands = null;
			this.scriptIterator = null;
		}
		this.alternateDelimiter = alternateDelim;
	}

	private boolean alternateDelimiterChanged(DelimiterDefinition newDelim)
	{
		if (this.alternateDelimiter == null && newDelim == null) return false;
		if (this.alternateDelimiter == null && newDelim != null) return true;
		if (this.alternateDelimiter != null && newDelim == null) return true;
		return !alternateDelimiter.equals(newDelim);
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
		if (this.alternateDelimiter == null) return;
		if (this.originalScript == null) return;

		boolean isMySQL = "#".equals(this.alternateLineComment);
		useAlternateDelimiter = alternateDelimiter.terminatesScript(originalScript, isMySQL);
		this.commands = null;
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
		if (this.commands == null) this.parseCommands();
		if (commandIndex < 0 || commandIndex >= this.commands.size()) return -1;
		ScriptCommandDefinition b = this.commands.get(commandIndex);
		int start = b.getStartPositionInScript();
		int end = b.getEndPositionInScript();
		int relativePos = (cursorPos - start);
		int commandLength = (end - start);
		if (relativePos > commandLength)
		{
			// This can happen when trimming the statements.
			relativePos = commandLength;
		}
		return relativePos;
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
			ScriptCommandDefinition b = this.commands.get(i);
			ScriptCommandDefinition next = this.commands.get(i + 1);
			if (b.getWhitespaceStart() <= cursorPos && b.getEndPositionInScript() >= cursorPos) return i;
			if (cursorPos > b.getEndPositionInScript() && cursorPos < next.getEndPositionInScript()) return i + 1;
			if (b.getEndPositionInScript() > cursorPos && next.getWhitespaceStart() <= cursorPos) return i+1;
		}
		ScriptCommandDefinition b = this.commands.get(count - 1);
		if (b.getWhitespaceStart() <= cursorPos && b.getEndPositionInScript() >= cursorPos) return count - 1;
		return -1;
	}

	/**
	 *	Get the starting offset in the original script for the command indicated by index
	 */
	public int getStartPosForCommand(int index)
	{
		if (this.commands == null) this.parseCommands();
		if (index < 0 || index >= this.commands.size()) return -1;
		ScriptCommandDefinition b = this.commands.get(index);
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
		ScriptCommandDefinition b = this.commands.get(index);
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
		return getCommand(index, true);
	}

	public DelimiterDefinition getDelimiterUsed(int commandIndex)
	{
		ScriptCommandDefinition c = getCommandDefinition(commandIndex);
		if (c != null) return c.getDelimiterUsed();
		return getDelimiter();
	}

	/**
	 * Return the command at the given index position.
	 * <br/>
	 * This will force a complete parsing of the script
	 * and the script will be loaded into memory!
	 */
	public String getCommand(int index, boolean rightTrimCommand)
	{
		ScriptCommandDefinition c = getCommandDefinition(index);
		String s = originalScript.substring(c.getStartPositionInScript(), c.getEndPositionInScript());

		if (rightTrimCommand)
		{
			return StringUtil.rtrim(s);
		}
		else
		{
			return s;
		}
	}

	private ScriptCommandDefinition getCommandDefinition(int index)
	{
		if (this.commands == null) this.parseCommands();
		if (index < 0 || index >= this.commands.size()) return null;

		return this.commands.get(index);
	}

	/**
	 * Returns the number of statements in this script.
	 * <br/>
	 * This will force a complete parsing of the script and the
	 * script will be loaded into memory!
	 *
	 * @see #getStatementCount()
	 */
	public int getSize()
	{
		if (this.commands == null) this.parseCommands();
		return this.commands.size();
	}

	public void startIterator()
	{
		this.currentIteratorIndex = 0;
		if (this.scriptIterator == null && this.commands == null)
		{
			this.parseCommands();
		}
		else if (this.scriptIterator != null)
		{
			this.scriptIterator.reset();
		}
	}

	public void done()
	{
		if (this.scriptIterator != null)
		{
			this.scriptIterator.done();
		}
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
	}

	public DelimiterDefinition getDelimiter()
	{
		if (this.useAlternateDelimiter) return this.alternateDelimiter;
		return this.delimiter;
	}

	private ScriptIterator getParserInstance()
	{
		ScriptIterator p = null;
		boolean useOldParser = Settings.getInstance().getBoolProperty("workbench.sql.use.oldparser", true);

		if (parserType == ParserType.Postgres || parserType == ParserType.SqlServer || parserType == ParserType.Oracle)
		{
			LexerBasedParser l = new LexerBasedParser(parserType);
			p = l;
		}
		else if (useOldParser || checkEscapedQuotes || alternateLineComment != null || checkSingleLineCommands || supportIdioticQuotes)
		{
			p = new IteratingScriptParser();
			((IteratingScriptParser)p).setSupportIdioticQuotes(supportIdioticQuotes);
		}
		else
		{
			p = new LexerBasedParser();
		}
		p.setSupportOracleInclude(this.supportOracleInclude);
		p.setEmptyLineIsDelimiter(this.emptyLineIsSeparator && !useAlternateDelimiter);
		p.setCheckEscapedQuotes(this.checkEscapedQuotes);
		if (p.supportsMixedDelimiter())
		{
			p.setDelimiter(delimiter);
			p.setAlternateDelimiter(this.alternateDelimiter);
		}
		else
		{
			findDelimiterToUse();
			p.setDelimiter(useAlternateDelimiter ? this.alternateDelimiter : this.delimiter);
			p.setAlternateDelimiter(null);
		}

		p.setReturnStartingWhitespace(this.returnTrailingWhitesapce);
		p.setAlternateLineComment(this.alternateLineComment);

		if (useAlternateDelimiter || this.delimiter.isNonStandard())
		{
			p.setCheckForSingleLineCommands(false);
		}
		else
		{
			p.setCheckForSingleLineCommands(this.checkSingleLineCommands);
		}
		return p;
	}

	/**
	 *	Parse the given SQL Script into a List of single SQL statements.
	 */
	private void parseCommands()
	{
		ScriptIterator p = getParserInstance();
		commands = new ArrayList<>();
		p.setScript(this.originalScript);
		p.setStoreStatementText(false); // no need to store the statements twice

		ScriptCommandDefinition c = null;
		int index = 0;

		while ((c = p.getNextCommand()) != null)
		{
			c.setIndexInScript(index);
			index++;
			this.commands.add(c);
		}
		currentIteratorIndex = 0;
	}

	/**
	 * Returns the number of statements in the script in case the script was loaded into memory.
	 *
	 * @return  the number of statements or -1 if the script is not loaded into memory
	 * @see #getSize()
	 */
	public int getStatementCount()
	{
		if (this.scriptIterator != null)
		{
			return -1;
		}
		if (commands == null) parseCommands();
		return this.commands.size();
	}

	/**
	 *	Check if more commands are present.
	 */
	public boolean hasNext()
	{
		if (this.scriptIterator != null)
		{
			return this.scriptIterator.hasMoreCommands();
		}
		else
		{
			if (commands == null) parseCommands();
			return this.currentIteratorIndex < this.commands.size();
		}
	}

	/**
	 * Return the next {@link ScriptCommandDefinition} from the script.
	 *
	 */
	public String getNextCommand()
	{
		ScriptCommandDefinition command = null;
		String result = null;
		if (this.scriptIterator != null)
		{
			command = this.scriptIterator.getNextCommand();
			if (command == null) return null;
			result = command.getSQL();
		}
		else
		{
			if (commands == null) parseCommands();
			if (currentIteratorIndex < commands.size())
			{
				command = this.commands.get(this.currentIteratorIndex);
				result = this.originalScript.substring(command.getStartPositionInScript(), command.getEndPositionInScript());
				this.currentIteratorIndex ++;
			}
		}
		return result;
	}

}
