/*
 * AlterTableAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

/**
 * Analyze an ALTER TABLE statement to provide completion for tables and columns
 * @author support@sql-workbench.net
 */
public class AlterTableAnalyzer
	extends BaseAnalyzer
{

	public AlterTableAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	protected void checkContext()
	{
		int addPos = -1;
		int modifyPos = -1;
		int tablePos = -1;
		int tableEnd = -1;
		SQLLexer lexer = new SQLLexer(this.sql);

		try
		{
			SQLToken token = lexer.getNextToken(false, false);
			while (token != null)
			{
				String v = token.getContents();
				if ("TABLE".equalsIgnoreCase(v))
				{
					tablePos = token.getCharEnd();
				}
				else if ("ADD".equalsIgnoreCase(v))
				{
					addPos = token.getCharEnd();
					tableEnd = token.getCharBegin() - 1;
				}
				else if ("MODIFY".equalsIgnoreCase(v) || "DROP".equalsIgnoreCase(v))
				{
					modifyPos = token.getCharEnd();
					tableEnd = token.getCharBegin() - 1;
				}
				token = lexer.getNextToken(false, false);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("AlterTableAnalyzer", "Error parsing SQL", e);
		}

		String q = this.getQualifierLeftOfCursor();
		if (q != null)
		{
			this.schemaForTableList = q;
		}
		else
		{
			this.schemaForTableList = this.dbConnection.getMetadata().getCurrentSchema();
		}

		if (between(cursorPos, tablePos, modifyPos) || between(cursorPos, tablePos, addPos) ||
			 (modifyPos == -1 && addPos == -1))
		{
			context = CONTEXT_TABLE_LIST;
			setTableTypeFilter(this.dbConnection.getMetadata().getTableTypeName());
		}
		else if (modifyPos > -1 && tableEnd > -1)
		{
			String table = this.sql.substring(tablePos, tableEnd).trim();
			context = CONTEXT_COLUMN_LIST;
			tableForColumnList = new TableIdentifier(table);
		}

	}

}
