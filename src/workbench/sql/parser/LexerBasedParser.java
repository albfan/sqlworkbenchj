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
	protected Pattern MULTI_LINE_PATTERN = Pattern.compile("(([ \t\f]*\n[ \t\f]*){2,})|(([ \t\f]*\r\n[ \t\f]*){2,})");
	protected Pattern SIMPLE_LINE_BREAK = Pattern.compile("(([ \t\f]*\n[ \t\f]*)+)|(([ \t\f]*\r\n[ \t\f]*)+)");
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

		SQLToken token = null;

		if (lastStatementUsedTerminator && emptyLineIsDelimiter)
		{
			token = skipEmptyLines();
		}
		else
		{
			token = lexer.getNextToken(true, true);
		}

		boolean singleLineCommand = false;
		boolean danglingQuote = false;
		boolean inPgQuote = false;
		boolean scriptEnd = false;
		boolean isFirstToken = true;
		boolean isNewline = false;

		int statementEnd = -1;
		int statementStart = token != null ? token.getCharBegin() : 0;
		int lineStartIndex = -1;
		String currentLine = "";
		String pgQuoteString = null;

		StringBuilder sql = null;
		if (storeStatementText || !returnLeadingWhitespace)
		{
			sql = new StringBuilder(250);
		}

		lastStatementUsedTerminator = false;
		DelimiterDefinition matchedDelimiter = null;

		while (token != null)
		{
			String text = token.getText();

			isNewline = !token.isComment() && isLineBreak(text);

			if (!isNewline)
			{
				if (currentLine.isEmpty())
				{
					lineStartIndex = token.getCharBegin();
				}
				currentLine += text;
			}

			if (delimiterTester != null)
			{
				delimiterTester.currentToken(token, isFirstToken);
				currentDelim = delimiterTester.getCurrentDelimiter();
			}

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
				// this ensures that a statement with a dangling quote is still "detected".
				// If this test wasn't here, the incorrect statement would never be returned by the parser at all.
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
				if (isFirstToken && delimiterTester != null && !token.isWhiteSpace() && !token.isComment())
				{
					singleLineCommand = delimiterTester.isSingleLineStatement(token, isFirstToken);
				}

				if (!singleLineCommand && !currentDelim.isSingleLine())
				{
					if (text.equals(currentDelim.getDelimiter()))
					{
						lastStatementUsedTerminator = true;
						matchedDelimiter = currentDelim;
						if (statementEnd == -1)
						{
							statementEnd  = token.getCharBegin();
						}
						break;
					}
				}

				if (isNewline || (token.isWhiteSpace() && isMultiLine(text)))
				{
					if (delimiterTester != null)
					{
						delimiterTester.lineEnd();
					}

					DelimiterDefinition lineDelim = null;
					if (!singleLineCommand)
					{
						lineDelim = matchesLineDelimiter(currentLine, currentDelim);
					}

					if (lineDelim != null)
					{
						lastStatementUsedTerminator = true;
						matchedDelimiter = lineDelim;
						break;
					}
					else if (singleLineCommand || (emptyLineIsDelimiter && isMultiLine(text) && currentDelim.equals(delimiter)))
					{
						lastStatementUsedTerminator = false;
						break;
					}
					singleLineCommand = false;
					currentLine = "";
				}
			}

			if (isFirstToken && !token.isWhiteSpace() && !token.isComment())
			{
				isFirstToken = false;
			}
			statementEnd = token.getCharEnd();
			token = lexer.getNextToken(true, true);

			if (sql != null)
			{
				sql.append(text);
			}
		}

		boolean matchedAtEnd = false;
		if (lineStartIndex > -1)
		{
			DelimiterDefinition lineDelim = matchesLineDelimiter(currentLine, currentDelim);
			if (lineDelim != null)
			{
				trimStatement(sql, lineStartIndex, statementEnd);
				statementEnd = lineStartIndex;
				matchedDelimiter = lineDelim;
				matchedAtEnd = true;
			}
		}

		if (statementEnd > 0)
		{
			if (token == null && !scriptEnd && !matchedAtEnd) statementEnd = realScriptLength;
			ScriptCommandDefinition cmd = createCommandDef(sql, statementStart, statementEnd);
			cmd.setIndexInScript(currentStatementIndex);
			cmd.setDelimiterUsed(matchedDelimiter);
			cmd.setDelimiterNeeded(!singleLineCommand);
			currentStatementIndex ++;
			if (delimiterTester != null)
			{
				delimiterTester.statementFinished();
			}
			return cmd;
		}
		return null;
	}

	private DelimiterDefinition matchesLineDelimiter(String currentLine, DelimiterDefinition currentDelimiter)
	{
		if (currentLine == null) return null;
		currentLine = currentLine.trim();
		if (currentDelimiter != null && currentDelimiter.isSingleLine())
		{
			if (currentLine.equalsIgnoreCase(currentDelimiter.getDelimiter())) return currentDelimiter;
		}

		if (alternateDelimiter != null && alternateDelimiter.isSingleLine())
		{
			if (currentLine.equalsIgnoreCase(alternateDelimiter.getDelimiter())) return alternateDelimiter;
		}
		return null;
	}

	private void trimStatement(StringBuilder sql, int realEnd, int statementEnd)
	{
		if (sql == null) return;

		if (realEnd < statementEnd)
		{
			int num = statementEnd - realEnd;
			// the delimiter was already added to the text we store
			// so we need to remove that
			sql.delete(sql.length() - num, sql.length());
		}
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
		if (i < 0) i = 0;
		String toStore = null;
		if (storeStatementText)
		{
			toStore = sql.substring(i);
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
