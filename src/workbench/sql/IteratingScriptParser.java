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
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.interfaces.CharacterSequence;
import workbench.util.EncodingUtil;
import workbench.util.FileMappedSequence;
import workbench.util.SqlUtil;
import workbench.util.StringSequence;
import workbench.util.StringUtil;


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
 * @see workbench.util.StringSequences
 *
 * @author  info@sql-workbench.net
 */
public class IteratingScriptParser
{
	private CharacterSequence script;
	private String delimiter = ";";
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
		this.setFile(f, encoding);
	}
	
	/**
	 *	Create a ScriptParser for the given String.
	 *	The delimiter to be used will be evaluated dynamically
	 */
	public IteratingScriptParser(String aScript)
		throws IOException
	{
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
	public void setFile(File f, String enc)
		throws IOException
	{
		this.cleanup();
		// Make sure we have an encoding (otherwise FileMappedSequence will not work!
		if (enc == null) enc = EncodingUtil.getDefaultEncoding();
		this.script = new FileMappedSequence(f, enc);
		this.scriptLength = (int)f.length();
		this.checkEscapedQuotes = false;
		this.reset();
	}

	private void cleanup()
	{
		if (this.script != null) this.script.done();
	}
	
	/**
	 *	Define the script to be parsed
	 */
	public void setScript(String aScript)
	{
		this.cleanup();
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

	public void setDelimiter(String delim)
	{
		if (delim == null)
		{
			this.delimiter = ";";
			this.delimiterLength = 1;
		}
		else
		{
			this.delimiter = delim;
			this.delimiterLength = this.delimiter.length();
		}
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

	public String getDelimiter()
	{
		return this.delimiter;
	}


	// These patterns cover the statements that
	// can be used in a single line without a delimiter
	// This is basically to make the parser as Oracle compatible as possible
	// while not breaking the SQL queries for other servers
	private static final Pattern[] SLC_PATTERNS =
         { Pattern.compile("(?m)^\\s*@.*$"),
					 Pattern.compile("(?mi)^\\s*SET\\s*\\w*\\s*((ON)|(OFF))\\s*;?\\s*$"),
					 Pattern.compile("(?mi)^\\s*ECHO\\s*((ON)|(OFF))\\s*;?\\s*$"),
					 Pattern.compile("(?mi)^\\s*WHENEVER\\s*ERROR\\s*$"),
					 Pattern.compile("(?mi)^\\s*SET\\s*TRANSACTION\\s*READ\\s*((WRITE)|(ONLY))\\s*;?\\s*$")
	       };

	public boolean hasMoreCommands()
	{
		return this.lastPos < this.scriptLength;
	}
	
	/**
	 *	Parse the given SQL Script into a List of single SQL statements.
	 *	Returns the index of the statement indicated by the currentCursorPos
	 */
	public ScriptCommandDefinition getNextCommand()
	{
		int pos;
		String currChar;
		for (pos = this.lastPos; pos < this.scriptLength; pos++)
		{
			currChar = this.script.substring(pos, pos + 1).toUpperCase();
			char firstChar = currChar.charAt(0);

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
					char next = this.script.charAt(pos + 1);

					if (firstChar == '/' && next == '*')
					{
						blockComment = true;
						singleLineComment = false;
						commentOn = true;
						//pos ++; // ignore the next character
					}
					else if (startOfLine && (firstChar == '#' || (firstChar == '-' && next == '-')))
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
						if (firstChar == '\r' || firstChar == '\n')
						{
							singleLineComment = false;
							blockComment = false;
							commentOn = false;
							startOfLine = true;
							lastNewLineStart = pos;
							
							// don't include the comment in the next command
							lastPos = pos + 1;
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

				if ((currChar.equals(this.delimiter) || (pos == scriptLength)))
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
					// check for single line commands...
					if (firstChar == '\r' || firstChar == '\n' )
					{
						String line = this.script.substring(lastNewLineStart, pos).trim();
						String clean = SqlUtil.makeCleanSql(line, false, false, '\'');
						
						boolean slcFound = false;
						
						int commandStart = lastNewLineStart;
						int commandEnd = pos;
						int newEndPos = lastNewLineStart + line.length();
						
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
						}
						
						if (slcFound)
						{
							lastPos = pos;
							this.lastCommandEnd = commandEnd + 1;
							return createCommand(commandStart, commandEnd);
						}
						continue;
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
			if (value.endsWith(this.delimiter))
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
		
		value = this.script.substring(startPos, endPos).trim();
		if (value.length() == 0) return null;
		
		int offset = this.getRealStartOffset(value);
		if (offset > 0) value = value.substring(offset);

		ScriptCommandDefinition c = new ScriptCommandDefinition(value, startPos, endPos);
		
		return c;
	}

	/**
	 *	Check for the real beginning of the statement identified by
	 *	startPos/endPos. This method will return the actual start of the
	 *	command with leading comments trimmed
	 */
	private int getRealStartOffset(String sql)
	{
		int len = sql.length();
		int pos = 0;
		
		boolean inComment = false;
		boolean inQuotes = false;
		char last = 0;
		
		for (int i=0; i < len - 1; i++)
		{
			char c = sql.charAt(i);
			inQuotes = c == '\'';
			if (inQuotes) continue;
			//if (Character.isWhitespace(c)) continue;

			if ( c == '/' && sql.charAt(i+1) == '*')
			{
				inComment = true;
				// skip the start at the next position
				i++;
				last = '*';
				continue;
			}
			
			if (c == '-' && sql.charAt(i+1) == '-')
			{
				i+= 2;
				c = sql.charAt(i);
				// ignore rest of line for -- style comments
				while (c != '\n' && c != '\r' && i < len - 1)
				{
					i++;
					c = sql.charAt(i);
				}
				while (i < len -1  && Character.isWhitespace(sql.charAt(i+1)))
				{
					i++;
				}
				continue;
			}			
			
			if (inComment && c == '*' && sql.charAt(i+1) == '/')
			{
				inComment = false;
				i += 2;
				while (i < len - 1 && Character.isWhitespace(sql.charAt(i)))
				{
					i++;
				}
			}
			
			if (!inComment)
			{
				pos = i;
				break;
			}
		}
		
		return pos;
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
