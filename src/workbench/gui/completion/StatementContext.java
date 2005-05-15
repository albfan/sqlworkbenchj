/*
 * StatementContextAnalyzer.java
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

import java.util.Collections;
import java.util.List;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 * A factory to generate a BaseAnalyzer based on a given SQL statement
 * @author support@sql-workbench.net
 */
public class StatementContext
{
	private BaseAnalyzer analyzer;
	
	public StatementContext(WbConnection conn, String sql, int pos)
	{
		String verb = SqlUtil.getSqlVerb(sql);
		if ("SELECT".equalsIgnoreCase(verb))
		{
			analyzer = new SelectAnalyzer(conn, sql, pos);
		}
		else if ("UPDATE".equalsIgnoreCase(verb))
		{
			analyzer = new UpdateAnalyzer(conn, sql, pos);
		}
		else if ("DELETE".equalsIgnoreCase(verb))
		{
			analyzer = new DeleteAnalyzer(conn, sql, pos);
		}
		else if ("DROP".equalsIgnoreCase(verb) || "TRUNCATE".equalsIgnoreCase(verb))
		{
			analyzer = new DdlAnalyzer(conn, sql, pos);
		}
		
		if (analyzer != null)
		{
			analyzer.retrieveObjects();
		}
	}
	
	public boolean isStatementSupported()
	{
		return this.analyzer != null;
	}
	
	public List getData()
	{
		if (analyzer == null) return Collections.EMPTY_LIST;
		List result = analyzer.getData();
		if (result == null) return Collections.EMPTY_LIST;
		return result;
	}
	
	public String getTitle()
	{
		if (analyzer == null) return "";
		return analyzer.getTitle();
	}
	
}
