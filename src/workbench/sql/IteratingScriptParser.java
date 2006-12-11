/*
 * IteratingScriptParser.java
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

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.interfaces.CharacterSequence;
import workbench.util.EncodingUtil;
import workbench.util.FileMappedSequence;
import workbench.util.SqlUtil;
import workbench.util.StringSequence;

/**
 * A class to parse a script with SQL commands. Access to the commands
 * is given through an Iterator. If a file is set as the source for
 * this parser, then the file will not be read into memory. A 
 * {@link workbench.util.FileMappedSequence} will be used to process
 * the file. If the script is defined through a String, then 
 * a {@link workbench.util.StringSequence} is used to process the Script
 *
 * @see workbench.interfaces.CharacterSequence
 * @see workbench.util.FileMappedSequence
 * @see workbench.util.StringSequence
 *
 * @author  support@sql-workbench.net
 */

public class IteratingScriptParser
{
	private CharacterSequence script;
	private DelimiterDefinition delimiter = DelimiterDefinition.STANDARD_DELIMITER;
	private int delimiterLength = 1;
	private int scriptLength = -1;
	private int lastPos = 0;
	private int lastCommandEnd = -1;
	private boolean quoteOn = false;
	private boolean commentOn = false;
	private boolean blockComment = false;
	private boolean singleLineComment = false;
	private boolean startOfLine = true;
	private int lastNewLineStart = 0;
	private char lastQuote = 0;
	private boolean checkEscapedQuotes = true;
	private boolean emptyLineIsSeparator = false;
	private boolean supportOracleInclude = true;
	private boolean checkSingleLineCommands = true;
	private boolean storeSqlInCommands = false;
	private boolean returnStartingWhitespace = false;
	private boolean checkHashComment = false;
	
	// These patterns cover the statements that
	// can be used in a single line without a delimiter
	// This is basically to make the parser as Oracle compatible as possible
	// while not breaking the SQL queries for other servers
	private Pattern[] SLC_PATTERNS =
         { Pattern.compile("(?mi)^\\s*SET\\s*\\w*\\s*(ON|OFF)\\s*;?\\s*$"),
					 Pattern.compile("(?mi)^\\s*ECHO\\s*((ON)|(OFF))\\s*;?\\s*$"),
					 Pattern.compile("(?mi)^\\s*WHENEVER\\s*ERROR\\s*$"),
					 Pattern.compile("(?mi)^\\s*SET\\s*TRANSACTION\\s*READ\\s*((WRITE)|(ONLY))\\s*;?\\s*$")
	       };

	private Pattern ORA_INCLUDE_PATTERN = Pattern.compile("(?m)^\\s*@.*$");
		
	/** Create an InteratingScriptParser
	 */
	public IteratingScriptParser()
	{
	}
	

	/**
	 * Initialize a ScriptParser from a file with a given encoding.
	 * @see #setFile(File, String)
	 */
	public IteratingScriptParser(File f, String encoding)
		throws IOException
	{
		this();
		this.setFile(f, encoding);
	}
	
	/**
	 *	Create a ScriptParser for the given String.
	 *	The delimiter to be used will be evaluated dynamically
	 */
	public IteratingScriptParser(String aScript)
		throws IOException
	{
		this();
		if (aScript == null) throw new IllegalArgumentException("Script may not be null");
		this.setScript(aScript);
	}

	/**
	 * Define the source file for the script using the default encoding.
	 * @see #setFile(File, String)
	 * @see workbench.util.EncodingUtil#getDefaultEncoding()
	 */
	public void setFile(File f)
		throws IOException
	{
		this.setFile(f, EncodingUtil.getDefaultEncoding());
	}
	
	/**
	 * Define the source file to be used and the encoding of the file.
	 * If the encoding is null, the default encoding will be used.
	 * @see #setFile(File, String)
	 * @see workbench.util.EncodingUtil#getDefaultEncoding()
	 */
	public final void setFile(File f, String enc)
		throws IOException
	{
		this.cleanup();
		// Make sure we have an encoding (otherwise FileMappedSequence will not work!
		if (enc == null) enc = EncodingUtil.getDefaultEncoding();
		this.script = new FileMappedSequence(f, enc);
		this.scriptLength = (int)f.length();
		this.checkEscapedQuotes = false;
		this.storeSqlInCommands = true;
		this.reset();
	}

	/**
	 * Should the parser check for MySQL hash comments? 
	 */
	public void setCheckForHashComments(boolean flag)
	{
		this.checkHashComment = flag;
	}
	
	public void setCheckForSingleLineCommands(boolean flag)
	{
		this.checkSingleLineCommands = flag;
	}

