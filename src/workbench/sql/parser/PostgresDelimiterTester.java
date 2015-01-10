/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

	public PostgresDelimiterTester()
	{
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
		}
	}

	@Override
	public DelimiterDefinition getCurrentDelimiter()
	{
		return DelimiterDefinition.STANDARD_DELIMITER;
	}

	@Override
	public void statementFinished()
	{
		firstToken = null;
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
			return text.charAt(0) == '\\';
		}
		return false;
	}

	@Override
	public void lineEnd()
	{
	}

}
