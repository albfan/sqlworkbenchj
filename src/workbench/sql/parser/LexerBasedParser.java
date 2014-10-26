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
package workbench.sql.parser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Pattern;

import workbench.log.LogMgr;

import workbench.db.importer.RowDataProducer;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ScriptCommandDefinition;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

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
	protected DelimiterDefinition alternateDelimiter = null;
	protected int lastStart = -1;
	protected int currentStatementIndex;
	protected boolean checkEscapedQuotes;
	protected boolean storeStatementText = true;
	protected boolean returnLeadingWhitespace;
	protected boolean emptyLineIsDelimiter;
	protected int scriptLength;
	protected int realScriptLength;
	protected boolean calledOnce;
	protected boolean checkPgQuoting;
	protected boolean lastStatementUsedTerminator;
	protected final ParserType parserType;
	protected Pattern MULTI_LINE_PATTERN = Pattern.compile("((\r\n)|(\n)){2,}|[ \t\f]*((\r\n)|(\n))+[ \t\f]*((\r\n)|(\n))+[ \t\f]*");
	protected Pattern SIMPLE_LINE_BREAK = Pattern.compile("[ \t\f]*((\r\n)|(\n\r)|(\r|\n))+[ \t\f]*");
	protected DelimiterTester delimiterTester;

	public LexerBasedParser()
	{
		this(ParserType.Standard);
	}

	public LexerBasedParser(ParserType type)
	{
		parserType = type;
		setCheckPgQuoting(parserType == ParserType.Postgres);
		if (type == ParserType.Oracle)
		{
			delimiterTester = new OracleDelimiterTester();
		}
	}

	public LexerBasedParser(String script)
		throws IOException
	{
		parserType = ParserType.Standard;
		setScript(script);
	}

	public LexerBasedParser(File f, String encoding)
		throws IOException
	{
		parserType = ParserType.Standard;
		setFile(f, encoding);
	}

	@Override
	public boolean supportsMixedDelimiter()
	{
		return delimiterTester != null;
	}

	@Override
	public void setAlternateDelimiter(DelimiterDefinition delim)
	{
		if (delim == null)
		{
			this.alternateDelimiter = null;
		}
		else
		{
			this.alternateDelimiter = delim.createCopy();
			if (this.delimiterTester != null)
			{
				delimiterTester.setAlternateDelimiter(alternateDelimiter);
			}
		}
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
		cleanup();
		FileUtil.closeQuietely(input);
	}

	protected DelimiterDefinition getCurrentDelimiter()
	{
		if (delimiterTester != null)
		{
			return delimiterTester.getCurrentDelimiter();
		}
		return delimiter;
	}

	@Override
	public ScriptCommandDefinition getNextCommand()
	{
		calledOnce = true;

		DelimiterDefinition currentDelim = getCurrentDelimiter();

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
		boolean firstToken = true;

		String pgQuoteString = null;

		lastStatementUsedTerminator = false;
		DelimiterDefinition matchedDelimiter = null;

		while (token != null)
		{
			if (lastStart == -1) lastStart = token.getCharBegin();
			String text = token.getText();

			if (delimiterTester != null)
			{
				delimiterTester.currentToken(token, startOfLine);
				currentDelim = delimiterTester.getCurrentDelimiter();
			}

			boolean checkForDelimiter = !currentDelim.isSingleLine() || (startOfLine && currentDelim.isSingleLine()) || (startOfLine && alternateDelimiter != null);

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
				if (firstToken && delimiterTester != null && !token.isWhiteSpace())
				{
					singleLineCommand = delimiterTester.isSingleLineStatement(token, firstToken);
				}

				if (!singleLineCommand && checkForDelimiter)
				{
					DelimiterCheckResult check = isDelimiter(currentDelim, token, startOfLine);
					if (check.found)
					{
						lastStatementUsedTerminator = true;
						matchedDelimiter = check.matchedDelimiter;
						scriptEnd = (check.lastToken == null);
						// if previousEnd is still -1
						// this means we have a completely empty statement --> silently ignore this
						if (previousEnd > -1)
						{
							break;
						}
					}

					if (check.skippedText != null)
					{
						text += check.skippedText;
					}

				}

				if (startOfLine && !token.isWhiteSpace())
				{
					startOfLine = false;
				}

				if (isLineBreak(text))
				{
					if (singleLineCommand || (emptyLineIsDelimiter && isMultiLine(text) && currentDelim.equals(delimiter)))
					{
						lastStatementUsedTerminator = false;
						break;
					}
					startOfLine = true;
					singleLineCommand = false;
					if (delimiterTester != null)
					{
						delimiterTester.lineEnd();
					}
				}
			}

			if (firstToken && !token.isWhiteSpace())
			{
				firstToken = false;
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
			cmd.setDelimiterUsed(matchedDelimiter == null ? getCurrentDelimiter() : matchedDelimiter);
			currentStatementIndex ++;
			lastStart = -1;
			if (delimiterTester != null)
			{
				delimiterTester.statementFinished();
			}
			return cmd;
		}
		return null;
	}

	private DelimiterCheckResult isDelimiter(DelimiterDefinition currentDelimiter, SQLToken token, boolean startOfLine)
	{
		DelimiterCheckResult result = checkDelimiter(currentDelimiter, token, startOfLine);
		if (result.found)
		{
			return result;
		}

		boolean checkForAlternateDelim = currentDelimiter != null && currentDelimiter.isStandard() && delimiterTester != null;
		if (checkForAlternateDelim)
		{
			result = isDelimiter(alternateDelimiter, token, startOfLine);
		}
		return result;
	}

	private DelimiterCheckResult checkDelimiter(DelimiterDefinition delimiter, SQLToken token, boolean startOfLine)
	{
		DelimiterCheckResult result = new DelimiterCheckResult();
		result.lastToken = token;
		result.skippedText = null;

		if (token == null) return result;
		if (delimiter == null) return result;

		String tokenText = token.getText();
		String delimiterString = delimiter.getDelimiter();

		if (tokenText.equals(delimiterString) )
		{
			if (delimiter.isSingleLine() && startOfLine || !delimiter.isSingleLine())
			{
				result.matchedDelimiter = delimiter;
				result.found = true;
				return result;
			}
		}

		if (!delimiterString.startsWith(tokenText))
		{
			result.found = false;
			return result;
		}

		// handle delimiters that are not parsed as a single token
		StringBuilder delim = new StringBuilder(delimiterString.length());
		delim.append(tokenText);
		StringBuilder skippedText = new StringBuilder(tokenText.length() + 5);
		skippedText.append(tokenText);

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
			if (delimiter.isSingleLine())
			{
				result.found = startOfLine;
			}
			else
			{
				result.found = true;
			}
			if (result.found)
			{
				result.matchedDelimiter = delimiter;
			}
		}
		result.skippedText = skippedText.toString();
		return result;
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
		if (returnLeadingWhitespace || StringUtil.isEmptyString(sql) || !Character.isWhitespace(sql.charAt(0)))
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
	public void setAlternateLineComment(String comment)
	{
	}

	@Override
	public void setCheckEscapedQuotes(boolean flag)
	{
		checkEscapedQuotes = flag;
	}

	private void createLexer()
	{
		if (checkEscapedQuotes)
		{
			lexer = SQLLexerFactory.createNonStandardLexer(parserType, input);
		}
		else
		{
			lexer = SQLLexerFactory.createLexer(parserType, input);
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
	}

	private void cleanup()
	{
		calledOnce = false;
		lastStart = -1;
		currentStatementIndex = 0;
		lastStatementUsedTerminator = false;
		if (delimiterTester != null)
		{
			delimiterTester.statementFinished();
		}
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
			cleanup();
			createLexer();
		}
		catch (IOException io2)
		{
			LogMgr.logError("LexerBasedParser.reset()", "Cannot re-open input stream", io2);
		}
	}

	@Override
	public boolean isSingleLimeCommand()
	{
		if (this.delimiterTester == null) return false;
		reset();
		SQLToken first = lexer.getNextToken(false, false);
		return delimiterTester.isSingleLineStatement(first, true);
	}

	@Override
	public boolean supportsSingleLineCommands()
	{
		if (delimiterTester == null) return false;
		return delimiterTester.supportsSingleLineStatements();
	}

	protected boolean isDollarQuote(String text)
	{
		if (text == null || text.isEmpty()) return false;
		if (text.charAt(0) != '$') return false;
		if (text.equals(RowDataProducer.SKIP_INDICATOR)) return false;
		return text.endsWith("$");
	}

	private class DelimiterCheckResult
	{
		boolean found;
		String skippedText;
		SQLToken lastToken;
		DelimiterDefinition matchedDelimiter;
	}

}
