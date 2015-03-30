/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.parser;

import workbench.sql.DelimiterDefinition;
import workbench.sql.lexer.SQLToken;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresDelimiterTester
	implements DelimiterTester
{
	private SQLToken firstToken;
  private SQLToken lastToken;

	private DelimiterDefinition defaultDelimiter = DelimiterDefinition.STANDARD_DELIMITER;
  private DelimiterDefinition copyDelimiter = new DelimiterDefinition("\\.");
  private boolean isCopy = false;
  private boolean isCopyFromStdin = false;

	public PostgresDelimiterTester()
	{
	}

	@Override
	public void setDelimiter(DelimiterDefinition delim)
	{
		this.defaultDelimiter = delim;
	}

	@Override
	public boolean supportsMixedDelimiters()
	{
		return true;
	}

	@Override
	public void setAlternateDelimiter(DelimiterDefinition delimiter)
	{
	}

	@Override
	public void currentToken(SQLToken token, boolean isStartOfStatement)
	{
		if (token == null) return;
		if (token.isComment() || token.isWhiteSpace()) return;

		if (firstToken == null)
		{
			firstToken = token;
      isCopy = token.getText().equalsIgnoreCase("copy");
		}

    if (isCopy && token.getText().equalsIgnoreCase("stdin") && lastToken != null && lastToken.getText().equalsIgnoreCase("from"))
    {
      isCopyFromStdin = true;
    }

    lastToken = token;
	}

	@Override
	public DelimiterDefinition getCurrentDelimiter()
	{
    if (isCopyFromStdin) return copyDelimiter;
		if (defaultDelimiter != null) return defaultDelimiter;
		return DelimiterDefinition.STANDARD_DELIMITER;
	}

	@Override
	public void statementFinished()
	{
		firstToken = null;
    lastToken = null;
    isCopy = false;
    isCopyFromStdin = false;
	}

	@Override
	public boolean supportsSingleLineStatements()
	{
		return true;
	}

	@Override
	public boolean isSingleLineStatement(SQLToken token, boolean isStartOfLine)
	{
		if (token == null) return false;

		if (isStartOfLine && !token.isWhiteSpace())
		{
			String text = token.getText();
			char c = text.charAt(0);
			return c == '\\' || c == '@';
		}
		return false;
	}

	@Override
	public void lineEnd()
	{
	}

}
