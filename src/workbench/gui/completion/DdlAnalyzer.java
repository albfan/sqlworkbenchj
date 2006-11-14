/*
 * DdlAnalyzer.java
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
import java.util.ArrayList;
import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.WbConnection;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

/**
 * Analyze a DDL statement regarding the context for the auto-completion
 * @author support@sql-workbench.net
 */
public class DdlAnalyzer
	extends BaseAnalyzer
{
	public DdlAnalyzer(WbConnection conn, String statement, int cursorPos)
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
		
		String verb = verbToken.getContents();
		
		if ("TRUNCATE".equalsIgnoreCase(verb))
		{
			context = CONTEXT_TABLE_LIST;
			return;
		}
		
		SQLToken typeToken = lexer.getNextToken(false, false);
		String type = (typeToken != null ? typeToken.getContents() : null);

		String q = this.getQualifierLeftOfCursor();
		if (q != null)
		{
			this.schemaForTableList = q;
		}
		else
		{
			this.schemaForTableList = this.dbConnection.getMetadata().getCurrentSchema();
		}

		if (!"DROP".equals(verb))
		{
			context = NO_CONTEXT;
			return;
		}
		
		if (type == null || between(cursorPos,verbToken.getCharEnd(), typeToken.getCharBegin()))
		{
			context = CONTEXT_KW_LIST;
			this.keywordFile = "drop_types.txt";
		}
		
		// for DROP etc, we'll need to be after the table keyword
		// otherwise it could be a DROP PROCEDURE as well.
		if ("TABLE".equals(type) && cursorPos >= typeToken.getCharEnd())
		{
			context = CONTEXT_TABLE_LIST;
			setTableTypeFilter(this.dbConnection.getMetadata().getTableTypeName());
		}
		else if ("VIEW".equals(type) && cursorPos >= typeToken.getCharEnd())
		{
			context = CONTEXT_TABLE_LIST;
			setTableTypeFilter(this.dbConnection.getMetadata().getViewTypeName());
		}
	}


}
