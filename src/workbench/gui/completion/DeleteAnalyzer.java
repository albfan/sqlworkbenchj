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
public class DeleteAnalyzer
	extends BaseAnalyzer
{
	private final Pattern WHERE_PATTERN = Pattern.compile("\\sWHERE\\s|\\sWHERE$", Pattern.CASE_INSENSITIVE);
	
	public DeleteAnalyzer(WbConnection conn, String statement, int cursorPos)
	{	
		super(conn, statement, cursorPos);
	}
	
	protected void checkContext()
	{

		this.context = -1;

		int wherePos = StringUtil.findPattern(WHERE_PATTERN, sql, 0);
		
		if ( wherePos == -1 || wherePos > -1 && pos < wherePos)
		{
			context = CONTEXT_TABLE_LIST;
			String q = this.getQualifierLeftOfCursor(sql, pos);
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
			// current cursor position is after the WHERE
			// so we'll need a column list
			int start = StringUtil.findFirstWhiteSpace(sql);
			
			int end = -1;
			if (start > -1) end = StringUtil.findFirstWhiteSpace(sql, start + 1);
			if (end == -1 && start > -1) end = this.sql.length() - 1;
			
			if (end > -1 && start > -1)
			{
				context = CONTEXT_COLUMN_LIST;
				String table = sql.substring(start, end).trim();
				tableForColumnList = new TableIdentifier(table);
			}
		}
	}


}