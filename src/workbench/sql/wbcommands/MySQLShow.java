/*
 * MySQLShow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.List;

import workbench.storage.DataStore;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;


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

	private boolean isInnoDBStatus(String sql)
	{
		String[] words = new String[] { "show", "engine", "innodb", "status"};
		SQLLexer lexer = new SQLLexer(sql);
		SQLToken token = lexer.getNextToken(false, false);
		int index = 0;
		while (token != null)
		{
			if (!token.getText().equalsIgnoreCase(words[index])) return false;
			index ++;
			token = lexer.getNextToken(false, false);
		}
		return true;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

}
