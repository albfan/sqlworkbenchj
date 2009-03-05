/*
 * ExecAnalyzer.java
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

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.StringUtil;

/**
 * Supply a list of stored procedures when running EXEC or WbRun
 *
 */
public class ExecAnalyzer
	extends BaseAnalyzer
{
	private String qualifier;

	public ExecAnalyzer(WbConnection conn, String statement, int cursorPos)
	{	
		super(conn, statement, cursorPos);
	}
	
	protected void checkContext()
	{
		SQLLexer lexer = new SQLLexer(this.sql);
		SQLToken verbToken = lexer.getNextToken(false, false);
		
		if (verbToken == null) 
		{
			this.context = NO_CONTEXT;
			return;
		}
		
		context = CONTEXT_TABLE_LIST;
		qualifier = getQualifierLeftOfCursor();
	}

	@Override
	protected void buildResult()
	{
		if (context == NO_CONTEXT) return;
		
		title = ResourceMgr.getString("TxtDbExplorerProcs");
		String schema = null;

		if (StringUtil.isNonBlank(qualifier))
		{
			String[] parsed = qualifier.split("\\.");
			if (parsed.length == 1)
			{
				schema = parsed[0];
			}
			if (parsed.length == 2)
			{
				schema = parsed[1];
			}
		}

		if (schema == null)
		{
			schema = this.dbConnection.getCurrentSchema();
		}
		elements = dbConnection.getObjectCache().getProcedures(schema);
	}

}
