/*
 * LexerBasedParser.java
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
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Pattern;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class LexerBasedParser
	implements ScriptIterator
{
	private File originalFile;
	private String fileEncoding;
	private SQLLexer lexer;
	private Reader input;
	private DelimiterDefinition delimiter = DelimiterDefinition.STANDARD_DELIMITER;
	private int lastStart = -1;
	private int currentStatementIndex;
	private boolean storeStatementText = true;
	private boolean returnLeadingWhitespace;
	private boolean emptyLineIsDelimiter;
	private int scriptLength;
	private int realScriptLength;
	private boolean hasMoreCommands;
	private boolean checkOracleInclude;
	private boolean calledOnce;

	private Pattern MULTI_LINE_PATTERN = Pattern.compile("((\r\n)|(\n)){2,}|[ \t\f]*((\r\n)|(\n))+[ \t\f]*((\r\n)|(\n))+[ \t\f]*");
	private Pattern SIMPLE_LINE_BREAK = Pattern.compile("[ \t\f]*((\r\n)|(\n\r)|(\r|\n))+[ \t\f]*");

	public LexerBasedParser()
	{
	}

	public LexerBasedParser(String script)
		throws IOException
	{
		setScript(script);
	}

	public LexerBasedParser(File f, String encoding)
		throws IOException
	{
		setFile(f, encoding);
	}

	@Override
	public void setDelimiter(DelimiterDefinition def)
	{
		delimiter = def.createCopy();
	}

	@Override
	public void setEmptyLineIsDelimiter(boolean flag)
	{
		emptyLineIsDelimiter = flag;
	}

	/**
	 * Controls if the actual SQL for each command returned by
	 * #getNextCommand() is stored in the ScriptCommandDefinition
	 * or if only start and end in the script should be stored.
	 *
	 * @param flag if true, the actual SQL is returned otherwise only the start and end
	 */
	@Override
	public void setStoreStatementText(boolean flag)
	{
		storeStatementText = flag;
	}

	@Override
	public void done()
	{
		FileUtil.closeQuietely(input);
	}

	@Override
	public ScriptCommandDefinition getNextCommand()
	{
		calledOnce = true;

		String delimiterString = delimiter.getDelimiter();
		try
		{
			StringBuilder sql = new StringBuilder(250);

			int previousEnd = -1;

			SQLToken token = lexer.getNextToken();
			boolean startOfLine = false;
			boolean singleLineCommand = false;
			boolean danglingQuote = false;

			while (token != null)
			{
				if (lastStart == -1) lastStart = token.getCharBegin();
				String text = token.getText();

				boolean checkForDelimiter = !delimiter.isSingleLine() || (delimiter.isSingleLine() && startOfLine);

				if (token.isUnclosedString())
				{
					danglingQuote = true;
				}
				else if (danglingQuote)
				{
					if (text.charAt(0) == '\'')
					{
						danglingQuote = false;
					}
				}
				else
				{
					if (checkOracleInclude && startOfLine && !singleLineCommand && text.charAt(0) == '@')
					{
						singleLineCommand = true;
					}

					if (startOfLine && !token.isWhiteSpace())
					{
						startOfLine = false;
					}

					if (checkForDelimiter && delimiterString.equals(text))
					{
						break;
					}
					else if (checkForDelimiter && delimiterString.startsWith(text))
					{
						StringBuilder delim = new StringBuilder(delimiter.getDelimiter().length());
						delim.append(text);
						StringBuilder skippedText = new StringBuilder(text.length() + 5);
						skippedText.append(text);

						while ((token = lexer.getNextToken()) != null)
						{
							if (storeStatementText) skippedText.append(token.getText());
							if (token.isComment() || token.isWhiteSpace() || token.isLiteral()) break;
							delim.append(token.getText());
							if (delim.length() > delimiterString.length()) break;
							if (!delimiterString.startsWith(delim.toString())) break;
						}
						boolean delimiterMatched = delimiterString.equals(delim.toString());
						if (delimiterMatched)
						{
							break;
						}
						text += skippedText.toString();
					}
					else if (isLineBreak(text))
					{
						if (singleLineCommand || (emptyLineIsDelimiter && isMultiLine(text)))
						{
							break;
						}
						startOfLine = true;
						singleLineCommand = false;
					}
				}
				previousEnd = token.getCharEnd();
				token = lexer.getNextToken();
				sql.append(text);
			}
			if (previousEnd > 0)
			{
				if (token == null) previousEnd = realScriptLength;
				ScriptCommandDefinition cmd = createCommandDef(sql, lastStart, previousEnd);
				cmd.setIndexInScript(currentStatementIndex);
				currentStatementIndex ++;
				lastStart = -1;
				hasMoreCommands = (token != null);
				return cmd;
			}
			hasMoreCommands = false;
			return null;
		}
		catch (IOException e)
		{
			LogMgr.logError("LexerBasedParser.getNextCommand()", "Error parsing script", e);
			hasMoreCommands = false;
			return null;
		}
	}

	boolean isLineBreak(String text)
	{
		return SIMPLE_LINE_BREAK.matcher(text).matches();
	}

	boolean isMultiLine(String text)
	{
		return MULTI_LINE_PATTERN.matcher(text).matches();
	}

	private ScriptCommandDefinition createCommandDef(StringBuilder sql, int start, int end)
	{
		if (returnLeadingWhitespace || sql == null || !Character.isWhitespace(sql.charAt(0)) || sql.length() == 0)
		{
			String toStore = storeStatementText ? sql.toString() : null;
			return new ScriptCommandDefinition(toStore, start, end);
		}

		int i = StringUtil.findFirstNonWhitespace(sql);
		String toStore = null;
		if (storeStatementText)
		{
			if (i > -1) toStore = sql.substring(i);
			else toStore = sql.toString();
		}
		ScriptCommandDefinition cmd = new ScriptCommandDefinition(toStore, start + i, end);
		cmd.setWhitespaceStart(start);

		return cmd;
	}

	@Override
	public int getScriptLength()
	{
		return scriptLength;
	}

	@Override
	public boolean hasMoreCommands()
	{
		return hasMoreCommands;
	}

	@Override
	public void setCheckForSingleLineCommands(boolean flag)
	{
	}

	@Override
	public void setAlternateLineComment(String comment)
	{
	}

	@Override
	public void setCheckEscapedQuotes(boolean flag)
	{
	}

	@Override
	public void setSupportOracleInclude(boolean flag)
	{
		checkOracleInclude = flag;
	}

	@Override
	public final void setFile(File f, String encoding)
		throws IOException
	{
		scriptLength = (int)FileUtil.getCharacterLength(f, encoding);
		input = EncodingUtil.createBufferedReader(f, encoding);
		lexer = new SQLLexer(input);
		calledOnce = false;
		hasMoreCommands = (scriptLength > 0);
		fileEncoding = encoding;
		originalFile = f;
	}

	@Override
	public void setReturnStartingWhitespace(boolean flag)
	{
		returnLeadingWhitespace = flag;
	}

	@Override
	public final void setScript(String script)
	{
		String toUse = StringUtil.rtrim(script);
		input = new StringReader(toUse);
		lexer = new SQLLexer(input);
		scriptLength = toUse.length();
		realScriptLength = script.length();
		calledOnce = false;
		hasMoreCommands = (scriptLength > 0);
	}

	@Override
	public void reset()
	{
		if (!calledOnce) return;

		try
		{
			if (originalFile != null)
			{
				FileUtil.closeQuietely(input);
				input = EncodingUtil.createBufferedReader(originalFile, fileEncoding);
			}
			else
			{
				input.reset();
			}
		}
		catch (IOException io2)
		{
			LogMgr.logError("LexerBasedParser.reset()", "Cannot re-open input stream", io2);
		}
	}
}
