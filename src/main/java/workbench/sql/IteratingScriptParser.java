/*
 * IteratingScriptParser.java
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
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.interfaces.CharacterSequence;
import workbench.resource.Settings;

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
 * @see workbench.util.StringSequence
 *
 * @author  Thomas Kellerer
 */

public class IteratingScriptParser
	implements ScriptIterator
{
	private CharacterSequence script;
	private DelimiterDefinition delimiter = DelimiterDefinition.STANDARD_DELIMITER;
	private int delimiterLength = 1;
	private int scriptLength = -1;
	private int lastPos = 0;
	private int lastCommandEnd = -1;
	private boolean quoteOn;
	private boolean commentOn;
	private boolean blockComment;
	private boolean singleLineComment;
	private int lastNewLineStart = 0;
	private char lastQuote = 0;
	private boolean checkEscapedQuotes = true;
	private boolean emptyLineIsSeparator;
	private boolean supportOracleInclude = true;
	private boolean checkSingleLineCommands = true;
	private boolean storeSqlInCommands;
	private boolean returnStartingWhitespace;
	private String alternateLineComment;

	// Enables the idiotic MS Style quoting using []
	private boolean supportIdioticQuotes;

	// These patterns cover the statements that
	// can be used in a single line without a delimiter
	// This is basically to make the parser as Oracle compatible as possible
	// while not breaking the SQL queries for other servers
	private final Pattern[] SLC_PATTERNS =
         { Pattern.compile("(?mi)^\\s*SET\\s+\\w+\\s+(ON|OFF)\\s*;?\\s*$"),
					 Pattern.compile("(?mi)^\\s*ECHO\\s+((ON)|(OFF))\\s*;?\\s*$"),
					 Pattern.compile("(?mi)^\\s*DECLARE\\s+\\S+.*$"),
					 Pattern.compile("(?mi)^\\s*WHENEVER\\s+ERROR\\s*$"),
					 Pattern.compile("(?mi)^\\s*SET\\s+TRANSACTION\\s+READ\\s+((WRITE)|(ONLY))\\s*;?\\s*$")
	       };

	private final Pattern ORA_INCLUDE_PATTERN = Pattern.compile("(?m)^\\s*@.*$");

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
	 * @see workbench.resource.Settings#getDefaultEncoding()
	 */
	public void setFile(File f)
		throws IOException
	{
		this.setFile(f, Settings.getInstance().getDefaultFileEncoding());
	}

	/**
	 * Define the source file to be used and the encoding of the file.
	 * If the encoding is null, the default encoding will be used.
	 * @see #setFile(File, String)
	 * @see workbench.resource.Settings#getDefaultEncoding()
	 */
	@Override
	public final void setFile(File f, String enc)
		throws IOException
	{
		this.cleanup();
		// Make sure we have an encoding (otherwise FileMappedSequence will not work!
		if (enc == null) enc = Settings.getInstance().getDefaultEncoding();
		this.script = new FileMappedSequence(f, enc);
		this.scriptLength = script.length();
		this.checkEscapedQuotes = false;
		this.storeSqlInCommands = true;
		this.reset();
	}

	@Override
	public void setStoreStatementText(boolean flag)
	{
		storeSqlInCommands = flag;
	}

	public void setSupportIdioticQuotes(boolean flag)
	{
		supportIdioticQuotes = flag;
	}
	/**
	 * Should the parser check for MySQL hash comments?
	 */
	@Override
	public void setAlternateLineComment(String comment)
	{
		this.alternateLineComment = (StringUtil.isBlank(comment) ? null : comment.trim());
	}

	@Override
	public void setCheckForSingleLineCommands(boolean flag)
	{
		this.checkSingleLineCommands = flag;
	}

	@Override
	public void setReturnStartingWhitespace(boolean flag)
	{
		this.returnStartingWhitespace = flag;
	}

	/**
	 * Support Oracle style @ includes
	 */
	@Override
	public void setSupportOracleInclude(boolean flag)
	{
		this.supportOracleInclude = flag;
	}

	@Override
	public void setEmptyLineIsDelimiter(boolean flag)
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
	@Override
	public final void setScript(String aScript)
	{
		this.cleanup();
		this.storeSqlInCommands = false;
		this.script = new StringSequence(aScript);
		this.scriptLength = aScript.length();
		this.reset();
	}

	@Override
	public void reset()
	{
		lastCommandEnd = 0;
		lastPos = 0;
		quoteOn = false;
		commentOn = false;
		blockComment = false;
		singleLineComment = false;
		lastNewLineStart = 0;
		lastQuote = 0;
	}

	@Override
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

	@Override
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

	@Override
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

	private boolean isLineComment(int pos)
	{
		return StringUtil.lineStartsWith(this.script, pos, "--") || StringUtil.lineStartsWith(this.script, pos, alternateLineComment);
	}

	private boolean isQuoteChar(boolean inQuotes, char currentChar)
	{
		if (currentChar == '\'') return true;
		if (currentChar == '"') return true;
		if (supportIdioticQuotes)
		{
			if (inQuotes) return currentChar == ']';
			else return currentChar == '[';
		}
		return false;
	}

	/**
	 *	Parse the given SQL Script into a List of single SQL statements.
	 *	Returns the index of the statement indicated by the currentCursorPos
	 */
	@Override
	public ScriptCommandDefinition getNextCommand()
	{
		int pos;
		boolean delimiterOnOwnLine = this.delimiter.isSingleLine();
		String delim = this.delimiter.getDelimiter();

		for (pos = this.lastPos; pos < this.scriptLength; pos++)
		{
			char firstChar = this.script.charAt(pos);

			// skip CR characters
			if (firstChar == '\r') continue;

			char nextChar = (pos < scriptLength - 1 ? this.script.charAt(pos + 1) : 0);

			// ignore quotes in comments
			if (!commentOn && isQuoteChar(quoteOn, firstChar))
			{
				if (!quoteOn)
				{
					lastQuote = firstChar;
					quoteOn = true;
				}
				else if (firstChar == lastQuote || (lastQuote == '[' && firstChar == ']'))
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
					}
					else if (firstChar != '\n' && isLineComment(pos))
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
				String currWord = null;
				if (this.delimiterLength > 1 && pos + this.delimiterLength < scriptLength)
				{
					currWord = this.script.subSequence(pos, pos + this.delimiterLength).toString().toUpperCase();
				}
				else
				{
					currWord = String.valueOf(firstChar);
				}

				if (!delimiterOnOwnLine && (currWord.equals(delim) || (pos == scriptLength)))
				{
					if (lastPos >= pos && pos < scriptLength - 1)
					{
						lastPos ++;
						continue;
					}
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
						String line = this.script.subSequence(lastNewLineStart, pos).toString().trim();
						String clean = SqlUtil.makeCleanSql(line, false, false);

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
								this.lastNewLineStart = pos + 1;
								this.lastPos = lastNewLineStart;
								this.lastCommandEnd = lastPos;
								return c;
							}
						}

						if (this.checkSingleLineCommands || supportOracleInclude)
						{
							boolean slcFound = false;

							int commandStart = lastNewLineStart;
							int commandEnd = pos;

							lastNewLineStart = pos;

							if (clean.length() > 0)
							{
								if (this.supportOracleInclude)
								{
									Matcher m = ORA_INCLUDE_PATTERN.matcher(clean);
									if (m.matches())
									{
										slcFound = true;
									}
								}

								if (!slcFound && checkSingleLineCommands)
								{
									for (Pattern SLC_PATTERNS1 : SLC_PATTERNS)
									{
										Matcher m = SLC_PATTERNS1.matcher(clean);
										if (m.matches())
										{
											slcFound = true;
											break;
										}
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
				}
			}

		} // end loop for next statement

		ScriptCommandDefinition c = null;
		if (lastPos < pos)
		{
			String value = this.script.subSequence(lastCommandEnd, scriptLength).toString();
			String tvalue = value.trim();
			if (!this.delimiter.equals(tvalue))
			{
				int endpos = scriptLength;
				if (tvalue.endsWith(delim))
				{
					int dpos = value.lastIndexOf(delim);
					endpos = lastCommandEnd + dpos;
					//endpos = endpos - this.delimiterLength;
				}
				c = createCommand(lastCommandEnd, endpos);
			}
		}
		this.lastPos = scriptLength;
		return c;
	}

	private ScriptCommandDefinition createCommand(int startPos, int endPos)
	{
		String value = null;

		if (startPos >= scriptLength) return null;

		if (endPos == -1)
		{
			endPos = scriptLength;
		}

		int realStart = startPos;

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

		if (startPos >= endPos) return null;
		if (storeSqlInCommands)
		{
			value = this.script.subSequence(startPos, endPos).toString();
		}
		ScriptCommandDefinition c = new ScriptCommandDefinition(value, startPos, endPos);
		c.setWhitespaceStart(realStart);

		return c;
	}

	@Override
	public void setCheckEscapedQuotes(boolean flag)
	{
		this.checkEscapedQuotes = flag;
	}

	@Override
	public void done()
	{
		this.script.done();
	}

}
