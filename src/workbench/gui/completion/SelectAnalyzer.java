/*
 * SelectAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
	private final Pattern FROM_PATTERN = Pattern.compile("\\sFROM\\s|\\sFROM$", Pattern.CASE_INSENSITIVE);
	private final Pattern WHERE_PATTERN = Pattern.compile("\\sWHERE\\s|\\sWHERE$", Pattern.CASE_INSENSITIVE);
	
	public SelectAnalyzer(WbConnection conn, String statement, int cursorPos)
	{	
		super(conn, statement, cursorPos);
	}
	
	protected void checkContext()
	{

		int len = sql.length();
		this.context = NO_CONTEXT;

		int fromPos = StringUtil.findPattern(FROM_PATTERN, sql, 0);
		int wherePos = -1;
		if (fromPos > 0)
		{
			wherePos = StringUtil.findPattern(WHERE_PATTERN, sql, fromPos);
			//if (wherePos == -1) wherePos = sql.length() + 1;
		}

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

		if ( fromPos < 0 ||
			   (wherePos < 0 && pos > fromPos) ||
			   (wherePos > -1 && pos > fromPos && pos <= wherePos))
		{
			String q = this.getQualifierLeftOfCursor(sql, pos);
			
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
			if (count == 1)
			{
				TableAlias tbl = new TableAlias((String)tables.get(0));
				tableForColumnList = tbl.getTable();
			}
			else
			{
				String q = getQualifierLeftOfCursor(sql, pos);
				String tableToUse = null;
				this.tableForColumnList = null;
				
				// check if the current qualifier is either one of the
				// tables in the table list or one of the aliases used
				// in the table list.
				if (q != null)
				{
					for (int i=0; i < count; i++)
					{
						String element = (String)tables.get(i);
						TableAlias tbl = new TableAlias(element);

						if (tbl.isTableOrAlias(q))
						{
							tableForColumnList = tbl.getTable();
							break;
						}
					}
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
					}
				}
			}
		}
	}


}