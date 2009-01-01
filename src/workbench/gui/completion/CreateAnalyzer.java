/*
 * CreateAnalyzer.java
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
import workbench.util.StringUtil;

/**
 * Analyze a CREATE INDEX statement to provide completion for tables and columns
 * @author support@sql-workbench.net
 */
public class CreateAnalyzer
	extends BaseAnalyzer
{
	
	public CreateAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}
	
	protected void checkContext()
	{
		SQLLexer lexer = new SQLLexer(this.sql);
		
		boolean isCreateIndex = false;
		boolean showColumns = false;
		boolean showTables = false;
		int tableStartPos = -1;
		int tableEndPos = -1;
		int tokenCount = 0;
		boolean afterCreate = true;
		
		try
		{
			SQLToken token = lexer.getNextToken(false, false);
			while (token != null)
			{
				final String t = token.getContents();
				tokenCount++;
				if (tokenCount == 2)
				{
					afterCreate = (token.getCharBegin() > this.cursorPos);
				}
				
				if (isCreateIndex)
				{
					if ("ON".equalsIgnoreCase(t))
					{
						if (this.cursorPos > token.getCharEnd()) 
						{
							showTables = true;
							showColumns = false;
						}
						tableStartPos = token.getCharEnd();
					}
					else if ("(".equals(t))
					{
						tableEndPos = token.getCharBegin() - 1;
						if (this.cursorPos >= token.getCharBegin())
						{
							showTables = false;
							showColumns = true;
						}
					}
					else if (")".equals(t))
					{
						if (this.cursorPos > token.getCharBegin())
						{
							showTables = false;
							showColumns = false;
						}
					}
				}
				else if ("INDEX".equalsIgnoreCase(t))
				{
					isCreateIndex = true;
				}
				
				token = lexer.getNextToken(false, false);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("AlterTableAnalyzer", "Error parsing SQL", e);
		}
		
		if (showTables)
		{
			context = CONTEXT_TABLE_LIST;
			String q = getQualifierLeftOfCursor();
			if (StringUtil.isEmptyString(q))
			{
				this.schemaForTableList = this.dbConnection.getMetadata().getCurrentSchema();
			}
			else
			{
				this.schemaForTableList = q;
			}
		}
		else if (showColumns)
		{
			context = CONTEXT_COLUMN_LIST;
			if (tableEndPos == -1) tableEndPos = this.sql.length() - 1;
			String table = this.sql.substring(tableStartPos, tableEndPos).trim();
			this.tableForColumnList = new TableIdentifier(table);
		}
		else if (afterCreate)
		{
			context = CONTEXT_KW_LIST;
			this.keywordFile = "create_types.txt";
		}
	}
	
}
