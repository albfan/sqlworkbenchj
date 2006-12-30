/*
 * StatementContext.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import workbench.db.WbConnection;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.wbcommands.WbSelectBlob;
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
			if ("SELECT".equalsIgnoreCase(verb) || WbSelectBlob.VERB.equalsIgnoreCase(verb))
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
			else if ("ALTER".equalsIgnoreCase(verb))
			{
				analyzer = new AlterTableAnalyzer(conn, sql, pos);
			}
			else if ("INSERT".equalsIgnoreCase(verb))
			{
				analyzer = new InsertAnalyzer(conn, sql, pos);
			}
			else if ("CREATE".equalsIgnoreCase(verb))
			{
				analyzer = new CreateAnalyzer(conn, sql, pos);
			}
		}
		
		if (analyzer != null)
		{
			analyzer.retrieveObjects();
		}
	}

	public boolean isKeywordList()
	{
		if (analyzer == null) return false;
		return analyzer.isKeywordList();
	}
	
	public boolean appendDotToSelection()
	{
		if (analyzer == null) return false;
		return analyzer.appendDotToSelection();
	}
	
	public boolean getOverwriteCurrentWord()
	{
		if (analyzer == null) return false;
		return analyzer.getOverwriteCurrentWord();
	}
	
	public String getColumnPrefix()
	{
		if (analyzer == null) return null;
		return analyzer.getColumnPrefix();
	}
	
//	private final Pattern INSERT_SELECT_PATTERN = Pattern.compile("INSERT\\s+INTO\\s+.*\\s+SELECT\\s+", Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
//	private final Pattern SELECT_PATTERN = Pattern.compile("SELECT\\s+", Pattern.CASE_INSENSITIVE);
	
	private boolean inSubSelect(WbConnection conn, String sql, int pos)
	{
		try
		{
			SQLLexer lexer = new SQLLexer(sql);

			SQLToken t = lexer.getNextToken(false, false);
			SQLToken lastToken = null;
			
			int lastStart = 0;
			int lastEnd = 0;
			String verb = t.getContents();
			
			// Will contain the position of each SELECT verb
			// if a UNION is encountered.
			List unionStarts = new ArrayList();
			int bracketCount = 0;
			boolean inSubselect = false;
			boolean checkForInsertSelect = verb.equals("INSERT") || verb.equals("CREATE");
			
			Set unionKeywords = new HashSet();
			unionKeywords.add("UNION");
			unionKeywords.add("MINUS");
			unionKeywords.add("INTERSECT");
			
			while (t != null)
			{
				String value = t.getContents();
				if ("(".equals(value)) 
				{
					bracketCount ++;
					if (bracketCount == 1) lastStart = t.getCharBegin();
				}
				else if (")".equals(value))
				{
					bracketCount --;
					if (inSubselect && bracketCount == 0)
					{
						lastEnd = t.getCharBegin();
						if (lastStart <= pos && pos <= lastEnd) 
						{
							int newpos = pos - lastStart - 1;
							String sub = sql.substring(lastStart + 1, lastEnd);
							analyzer = new SelectAnalyzer(conn, sub, newpos);
							return true;
						}
					}
					if (bracketCount == 0)
					{
						inSubselect = false;
						lastStart = 0;
						lastEnd = 0;
					}
				}
				else if (bracketCount == 0 && checkForInsertSelect && value.equals("SELECT"))
				{
					if (pos >= t.getCharBegin())
					{
						int newPos = pos - t.getCharBegin();
						analyzer = new SelectAnalyzer(conn, sql.substring(t.getCharBegin()), newPos);
						return true;
					}
				}
				else if (bracketCount == 0 && unionKeywords.contains(t.getContents()))
				{
					if (t.getContents().equals("UNION"))
					{
						SQLToken t2 = lexer.getNextToken(false, false);
						if (t2.getContents().equals("ALL"))
						{
							// swallow potential UNION ALL
							unionStarts.add(new Integer(t.getCharBegin()));
						}
						else
						{
							unionStarts.add(new Integer(t.getCharEnd()));
							
							// continue with the token just read
							lastToken = t;
							t = t2;
							continue;
						}
					}
					else
					{
						unionStarts.add(new Integer(t.getCharEnd()));
					}
				}
				
				if (bracketCount == 1 && lastToken.getContents().equals("(") && t.getContents().equals("SELECT"))
				{
					inSubselect = true;
				}

				lastToken = t;
				t = lexer.getNextToken(false, false);
			}
			
			if (unionStarts.size() > 0)
			{
				boolean found = false;
				int index = 0;
				int lastPos = 0;
				while (index < unionStarts.size())
				{
					int startPos = ((Integer)unionStarts.get(index)).intValue();
					if (lastPos <= pos && pos <= startPos)
					{
						int newPos = pos - lastPos;
						analyzer = new SelectAnalyzer(conn, sql.substring(lastPos, startPos), newPos);
						return true;
					}
					lastPos = startPos;
					index ++;
				}
				// check last union
				int startPos = ((Integer)unionStarts.get(unionStarts.size()-1)).intValue();
				if (pos >= startPos)
				{
					int newPos = pos - startPos;
					analyzer = new SelectAnalyzer(conn, sql.substring(startPos), newPos);
					return true;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
