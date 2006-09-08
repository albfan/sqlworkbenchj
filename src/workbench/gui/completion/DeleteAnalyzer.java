/*
 * DeleteAnalyzer.java
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
 *
 * @author support@sql-workbench.net
 */
public class DeleteAnalyzer
	extends BaseAnalyzer
{
	public DeleteAnalyzer(WbConnection conn, String statement, int cursorPos)
	{	
		super(conn, statement, cursorPos);
	}
	
	protected void checkContext()
	{
		this.context = -1;

		int wherePos = SqlUtil.getKeywordPosition("WHERE", sql);
		checkOverwrite();
		
		if ( wherePos == -1 || wherePos > -1 && cursorPos < wherePos)
		{
			
			context = CONTEXT_TABLE_LIST;
			String q = this.getQualifierLeftOfCursor();
			if (q != null)
			{
				this.setOverwriteCurrentWord(false);
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
			String table = SqlUtil.getDeleteTable(sql);
			if (table != null) tableForColumnList = new TableIdentifier(table);
		}
	}


}