	public void setReturnStartingWhitespace(boolean flag) 
	{ 
		this.returnStartingWhitespace = flag;
	}
	
	/**
	 * Support Oracle style @ includes
	 */
	public void setSupportOracleInclude(boolean flag)
	{
		this.supportOracleInclude = flag;
	}
	
	public void allowEmptyLineAsSeparator(boolean flag)
	{
		this.emptyLineIsSeparator = flag;
	}
	
	private void cleanup()
	{
		if (this.script != null) this.script.done();
	}
	
	/**
	 *	Define the script to be parsed
	 */
	public final void setScript(String aScript)
	{
		this.cleanup();
		this.storeSqlInCommands = false;
		this.script = new StringSequence(aScript);
		this.scriptLength = aScript.length();
		this.checkEscapedQuotes = false;
		this.reset();
	}
	
	private void reset()
	{
		lastCommandEnd = 0;
		lastPos = 0;
		quoteOn = false;
		commentOn = false;
		blockComment = false;
		singleLineComment = false;
		startOfLine = true;
		lastNewLineStart = 0;
		lastQuote = 0;
	}

	public void setDelimiter(DelimiterDefinition delim)
	{
		if (delim == null)
		{
			this.delimiter = DelimiterDefinition.STANDARD_DELIMITER;
		}
		else
		{
			this.delimiter = delim;
		}
		this.delimiterLength = this.delimiter.getDelimiter().length();
	}

	public int getScriptLength()
	{
		return this.scriptLength;
	}
	
	public int findNextLineStart(int pos)
	{
		if (pos < 0) return pos;
		
		if (pos >= this.scriptLength) return pos;
		char c = this.script.charAt(pos);
		while (pos < this.scriptLength && (c == '\n' || c == '\r'))
		{
			pos ++;
			c = script.charAt(pos);
		}
		return pos;
	}

	public boolean hasMoreCommands()
	{
		if (lastPos < this.scriptLength)
		{
			int nextPos = findNextNonWhiteSpace(lastPos);
			return nextPos < scriptLength;
		}
		return false;
	}
	
	
	private int findNextNonWhiteSpace(int start)
	{
		char ch = this.script.charAt(start);
		while (start < this.scriptLength && Character.isWhitespace(ch))
		{
			start ++;
			if (start < this.scriptLength) ch = this.script.charAt(start);
		}
		return start;
	}
	
