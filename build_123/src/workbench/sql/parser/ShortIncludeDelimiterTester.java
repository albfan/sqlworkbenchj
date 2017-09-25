/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
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
public class ShortIncludeDelimiterTester
	implements DelimiterTester
{
	private DelimiterDefinition defaultDelimiter = DelimiterDefinition.STANDARD_DELIMITER;
	private boolean checkShortInclude = true;

	public ShortIncludeDelimiterTester()
	{
	}


	@Override
	public void setDelimiter(DelimiterDefinition delim)
	{
		defaultDelimiter = delim;
		checkShortInclude = defaultDelimiter.isStandard();
	}

	@Override
	public void setAlternateDelimiter(DelimiterDefinition delimiter)
	{
	}

	@Override
	public boolean supportsMixedDelimiters()
	{
		return false;
	}

	@Override
	public void currentToken(SQLToken token, boolean isStartOfStatement)
	{
	}

	@Override
	public DelimiterDefinition getCurrentDelimiter()
	{
		if (defaultDelimiter != null) return defaultDelimiter;
		return DelimiterDefinition.STANDARD_DELIMITER;
	}

	@Override
	public void statementFinished()
	{
	}

	@Override
	public boolean supportsSingleLineStatements()
	{
		return true;
	}

	@Override
	public boolean isSingleLineStatement(SQLToken token, boolean isStartOfLine)
	{
		if (!checkShortInclude) return false;

		if (token == null) return false;

		if (isStartOfLine && !token.isWhiteSpace())
		{
			String text = token.getText();
			return text.charAt(0) == '@';
		}
		return false;
	}

	@Override
	public void lineEnd()
	{
	}

}
