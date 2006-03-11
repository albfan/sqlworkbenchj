/*
 * SelectAnalyzer.java
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

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
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
	private final Pattern WHERE_PATTERN = Pattern.compile("\\sWHERE\\s|\\sWHERE$", Pattern.CASE_INSENSITIVE);
	private final Pattern GROUP_PATTERN = Pattern.compile("\\sGROUP\\s*BY\\s|\\sGROUP\\s*BY$", Pattern.CASE_INSENSITIVE);
	
	public SelectAnalyzer(WbConnection conn, String statement, int cursorPos)
	{	
		super(conn, statement, cursorPos);
	}
	
	protected void checkContext()
	{
		this.context = NO_CONTEXT;
		String currentWord = StringUtil.getWordLeftOfCursor(this.sql, cursorPos, ",(;");
		if (!StringUtil.isEmptyString(currentWord))
		{
			boolean keyWord = this.dbConnection.getMetadata().isKeyword(currentWord);
			setOverwriteCurrentWord(!keyWord);
		}
		else
		{
			setOverwriteCurrentWord(false);
		}
		
		setAppendDot(false);
		setColumnPrefix(null);
		int fromPos = SqlUtil.getFromPosition(this.sql); 
		
		int wherePos = -1;
		
		if (fromPos > 0)
		{
			wherePos = StringUtil.findPattern(WHERE_PATTERN, sql, fromPos);
		}

		int groupStart = 0;
		if (wherePos > 0) groupStart = wherePos + 1;
		else if (fromPos > 0) groupStart = fromPos + 1;
		
		int groupPos = StringUtil.findPattern(GROUP_PATTERN, sql, groupStart);
		
		// find the tables from the FROM clause
		List tables = Collections.EMPTY_LIST;
		try
		{
			tables = SqlUtil.getTables(sql, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("SelectAnalyzer.getContext()", "Could not retrieve table list in FROM part", e);
			tables = Collections.EMPTY_LIST;
		}
		
		boolean afterWhere = (wherePos > 0 && cursorPos > wherePos);
		boolean afterGroup = (groupPos > 0 && cursorPos > groupPos);

		boolean inTableList = ( fromPos < 0 ||
			   (wherePos < 0 && cursorPos > fromPos) ||
			   (wherePos > -1 && cursorPos > fromPos && cursorPos <= wherePos));
		
		if (inTableList && afterGroup) inTableList = false;
		
		if (inTableList)
		{
			String q = getQualifierLeftOfCursor(sql, cursorPos);
			if (q != null)
			{
				setOverwriteCurrentWord(!this.dbConnection.getMetadata().isKeyword(q));
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
			String q = getQualifierLeftOfCursor(sql, cursorPos);
			this.tableForColumnList = null;

			if (afterGroup)
			{
				this.elements = getColumnsForGroupBy(fromPos);
				this.title = ResourceMgr.getString("TxtTitleColumns");
				return;
			}
			
			// check if the current qualifier is either one of the
			// tables in the table list or one of the aliases used
			// in the table list.
			TableAlias currentAlias = null;
			if (q != null)
			{
				for (int i=0; i < count; i++)
				{
					String element = (String)tables.get(i);
					TableAlias tbl = new TableAlias(element);

					if (tbl.isTableOrAlias(q))
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

			if (tableForColumnList == null && currentWord != null && currentWord.endsWith(".") && afterWhere)
			{
				tableForColumnList = new TableIdentifier(currentWord.substring(0, currentWord.length() - 1));
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


	private List getColumnsForGroupBy(int fromStart)
	{
		int colStart = StringUtil.findFirstWhiteSpace(this.sql.trim());
		String columns = this.sql.substring(colStart, fromStart);
		List cols = StringUtil.stringToList(columns, ",", true, true);
		List validCols = new LinkedList();
		Pattern p = Pattern.compile("\\s*AS\\s*", Pattern.CASE_INSENSITIVE);
		String[] funcs = new String[]{"sum", "count", "avg", "min", "max" };
		StringBuffer regex = new StringBuffer(50);
		for (int i = 0; i < funcs.length; i++)
		{
			if (i > 0) regex.append('|');
			regex.append("\\s*");
			regex.append(funcs[i]);
			regex.append("\\s*\\(");
		}
		System.out.println("pattern=" + regex);
		Pattern aggregate = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
		for (int i = 0; i < cols.size(); i++)
		{
			String col = (String)cols.get(i);
			int alias = StringUtil.findPattern(p, col, 0);
			if (alias > -1)
			{
				col = col.substring(0, alias).trim();
			}
			if (StringUtil.findPattern(aggregate, col, 0) == -1)
			{
				validCols.add(col);
			}
		}
		return validCols;
	}
}
