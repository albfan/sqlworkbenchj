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

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
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
 * Analyze an UPDATE statement regarding the context for the auto-completion
 * @author support@sql-workbench.net
 */
public class InsertAnalyzer
	extends BaseAnalyzer
{
	private final Pattern INTO_PATTERN = Pattern.compile("INSERT\\s+INTO\\s+[a-zA-Z0-9_$]*\\s+", Pattern.CASE_INSENSITIVE);
	
	public InsertAnalyzer(WbConnection conn, String statement, int cursorPos)
	{	
		super(conn, statement, cursorPos);
	}
	
	public void checkContext()
	{
		checkOverwrite();
		String currentWord = getCurrentWord();
		Matcher im = INTO_PATTERN.matcher(this.sql);
		
		int intoEnd = Integer.MAX_VALUE;
		int intoStart = Integer.MAX_VALUE;
		int tableBracketStart = Integer.MAX_VALUE;
		int tableBracketEnd = Integer.MAX_VALUE;
		
		if (im.find())
		{
			intoEnd = im.end();
			intoStart = im.start();
			tableBracketStart = this.sql.indexOf('(', intoEnd);
			if (tableBracketStart == -1) 
			{
				tableBracketStart = Integer.MAX_VALUE;
			}
			else
			{
				tableBracketEnd = this.sql.indexOf(')', tableBracketStart);
				if (tableBracketEnd == -1) tableBracketEnd = Integer.MAX_VALUE;
			}
		}
		
		if (cursorPos > tableBracketStart && cursorPos < tableBracketEnd)
		{
			// cursor is in VALUES ( ) part or 
			// in the column list for the INTO part
			context = CONTEXT_COLUMN_LIST;
				
			// find the table to be used
			int tableStart = intoEnd;
			int tableEnd = tableBracketStart - 1;
			String table = this.sql.substring(tableStart, tableEnd).trim();
			this.tableForColumnList = new TableIdentifier(table);
		}
		else if (cursorPos > intoStart)
		{
			context = CONTEXT_TABLE_LIST;
		}
	}
}