	/**
	 *	Parse the given SQL Script into a List of single SQL statements.
	 *	Returns the index of the statement indicated by the currentCursorPos
	 */
	public ScriptCommandDefinition getNextCommand()
	{
		int pos;
		String currChar;
		boolean delimiterOnOwnLine = this.delimiter.isSingleLine();
		String delim = this.delimiter.getDelimiter();
		
		for (pos = this.lastPos; pos < this.scriptLength; pos++)
		{
			currChar = this.script.substring(pos, pos + 1).toUpperCase();
			char firstChar = currChar.charAt(0);
			
			// skip CR characters
			if (firstChar == '\r') continue;
			
			char nextChar = (pos < scriptLength - 1 ? this.script.charAt(pos + 1) : 0);
			
			// ignore quotes in comments
			if (!commentOn && (firstChar == '\'' || firstChar == '"'))
			{
				if (!quoteOn)
				{
					lastQuote = firstChar;
					quoteOn = true;
				}
				else if (firstChar == lastQuote)
				{
					if (pos > 1)
					{
						// check if the current quote char was escaped
						if (!this.checkEscapedQuotes || this.script.charAt(pos - 1) != '\\')
						{
							lastQuote = 0;
							quoteOn = false;
						}
					}
					else
					{
						lastQuote = 0;
						quoteOn = false;
					}
				}
			}

			if (quoteOn) continue;

			// now check for comment start
			if (!quoteOn && pos < scriptLength - 1)
			{
				if (!commentOn)
				{
					if (firstChar == '/' && nextChar == '*')
					{
						blockComment = true;
						singleLineComment = false;
						commentOn = true;
						//pos ++; // ignore the next character
					}
					else if ((startOfLine && firstChar == '#' && checkHashComment) || (firstChar == '-' && nextChar == '-'))
					{
						singleLineComment = true;
						blockComment = false;
						commentOn = true;
					}
				}
				else
				{
					if (singleLineComment)
					{
						if (firstChar == '\n')
						{
							singleLineComment = false;
							blockComment = false;
							commentOn = false;
							startOfLine = true;
							lastNewLineStart = pos;
							continue;
						}
					}
					else if (blockComment)
					{
						char last = this.script.charAt(pos - 1);
						if (firstChar == '/' && last == '*')
						{
							blockComment = false;
							singleLineComment = false;
							commentOn = false;
							continue;
						}
					}
				}
			}

 			if (!quoteOn && !commentOn)
			{
				if (this.delimiterLength > 1 && pos + this.delimiterLength < scriptLength)
				{
					currChar = this.script.substring(pos, pos + this.delimiterLength).toUpperCase();
				}

				if (!delimiterOnOwnLine && (currChar.equals(delim) || (pos == scriptLength)))
				{
					if (lastPos >= pos && pos < scriptLength - 1) 
					{
						lastPos ++;
						continue;
					}
					startOfLine = true;
					this.lastNewLineStart = pos + 1;
					this.lastPos = pos + this.delimiterLength;
					int start = lastCommandEnd;
					this.lastCommandEnd = lastPos;
					ScriptCommandDefinition c = this.createCommand(start, pos);
					if (c == null) continue;
					return c;
				}
				else
				{
					if (firstChar == '\n')
					{
						String line = this.script.substring(lastNewLineStart, pos).trim();
						String clean = SqlUtil.makeCleanSql(line, false, false, '\'');
						
						if ( (this.emptyLineIsSeparator && clean.length() == 0) ||
							   (delimiterOnOwnLine && line.equalsIgnoreCase(delim)) )
						{
							int end = pos;
							
							if (clean.length() > 0)
							{
								// a single line delimiter was found, we have to make 
								// sure this is not added to the created command
								end = lastNewLineStart;
							}
							int start = lastCommandEnd;
							ScriptCommandDefinition c = this.createCommand(start, end);
							if (c != null) 
							{
								startOfLine = true;
								this.lastNewLineStart = pos + 1;
								this.lastPos = pos + this.delimiterLength;
								this.lastCommandEnd = lastPos;
								return c;
							}
						}
						
						if (this.checkSingleLineCommands)
						{
							boolean slcFound = false;

							int commandStart = lastNewLineStart;
							int commandEnd = pos;

							lastNewLineStart = pos;
							startOfLine = true;

							if (clean.length() > 0 )
							{
								for (int pi=0; pi < SLC_PATTERNS.length; pi++)
								{
									Matcher m = SLC_PATTERNS[pi].matcher(clean);

									if (m.matches())
									{
										slcFound = true;
										break;
									}
								}
								if (!slcFound && this.supportOracleInclude)
								{
									Matcher m = ORA_INCLUDE_PATTERN.matcher(clean);
									if (m.matches())
									{
										slcFound = true;
									}
								}
							}

							if (slcFound)
							{
								lastPos = pos;
								this.lastCommandEnd = commandEnd + 1;
								return createCommand(commandStart, commandEnd);
							}
							continue;
						}
						lastNewLineStart = pos + 1;
					}
					else
					{
						startOfLine = false;
					}
				}
			}
			
		} // end loop for next statement

		ScriptCommandDefinition c = null;
		if (lastPos < pos && !blockComment && !quoteOn)
		{
			String value = this.script.substring(lastCommandEnd, scriptLength).trim();
			int endpos = scriptLength;
			if (value.endsWith(delim))
			{
				endpos = endpos - this.delimiterLength;
			}
			c = createCommand(lastCommandEnd, endpos);
		}
		this.lastPos = scriptLength;
		return c;
	}

	private ScriptCommandDefinition createCommand(int startPos, int endPos)
	{
		String value = null;

		if (endPos == -1)
		{
			endPos = scriptLength;
		}
		
		// remove whitespaces at the start
		if (!returnStartingWhitespace)
		{
			char ch = this.script.charAt(startPos);
			while (startPos < endPos && Character.isWhitespace(ch))
			{
				startPos ++;
				if (startPos < endPos) ch = this.script.charAt(startPos);
			}
		}
		
		// always clean out trailing whitespace
		char ch = this.script.charAt(endPos - 1);
		while (startPos < endPos && (ch == '\n' || ch == '\r'))
		{
			endPos --;
			if (startPos < endPos) ch = this.script.charAt(endPos - 1);
		}
		
		if (startPos >= endPos) return null;
		if (storeSqlInCommands)
		{
			value = this.script.substring(startPos, endPos);
		}
		ScriptCommandDefinition c = new ScriptCommandDefinition(value, startPos, endPos);
		
		return c;
	}

	public void setCheckEscapedQuotes(boolean flag)
	{
		this.checkEscapedQuotes = flag;
	}

	public void done()
	{
		this.script.done();
	}

}
