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

import workbench.db.importer.RowDataProducer;

import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLLexerFactory;
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
	protected String fileEncoding;
	protected SQLLexer lexer;
	protected Reader input;
	protected DelimiterDefinition delimiter = DelimiterDefinition.STANDARD_DELIMITER;
	protected int lastStart = -1;
	protected int currentStatementIndex;
	protected boolean checkEscapedQuotes;
	protected boolean storeStatementText = true;
	protected boolean returnLeadingWhitespace;
	protected boolean emptyLineIsDelimiter;
	protected int scriptLength;
	protected int realScriptLength;
	protected boolean hasMoreCommands;
	protected boolean checkOracleInclude;
	protected boolean calledOnce;
	protected boolean checkPgQuoting;
	protected boolean lastStatementUsedTerminator;
	protected ParserType parserType;
	protected Pattern MULTI_LINE_PATTERN = Pattern.compile("((\r\n)|(\n)){2,}|[ \t\f]*((\r\n)|(\n))+[ \t\f]*((\r\n)|(\n))+[ \t\f]*");
	protected Pattern SIMPLE_LINE_BREAK = Pattern.compile("[ \t\f]*((\r\n)|(\n\r)|(\r|\n))+[ \t\f]*");

	public LexerBasedParser()
	{
		this.parserType = ParserType.Standard;
	}

	public LexerBasedParser(ParserType type)
	{
		this.parserType = type;
		setCheckPgQuoting(parserType == ParserType.Postgres);
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

	public void setCheckPgQuoting(boolean flag)
	{
		checkPgQuoting = flag;
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
		StringBuilder sql = null;
		if (storeStatementText || !returnLeadingWhitespace)
		{
			sql = new StringBuilder(250);
		}

		int previousEnd = -1;

		SQLToken token = null;

		if (lastStatementUsedTerminator && emptyLineIsDelimiter)
		{
			token = skipEmptyLines();
		}
		else
		{
			token = lexer.getNextToken(true, true);
		}

		boolean startOfLine = false;
		boolean singleLineCommand = false;
		boolean danglingQuote = false;
		boolean inPgQuote = false;
		boolean scriptEnd = false;

		String pgQuoteString = null;

		lastStatementUsedTerminator = false;

		while (token != null)
		{
			if (lastStart == -1) lastStart = token.getCharBegin();
			String text = token.getText();

			boolean checkForDelimiter = !delimiter.isSingleLine() || (delimiter.isSingleLine() && startOfLine);

			if (checkPgQuoting && isDollarQuote(text))
			{
				if (inPgQuote && text.equals(pgQuoteString))
				{
					inPgQuote = false;
				}
				else
				{
					inPgQuote = true;
					pgQuoteString = text;
				}
			}
			else if (token.isUnclosedString())
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
			else if (!inPgQuote)
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
					lastStatementUsedTerminator = true;
					break;
				}
				else if (checkForDelimiter && delimiterString.startsWith(text))
				{
					// handle delimiters that are not parsed as a single token
					StringBuilder delim = new StringBuilder(delimiter.getDelimiter().length());
					delim.append(text);
					StringBuilder skippedText = new StringBuilder(text.length() + 5);
					skippedText.append(text);

					while ((token = lexer.getNextToken(true, true)) != null)
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
						lastStatementUsedTerminator = true;
						scriptEnd = token == null;
						break;
					}
					text += skippedText.toString();
				}
				else if (isLineBreak(text))
				{
					if (singleLineCommand || (emptyLineIsDelimiter && isMultiLine(text)))
					{
						lastStatementUsedTerminator = false;
						break;
					}
					startOfLine = true;
					singleLineCommand = false;
				}
			}
			previousEnd = token.getCharEnd();
			token = lexer.getNextToken(true, true);
			if (sql != null)
			{
				sql.append(text);
			}
		}
		if (previousEnd > 0)
		{
			if (token == null && !scriptEnd) previousEnd = realScriptLength;
			ScriptCommandDefinition cmd = createCommandDef(sql, lastStart, previousEnd);
			cmd.setIndexInScript(currentStatementIndex);
			currentStatementIndex ++;
			lastStart = -1;
			hasMoreCommands = token != null && token.getCharEnd() < scriptLength;
			return cmd;
		}
		hasMoreCommands = false;
		return null;
	}

	protected SQLToken skipEmptyLines()
	{
		SQLToken token = lexer.getNextToken(true, true);
		if (token == null) return null;

		String text = token.getText();
		while (token != null && (isLineBreak(text) || isMultiLine(text)))
		{
			token = lexer.getNextToken(true, true);
			if (token != null)
			{
				text = token.getText();
			}
		}
		return token;
	}

	boolean isLineBreak(String text)
	{
		return SIMPLE_LINE_BREAK.matcher(text).matches();
	}

	boolean isMultiLine(String text)
	{
		return MULTI_LINE_PATTERN.matcher(text).matches();
	}

	protected ScriptCommandDefinition createCommandDef(StringBuilder sql, int start, int end)
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
		checkEscapedQuotes = flag;
	}

	@Override
	public void setSupportOracleInclude(boolean flag)
	{
		checkOracleInclude = flag;
	}

	private void createLexer()
	{
		if (checkEscapedQuotes)
		{
			lexer = SQLLexerFactory.createNonStandardLexer(input, parserType);
		}
		else
		{
			lexer = SQLLexerFactory.createLexer(input, parserType);
		}
	}

	@Override
	public final void setFile(File f, String encoding)
		throws IOException
	{
		cleanup();
		scriptLength = (int)FileUtil.getCharacterLength(f, encoding);
		input = EncodingUtil.createBufferedReader(f, encoding);
		createLexer();
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
		cleanup();
		String toUse = StringUtil.rtrim(script);
		input = new StringReader(toUse);
		createLexer();
		scriptLength = toUse.length();
		realScriptLength = script.length();
		hasMoreCommands = (scriptLength > 0);
	}

	private void cleanup()
	{
		calledOnce = false;
		lastStart = -1;
		currentStatementIndex = 0;
		lastStatementUsedTerminator = false;
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

	protected boolean isDollarQuote(String text)
	{
		if (text == null || text.isEmpty()) return false;
		if (text.charAt(0) != '$') return false;
		if (text.equals(RowDataProducer.SKIP_INDICATOR)) return false;
		return text.endsWith("$");
	}

}
