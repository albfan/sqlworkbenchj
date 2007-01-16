/*
 * SelectAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.formatter.SqlFormatter;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.TableAlias;

/**
 *
 * @author support@sql-workbench.net
 */
public class SelectAnalyzer
	extends BaseAnalyzer
{
	private final int NO_JOIN_ON = 0;
	private final int JOIN_ON_TABLE_LIST = 1;
	private final int JOIN_ON_COLUMN_LIST = 2;
	
	public SelectAnalyzer(WbConnection conn, String statement, int cursorPos)
	{	
		super(conn, statement, cursorPos);
	}
	
	protected void checkContext()
	{
		this.context = NO_CONTEXT;
		
		String currentWord = getCurrentWord();
		
		setAppendDot(false);
		setColumnPrefix(null);
		int fromPos = SqlUtil.getFromPosition(this.sql); 
		
		int wherePos = -1;
		
		if (fromPos > 0)
		{
			wherePos = SqlUtil.getWherePosition(sql);
		}

		int groupPos = SqlUtil.getKeywordPosition("GROUP BY", sql);
		int havingPos = SqlUtil.getKeywordPosition("HAVING", sql);
		int orderPos = SqlUtil.getKeywordPosition("ORDER BY", sql);
		
		// find the tables from the FROM clause
		List tables = SqlUtil.getTables(sql, true);
		
		boolean afterWhere = (wherePos > 0 && cursorPos > wherePos);
		boolean afterGroup = (groupPos > 0 && cursorPos > groupPos);
		if (havingPos > -1 && afterGroup)
		{
			afterGroup = (cursorPos < havingPos);
		}

		if (orderPos > -1 && afterGroup)
		{
			afterGroup = (cursorPos < orderPos);
		}
		
		boolean afterHaving = (havingPos > 0 && cursorPos > havingPos);
		if (orderPos > -1 && afterHaving)
		{
			afterHaving = (cursorPos < orderPos);
		}

		boolean inTableList = ( fromPos < 0 ||
			   (wherePos < 0 && cursorPos > fromPos) ||
			   (wherePos > -1 && cursorPos > fromPos && cursorPos <= wherePos));
		
		if (inTableList && afterGroup) inTableList = false;
		
		int joinState = inJoinONPart();
			
		if (inTableList && joinState != JOIN_ON_TABLE_LIST)
		{
			inTableList = false;
		}
		
		if (inTableList)
		{
			String q = getQualifierLeftOfCursor();
			if (q != null)
			{
				setOverwriteCurrentWord(true);//!this.dbConnection.getMetadata().isKeyword(q));
			}

			// If no FROM is present but there is a word with a dot
			// at the cursor position we will first try to use that 
			// as a table name (because usually you type the table name
			// first in the SELECT list. If no columns for that 
			// name are found, BaseAnalyzer will try to use that as a 
			// schema name.
			if (fromPos < 0 && q != null)
			{
				context = CONTEXT_TABLE_OR_COLUMN_LIST;
				this.tableForColumnList = new TableIdentifier(q);
			}
			else
			{
				context = CONTEXT_TABLE_LIST;
			}
		
			// The schemaForTableList will be set anyway
			// in order to allow BaseAnalyzer to retrieve 
			// the table list
			if (q != null)
			{
				this.schemaForTableList = q;
			}
			else
			{
				this.schemaForTableList = this.dbConnection.getMetadata().getCurrentSchema();
			}
		}
		else
		{
			context = CONTEXT_COLUMN_LIST;
			// current cursor position is after the WHERE
			// statement or before the FROM statement, so
			// we'll try to find a proper column list
			
			int count = tables.size();
			this.tableForColumnList = null;

			if (afterGroup)
			{
				this.elements = getColumnsForGroupBy();
				this.addAllMarker = true;
				this.title = ResourceMgr.getString("TxtTitleColumns");
				return;
			}
			
			if (afterHaving)
			{
				this.elements = getColumnsForHaving();
				this.addAllMarker = true;
				this.title = ResourceMgr.getString("TxtTitleGroupFuncs");
				return;
			}
			
			this.addAllMarker = !afterWhere;

			
			// check if the current qualifier is either one of the
			// tables in the table list or one of the aliases used
			// in the table list.
			TableAlias currentAlias = null;
			String table = null;
			if (currentWord != null)
			{
				int pos = currentWord.indexOf('.');
				if (pos > -1)
				{
					table = currentWord.substring(0, pos);
				}
			}
			
			if (table != null)
			{
				for (int i=0; i < count; i++)
				{
					String element = (String)tables.get(i);
					TableAlias tbl = new TableAlias(element);

					if (tbl.isTableOrAlias(table))
					{
						tableForColumnList = tbl.getTable();
						currentAlias = tbl;
						break;
					}
				}
			}
			else if (count == 1)
			{
				TableAlias tbl = new TableAlias((String)tables.get(0));
				tableForColumnList = tbl.getTable();
			}

			if (tableForColumnList == null)
			{
				context = CONTEXT_FROM_LIST;
				this.elements = new ArrayList();
				for (int i=0; i < count; i++)
				{
					String entry = (String)tables.get(i);
					TableAlias tbl = new TableAlias(entry);
					this.elements.add(tbl);
					setAppendDot(true);
				}
			}
			else if (currentAlias != null)
			{
				setColumnPrefix(currentAlias.getNameToUse());
			}
		}
	}

	private int inJoinONPart()
	{
		int result = NO_JOIN_ON;
		try
		{
			boolean afterFrom = false;
			boolean inONPart = false;
			int lastJoin = -1;
			SQLLexer lexer = new SQLLexer(this.sql);
			SQLToken token = lexer.getNextToken(false, false);
			int bracketCount = 0;
			while (token != null)
			{
				String t = token.getContents();
				if (afterFrom)
				{
					if ("(".equals(t))
					{
						bracketCount ++;
						if (inONPart && cursorPos >= token.getCharBegin()) result = JOIN_ON_COLUMN_LIST;
					}
					else if (")".equals(t))
					{
						if (bracketCount > 0)
						{
							if (inONPart && cursorPos < token.getCharBegin()) return JOIN_ON_COLUMN_LIST;
						}
						bracketCount --;
					}
					else if ("ON".equals(t))
					{
						inONPart = (cursorPos >= token.getCharBegin());
					}
					else if (t.equals("JOIN") || SqlUtil.JOIN_KEYWORDS.contains(t))
					{
						inONPart = false;
						if (t.equals("JOIN") && cursorPos > token.getCharEnd()) result = JOIN_ON_TABLE_LIST;
						else result = NO_JOIN_ON;
					}
					else if (SqlFormatter.FROM_TERMINAL.contains(t))
					{
						return result;
					}
				}
				else
				{
					if (SqlFormatter.FROM_TERMINAL.contains(t)) break;
					if (t.equals("FROM"))
					{
						if (cursorPos < token.getCharBegin()) return NO_JOIN_ON;
						afterFrom = true;
						result = JOIN_ON_TABLE_LIST;
					}
				}
				token = lexer.getNextToken(false, false);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SelectAnalyzer.inJoinONPart()", "Error parsing SQL Statement!", e);
		}
		return result;
	}	
	
	private List getColumnsForHaving()
	{
		List cols = SqlUtil.getSelectColumns(this.sql, false);
		List validCols = new LinkedList();
		for (int i = 0; i < cols.size(); i++)
		{
			String col = (String)cols.get(i);
			if (col.indexOf('(') > -1 && col.indexOf(')') > -1)
			{
				validCols.add(col);
			}
		}
		return validCols;
	}
	
	private List getColumnsForGroupBy()
	{
		List cols = SqlUtil.getSelectColumns(this.sql, false);
		List validCols = new LinkedList();
		String[] funcs = new String[]{"sum", "count", "avg", "min", "max" };
		StringBuilder regex = new StringBuilder(50);
		for (int i = 0; i < funcs.length; i++)
		{
			if (i > 0) regex.append('|');
			regex.append("\\s*");
			regex.append(funcs[i]);
			regex.append("\\s*\\(");
		}
		Pattern aggregate = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
		for (int i = 0; i < cols.size(); i++)
		{
			String col = (String)cols.get(i);
			if (StringUtil.findPattern(aggregate, col, 0) == -1)
			{
				validCols.add(col);
			}
		}
		return validCols;
	}
}
