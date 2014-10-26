/*
 * MySQLShow.java
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
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.List;

import workbench.storage.DataStore;

import workbench.sql.parser.ParserType;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;


/**
 * A class to display the output of MySQL's "show engine innodb status" as a message
 * rather than a result set.
 *
 * Every other option to the show command is handled as is.
 *
 * @author Thomas Kellerer
 */
public class MySQLShow
	extends SqlCommand
{
	public static final String VERB = "SHOW";

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result =  super.execute(sql);
		if (!isInnoDBStatus(sql))
		{
			return result;
		}

		List<DataStore> status = result.getDataStores();
		if (status != null && status.size() == 1)
		{
			DataStore ds = status.get(0);
			StringBuilder msg = new StringBuilder(500);
			int col = ds.getColumnIndex("Status");
			if (col > -1)
			{
				for (int row = 0; row < ds.getRowCount(); row ++)
				{
					String value = ds.getValueAsString(row, col);
					msg.append(value);
					msg.append('\n');
				}
			}
			result.clearResultData();
			result.addMessage(msg);
		}
		return result;
	}

	/**
	 * Package visible for testing purposes.
	 */
	boolean isInnoDBStatus(String sql)
	{
		String[] words = new String[] { "show", "engine", "innodb", "status"};
		SQLLexer lexer = SQLLexerFactory.createLexer(ParserType.MySQL, sql);
		SQLToken token = lexer.getNextToken(false, false);
		int index = 0;
		while (token != null)
		{
			if (!token.getText().equalsIgnoreCase(words[index])) return false;
			index ++;
			token = lexer.getNextToken(false, false);
		}
		return index == 4;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

}
