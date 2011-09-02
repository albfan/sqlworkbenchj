/*
 * DdlAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import workbench.db.WbConnection;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.CollectionUtil;

/**
 * Analyze a DDL statement regarding the context for the auto-completion
 * @author Thomas Kellerer
 */
public class DdlAnalyzer
	extends BaseAnalyzer
{
	public DdlAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	@Override
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

		if ("DROP".equals(verb))
		{
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
				setTableTypeFilter(this.dbConnection.getMetadata().getTableTypes());
			}
			else if ("VIEW".equals(type) && cursorPos >= typeToken.getCharEnd())
			{
				context = CONTEXT_TABLE_LIST;
				setTableTypeFilter(CollectionUtil.arrayList(this.dbConnection.getMetadata().getViewTypeName()));
			}
//			else if ("INDEX".equals(type) && cursorPos >= typeToken.getCharEnd())
//			{
//			IndexDefinition[] idx = this.dbConnection.getMetadata().getIndexList(this.schemaForTableList);
//			this.elements = new ArrayList();
//			for (int i = 0; i < idx.length; i++)
//			{
//				this.elements.add(idx[i].getName());
//			}
//			}
		}
		else
		{
			context = NO_CONTEXT;
		}

	}


}
