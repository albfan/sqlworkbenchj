/*
 * UpdateAnalyzer.java
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
 * Analyze an UPDATE statement regarding the context for the auto-completion
 * @author support@sql-workbench.net
 */
public class UpdateAnalyzer
	extends BaseAnalyzer
{
	private	final Pattern SET_PATTERN = Pattern.compile("\\sSET\\s|\\sSET$", Pattern.CASE_INSENSITIVE);

	public UpdateAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	protected void checkContext()
	{
		int setPos = StringUtil.findPattern(SET_PATTERN, sql, 0);

		checkOverwrite();
		
		if ( setPos == -1 || setPos > -1 && this.cursorPos < setPos )
		{
			context = CONTEXT_TABLE_LIST;
			String q = this.getQualifierLeftOfCursor();
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
