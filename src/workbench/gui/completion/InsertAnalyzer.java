/*
 * InsertAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

/**
 * Analyze an UPDATE statement regarding the context for the auto-completion
 * @author support@sql-workbench.net
 */
public class InsertAnalyzer
	extends BaseAnalyzer
{
	private final Pattern INTO_PATTERN = Pattern.compile("\\sINTO\\s|\\sINTO$", Pattern.CASE_INSENSITIVE);
	
	public InsertAnalyzer(WbConnection conn, String statement, int cursorPos)
	{	
		super(conn, statement, cursorPos);
	}
	
	public void checkContext()
	{
		SQLLexer lexer = new SQLLexer(this.sql);
		
		int intoEnd = Integer.MAX_VALUE;
		int intoStart = Integer.MAX_VALUE;
		int tableStart = Integer.MAX_VALUE;
		int columnBracketStart = Integer.MAX_VALUE;
		int columnBracketEnd = Integer.MAX_VALUE;
		int valuesPos = Integer.MAX_VALUE;
		boolean inColumnBracket = false;
		
		String schema = null;
		String table = null;
		try
		{
			int bracketCount = 0;
			boolean nextTokenIsTable = false;
			SQLToken t = lexer.getNextToken(false, false);
			
			while (t != null)
			{
				String value = t.getContents();
				if ("(".equals(value)) 
				{
					bracketCount ++;
					// if the INTO keyword was already read but not the VALUES
					// keyword, the opening bracket marks the end of the table 
					// definition between INTO and the column list
					if (intoStart != Integer.MAX_VALUE && valuesPos == Integer.MAX_VALUE)
					{
						intoEnd = t.getCharBegin();
						columnBracketStart = t.getCharEnd();
						inColumnBracket = true;
					}
				}
				else if (")".equals(value))
				{
					if (inColumnBracket)
					{
						columnBracketEnd = t.getCharBegin();
					}
					bracketCount --;
					inColumnBracket = false;
				}
				else if (bracketCount == 0)
				{
					if (nextTokenIsTable)
					{
						tableStart = t.getCharBegin();
						table = t.getContents();
						nextTokenIsTable = false;
					}
					if ("INTO".equals(value))
					{
						intoStart = t.getCharEnd();
						nextTokenIsTable = true;
					}
					else if ("VALUES".equals(value))
					{
						valuesPos = t.getCharBegin();
						break;
					}
				}
				t = lexer.getNextToken(false, false);
			}		
		}
		catch (Exception e)
		{
			LogMgr.logError("InsertAnalyzer.checkContext()", "Error parsing insert statement", e);
			this.context = NO_CONTEXT;
		}
		
		TableIdentifier id = new TableIdentifier(table);
		
		if (cursorPos > intoStart && cursorPos < intoEnd)
		{
			if (cursorPos > tableStart)
			{
				if (id != null)
				{
					schemaForTableList = id.getSchema();
				}
				else
				{
					schemaForTableList = this.dbConnection.getMetadata().getCurrentSchema();
				}
			}
			context = CONTEXT_TABLE_LIST;
		}
		else if (cursorPos >= columnBracketStart && cursorPos <= columnBracketEnd)
		{
			tableForColumnList = id;
			context = CONTEXT_COLUMN_LIST;
		}
	}
	
}
