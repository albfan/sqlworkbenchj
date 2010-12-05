/*
 * JoinCreator
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.fksupport;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import workbench.db.WbConnection;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.SqlUtil;
import workbench.util.TableAlias;

/**
 * A class to automatically create the needed joins between tables.
 * <br/>
 * Based on the current cursor position the passed SQL will be analyzed, the
 * join tables extraced and the correct join condition generated.
 * <br/>
 * Currently this is limited to two tables, multi-table JOIN ... ON clauses are not supported.
 * <br/>
 * In statements with mutiple JOIN conditions, always the last two tables are joined.
 * <br/>
 * Sub-Selects are not supported.
 *
 * @author Thomas Kellerer
 */
public class JoinCreator
{
	private String sql;
	private WbConnection connection;
	private int cursorPos;
	private Map<Integer, TableAlias> tablePositions;
	
	public JoinCreator(String statement, int positionInStatement, WbConnection dbConn)
	{
		this.sql = statement;
		this.connection = dbConn;
		this.cursorPos = positionInStatement;
		retrieveTablePositions();
	}

	public String getJoinCondition()
		throws SQLException
	{
		TableAlias joinTable = getJoinTable();
		TableAlias joinedTable = getJoinedTable();
		if (joinTable == null || joinedTable == null) return null;
		JoinColumnsDetector detector = new JoinColumnsDetector(connection, joinTable, joinedTable);
		return detector.getJoinCondition();
	}

	public TableAlias getJoinTable()
	{
		Integer pos = getTableIndexBeforeCursor();
		List<Integer> tableIndex = new ArrayList<Integer>(tablePositions.keySet());
		Collections.sort(tableIndex);
		int index = tableIndex.indexOf(pos);
		if (index > 0)
		{
			Integer mainPos = tableIndex.get(index - 1);
			return tablePositions.get(mainPos);
		}
		return null;
	}

	public TableAlias getJoinedTable()
	{
		Integer pos = getTableIndexBeforeCursor();
		return tablePositions.get(pos);
	}

	private Integer getTableIndexBeforeCursor()
	{
		List<Integer> tableIndex = new ArrayList<Integer>(tablePositions.keySet());
		Collections.sort(tableIndex);
		for (int i=0; i < tableIndex.size(); i++)
		{
			if (tableIndex.get(i) > cursorPos && i > 0)
			{
				return tableIndex.get(i - 1);
			}
		}
		return tableIndex.get(tableIndex.size() - 1);
	}
	
	private void retrieveTablePositions()
	{
		tablePositions = new HashMap<Integer, TableAlias>(5);
		SQLLexer lexer = new SQLLexer(sql);
		int bracketCount = 0;

		boolean nextIsTable = false;
		Set<String> joinKeywords = SqlUtil.getJoinKeyWords();
		
		SQLToken token = lexer.getNextToken(false, false);
		while (token != null)
		{
			String value = token.getContents();
			if ("(".equals(value))
			{
				bracketCount ++;
			}
			else if (")".equals(value))
			{
				bracketCount --;
			}
			else if (bracketCount == 0)
			{
				if (nextIsTable)
				{
					TableAlias tbl = null;
					SQLToken next = lexer.getNextToken(false, false);
					boolean useNext = false;
					if (next != null)
					{
						if (next.getText().equalsIgnoreCase("AS"))
						{
							next = lexer.getNextToken(false, false);
						}

						if (!next.isReservedWord())
						{
							tbl = new TableAlias(value + " " + next.getContents());
						}
					}

					if (tbl == null)
					{
						tbl = new TableAlias(value);
						useNext = true;
					}
					tablePositions.put(token.getCharBegin(), tbl);
					nextIsTable = false;
					if (useNext)
					{
						token = next;
						continue;
					}
				}
				else if(value.equals("FROM"))
				{
					nextIsTable = true;
				}
				else if (joinKeywords.contains(value))
				{
					nextIsTable = true;
				}
				else
				{
					nextIsTable = false;
				}
			}
			token = lexer.getNextToken(false, false);
		}
	}
}
