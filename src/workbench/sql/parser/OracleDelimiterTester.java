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

import java.util.Set;

import workbench.resource.Settings;

import workbench.db.oracle.OracleUtils;

import workbench.sql.DelimiterDefinition;
import workbench.sql.lexer.SQLToken;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDelimiterTester
	implements DelimiterTester
{
	private DelimiterDefinition alternateDelimiter = DelimiterDefinition.DEFAULT_ORA_DELIMITER;
	private boolean useAlternateDelimiter;
	private final Set<String> blockStart = CollectionUtil.caseInsensitiveSet("BEGIN", "DECLARE");
	private final Set<String> keywords = CollectionUtil.caseInsensitiveSet("CREATE", "CREATE OR REPLACE");
	private final Set<String> singleLineCommands = CollectionUtil.caseInsensitiveSet("WHENEVER", "ECHO", "DESC", "DESCRIBE", "SET");
	private final Set<String> types = CollectionUtil.caseInsensitiveSet("FUNCTION", "LIBRARY", "PACKAGE", "PACKAGE BODY", "PROCEDURE", "TRIGGER", "TYPE", "TYPE BODY");

	private SQLToken lastToken;
	private SQLToken firstToken;

	public OracleDelimiterTester()
	{
		boolean typesLikeSqlPlus = Settings.getInstance().getBoolProperty("workbench.oracle.sql.parser.types.altdelimiter", true);
		if (!typesLikeSqlPlus)
		{
			types.remove("TYPE");
		}
	}

	public void setRequireAlternateDelimiterForTypes(boolean flag)
	{
		if (flag)
		{
			types.add("TYPE");
		}
		else
		{
			types.remove("TYPE");
		}
	}

	@Override
	public void setAlternateDelimiter(DelimiterDefinition delimiter)
	{
		alternateDelimiter = delimiter.createCopy();
	}

	@Override
	public DelimiterDefinition getAlternateDelimiter()
	{
		return alternateDelimiter;
	}

	@Override
	public void currentToken(SQLToken token, boolean isStartOfStatement)
	{
		if (token == null) return;
		if (token.isComment()) return;

		if (useAlternateDelimiter && lastToken != null)
		{
			if (lastToken.getText().equals(alternateDelimiter.getDelimiter()) && isStartOfStatement)
			{
				useAlternateDelimiter = false;
			}
		}
		else if (blockStart.contains(token.getText()) && lastToken == null)
		{
			useAlternateDelimiter = true;
		}
		else if (firstToken != null)
		{
			useAlternateDelimiter = (types.contains(token.getText()) && keywords.contains(firstToken.getText()));
		}
		if (!token.isWhiteSpace() && !token.getContents().equalsIgnoreCase(OracleUtils.KEYWORD_EDITIONABLE))
		{
			lastToken = token;
		}
		if (firstToken == null && !token.isWhiteSpace())
		{
			firstToken = token;
		}
	}

	@Override
	public DelimiterDefinition getCurrentDelimiter()
	{
		if (useAlternateDelimiter)
		{
			return alternateDelimiter;
		}
		return DelimiterDefinition.STANDARD_DELIMITER;
	}

	@Override
	public void statementFinished()
	{
		useAlternateDelimiter = false;
		lastToken = null;
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
			if (text.charAt(0) == '@')
			{
				return true;
			}
			if (singleLineCommands.contains(text))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public void lineEnd()
	{
	}

}
