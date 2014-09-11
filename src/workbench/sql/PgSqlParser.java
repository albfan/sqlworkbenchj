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

import workbench.log.LogMgr;

import workbench.sql.formatter.SQLToken;


/**
 *
 * @author Thomas Kellerer
 */
public class PgSqlParser
	extends LexerBasedParser
{
	public PgSqlParser()
	{
	}

	public PgSqlParser(String script)
		throws IOException
	{
		super(script);
	}

	public PgSqlParser(File f, String encoding)
		throws IOException
	{
		super(f, encoding);
	}

	@Override
	public ScriptCommandDefinition getNextCommand()
	{
		calledOnce = true;

		String delimiterString = ";";
		try
		{
			StringBuilder sql = new StringBuilder(250);

			int previousEnd = -1;

			SQLToken token = lexer.getNextToken();
			boolean startOfLine = false;
			boolean danglingQuote = false;
			boolean inPgQuote = false;
			String pgQuoteString = null;

			while (token != null)
			{
				if (lastStart == -1) lastStart = token.getCharBegin();
				String text = token.getText();

				if (isDollarQuote(text))
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
					if (startOfLine && !token.isWhiteSpace())
					{
						startOfLine = false;
					}

					if (delimiterString.equals(text))
					{
						break;
					}
					else if (isLineBreak(text))
					{
						if (emptyLineIsDelimiter && isMultiLine(text))
						{
							break;
						}
						startOfLine = true;
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

	private boolean isDollarQuote(String text)
	{
		if (text == null || text.isEmpty()) return false;
		if (text.charAt(0) != '$') return false;
		return text.endsWith("$");
	}


}
