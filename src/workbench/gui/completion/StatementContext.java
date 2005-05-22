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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
		
		if (!inSubSelect(conn, sql, pos))
		{
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
		}
		
		if (analyzer != null)
		{
			analyzer.retrieveObjects();
		}
	}
	
	public boolean getOverwriteCurrentWord()
	{
		if (analyzer == null) return false;
		return analyzer.getOverwriteCurrentWord();
	}
	
	private final Pattern SUBSELECT_PATTERN = Pattern.compile("\\(\\s*SELECT.*FROM.*\\)", Pattern.CASE_INSENSITIVE);
	private final Pattern OPEN_BRACKET = Pattern.compile("^\\s*\\(\\s*");
	private final Pattern CLOSE_BRACKET = Pattern.compile("\\s*\\)\\s*$");
	
	private boolean inSubSelect(WbConnection conn, String sql, int pos)
	{
		Matcher m = SUBSELECT_PATTERN.matcher(sql);
		int start = 0;
		int end = -1;
		while (m.find())
		{
			start = m.start();
			end = m.end();
			if (pos > start && pos < end)
			{
				int newpos = pos - start;
				
				// cleanup the brackets, and adjust the position
				// to reflect the removed brackets at the beginning
				String subselect = sql.substring(start, end);
				Matcher o = OPEN_BRACKET.matcher(subselect);
				
				// the find() is necessary in order to get the correct
				// value from end()
				if (o.find())
				{
					int oend = o.end(); // is actually the number of characters that we'll remove
					newpos -= oend;
					subselect = o.replaceAll("");
				}
				
				Matcher c = CLOSE_BRACKET.matcher(subselect);
				subselect = c.replaceAll("");
				analyzer = new SelectAnalyzer(conn, subselect, newpos);
				return true;
			}
		}
		return false;
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
