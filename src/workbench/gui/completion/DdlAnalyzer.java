/*
 * DdlAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import workbench.db.WbConnection;
import static workbench.gui.completion.BaseAnalyzer.CONTEXT_KW_LIST;
import static workbench.gui.completion.BaseAnalyzer.CONTEXT_TABLE_LIST;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.CollectionUtil;

/**
 * Analyze a DDL statement regarding the context for the auto-completion.
 *
 * Currently only TRUNCATE and DROP are supported.
 *
 * @author Thomas Kellerer
 * @see CreateAnalyzer
 * @see AlterTableAnalyzer
 */
public class DdlAnalyzer
	extends BaseAnalyzer
{
	public DdlAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	@Override
	protected void checkContext()
	{
		SQLLexer lexer = new SQLLexer(this.sql);
		SQLToken verbToken = lexer.getNextToken(false, false);
		if (verbToken == null)
		{
			this.context = NO_CONTEXT;
			return;
		}

		String verb = verbToken.getContents();

		if ("TRUNCATE".equalsIgnoreCase(verb))
		{
			context = CONTEXT_TABLE_LIST;
			return;
		}

		SQLToken typeToken = lexer.getNextToken(false, false);
		String type = (typeToken != null ? typeToken.getContents() : null);
		SQLToken nameToken = lexer.getNextToken(false, false);

		String q = this.getQualifierLeftOfCursor();
		if (q != null)
		{
			this.schemaForTableList = q;
		}
		else
		{
			this.schemaForTableList = this.dbConnection.getMetadata().getCurrentSchema();
		}

		if ("DROP".equals(verb))
		{
			if (type == null || between(cursorPos,verbToken.getCharEnd(), typeToken.getCharBegin()))
			{
				context = CONTEXT_KW_LIST;
				keywordFile = "drop_types.txt";
			}

			boolean showObjectList = typeToken != null && cursorPos >= typeToken.getCharEnd() && (nameToken == null || cursorPos < nameToken.getCharBegin());
			boolean showDropOption = nameToken != null && cursorPos >=  nameToken.getCharEnd();

			// for DROP etc, we'll need to be after the table keyword
			// otherwise it could be a DROP PROCEDURE as well.
			if ("TABLE".equals(type))
			{
				if (showObjectList)
				{
					context = CONTEXT_TABLE_LIST;
					setTableTypeFilter(this.dbConnection.getMetadata().getTableTypes());
				}
				else if (showDropOption)
				{
					// probably after the table name
					context = CONTEXT_KW_LIST;
					keywordFile = "table.drop_options.txt";
				}
			}
			else if ("VIEW".equals(type) && showObjectList)
			{
				context = CONTEXT_TABLE_LIST;
				setTableTypeFilter(CollectionUtil.arrayList(this.dbConnection.getMetadata().getViewTypeName()));
			}
			else if (isDropSchema(type))
			{
				if (showObjectList)
				{
					context = CONTEXT_SCHEMA_LIST;
				}
				else if (showDropOption)
				{
					context = CONTEXT_KW_LIST;
					keywordFile = type.trim().toLowerCase() + ".drop_options.txt";
				}
			}
			else if ("DATABASE".equals(type) && showObjectList)
			{
				context = CONTEXT_CATALOG_LIST;
			}
			else if ("SEQUENCE".equals(type) && showObjectList)
			{
				if (showObjectList)
				{
					context = CONTEXT_SEQUENCE_LIST;
				}
				else if (showDropOption)
				{
					// probably after the table name
					context = CONTEXT_KW_LIST;
					keywordFile = "sequence.drop_options.txt";
				}
			}
		}
		else
		{
			context = NO_CONTEXT;
		}
	}

	private boolean isDropSchema(String type)
	{
		return "SCHEMA".equalsIgnoreCase(type) ||
			     dbConnection.getMetadata().isOracle() && "USER".equalsIgnoreCase(type);

	}
}
