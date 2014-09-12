/*
 * OracleDDLCleaner.java
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
package workbench.db.oracle;

import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLLexerFactory;
import workbench.sql.formatter.SQLToken;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDDLCleaner
{

	/**
	 * Remove double quotes from all quoted uppercase identifiers.
	 *
	 * @param input the SQL text (typically retrieved using dbms_metadata)
	 * @return a clean version without quotes that are not needed.
	 */
	public static String cleanupQuotedIdentifiers(String input)
	{
		StringBuilder result = new StringBuilder(input.length());
		SQLLexer lexer = SQLLexerFactory.createLexer(input);
		SQLToken token = lexer.getNextToken(true, true);
		while (token != null)
		{
			if (token.isIdentifier())
			{
				String text = token.getText();
				char firstChar = text.charAt(0);
				char lastChar = text.charAt(text.length() - 1);

				if (firstChar == '"' && lastChar == '"' && text.toUpperCase().equals(text))
				{
					result.append(text.substring(1, text.length() - 1));
				}
				else
				{
					result.append(text);
				}
			}
			else
			{
				result.append(token.getText());
			}
			token = lexer.getNextToken(true, true);
		}
		return result.toString();
	}

}
