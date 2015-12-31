/*
 * ExecAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.completion;

import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.StringUtil;

/**
 * Supply a list of stored procedures for EXEC or WbCall
 *
 */
public class ExecAnalyzer
	extends BaseAnalyzer
{
	private String qualifier;

	public ExecAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	@Override
	protected void checkContext()
	{
		SQLLexer lexer = SQLLexerFactory.createLexer(dbConnection, this.sql);
		SQLToken verbToken = lexer.getNextToken(false, false);

		if (verbToken == null)
		{
			this.context = NO_CONTEXT;
			return;
		}

		context = CONTEXT_TABLE_LIST;
		qualifier = getQualifierLeftOfCursor();
	}

	@Override
	protected void buildResult()
	{
		if (context == NO_CONTEXT) return;

		title = ResourceMgr.getString("TxtDbExplorerProcs");
		String schema = null;

		if (StringUtil.isNonBlank(qualifier))
		{
			String[] parsed = qualifier.split("\\.");
			if (parsed.length == 1)
			{
				schema = parsed[0];
			}
			if (parsed.length == 2)
			{
				schema = parsed[1];
			}
		}

		if (schema == null)
		{
			schema = this.dbConnection.getCurrentSchema();
		}
		elements = dbConnection.getObjectCache().getProcedures(schema);
	}

}
