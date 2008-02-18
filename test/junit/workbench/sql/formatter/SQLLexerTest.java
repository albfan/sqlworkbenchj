/*
 * SQLLexerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.formatter;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class SQLLexerTest extends TestCase
{
	
	public SQLLexerTest(String testName)
	{
		super(testName);
	}
	
	private List getTokenList(String sql)
	{
		ArrayList result = new ArrayList();
		
		SQLLexer l = new SQLLexer(sql);
		SQLToken t = l.getNextToken(false, false);
		while (t != null)
		{
			result.add(t);
			t = l.getNextToken(false, false);
		}
		return result;
	}

	public void testQuotedIdentifier()
	{
		try
		{
			String sql = "Select \"one AND two\" from thetable;";
			SQLLexer l = new SQLLexer(sql);
			SQLToken select = l.getNextToken(false, false);
			
			assertEquals(select.getContents(), "SELECT");
			SQLToken col = l.getNextToken(false, false);
			assertEquals("\"one AND two\"", col.getContents());
			
			
			sql = "WbExport -file=\"c:\\Documents and Settings\\test.txt\" -type=text";
			l = new SQLLexer(sql);
			SQLToken t = l.getNextToken(false, false);
			assertEquals("WBEXPORT", t.getText().toUpperCase());
			t = l.getNextToken(false, false);
			assertEquals("-", t.getText());
			t = l.getNextToken(false, false);
			assertEquals("file", t.getText());
			t = l.getNextToken(false, false);
			assertEquals("=", t.getText());
			t = l.getNextToken(false, false);
			assertEquals("\"c:\\Documents and Settings\\test.txt\"", t.getContents());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testLexer()
	{
		// Test if the multi-word keywords are detected properly
		
		String sql = "-- create a view\nCREATE\nOR\nREPLACE view my_view as (SELECT * FROM bal);";
		
		List tokens = getTokenList(sql);
		
		SQLToken t = (SQLToken)tokens.get(0);
		
		assertEquals("CREATE OR REPLACE", t.getContents());
		assertEquals(true, t.isReservedWord());
		
		sql = "SELECT * FROM bla INNER JOIN blub ON (x = y)";
		
		tokens = getTokenList(sql);
		t = (SQLToken)tokens.get(0);
		assertEquals("SELECT", t.getContents());
		
		t = (SQLToken)tokens.get(4);
		assertEquals("INNER JOIN", t.getContents());
		assertEquals(true, t.isReservedWord());
	
		sql = "SELECT * FROM bla INNER JOIN blub ON (x = y)\nOUTER JOIN blub2 on (y = y)";
		
		tokens = getTokenList(sql);
		t = (SQLToken)tokens.get(0);
		assertEquals("SELECT", t.getContents());
		
		t = (SQLToken)tokens.get(4);
		assertEquals("INNER JOIN", t.getContents());
		assertEquals(true, t.isReservedWord());
		
		t = (SQLToken)tokens.get(12);
		assertEquals("OUTER JOIN", t.getContents());
		assertEquals(true, t.isReservedWord());

		sql = "is null\nnot null\n--next\nunion\nall\n--next\n" +
			    "group    by\n"+
					"order\tby\n" + 
					"create or replace package body \n" + 
					"materialized view\n" + 
					"start  with\n" + 
				  "outer \t join\n" + 
					"cross join  full join \t full\touter join\n" + 
					"inner join\n"+ 
					"left join\n"+
					"left        outer join\n"+
					"right join\n" + 
					"right \nouter\n\n join\njoin\n" + 
					"is not null";
		tokens = getTokenList(sql);
		for (int i = 0; i < tokens.size(); i++)
		{
			t = (SQLToken)tokens.get(i);
			String v = t.getContents();
			//System.out.println(i  + ": " + v);
			switch (i)
			{
				case 0:
					assertEquals("IS NULL",v);
					break;
				case 1:
					assertEquals("NOT NULL",v);
					break;
				case 2:
					assertEquals("UNION ALL",v);
					break;
				case 3:
					assertEquals("GROUP BY",v);
					break;
				case 4:
					assertEquals("ORDER BY",v);
					break;
				case 5:
					assertEquals("CREATE OR REPLACE",v);
					break;
				case 6:
					assertEquals("PACKAGE BODY",v);
					break;
				case 7:
					assertEquals("MATERIALIZED VIEW",v);
					break;
				case 8:
					assertEquals("START WITH",v);
					break;
				case 9:
					assertEquals("OUTER JOIN",v);
					break;
				case 10:
					assertEquals("CROSS JOIN",v);
					break;
				case 11:
					assertEquals("FULL JOIN",v);
					break;
				case 12:
					assertEquals("FULL OUTER JOIN",v);
					break;
				case 13:
					assertEquals("INNER JOIN",v);
					break;
				case 14:
					assertEquals("LEFT JOIN",v);
					break;
				case 15:
					assertEquals("LEFT OUTER JOIN",v);
					break;
				case 16:
					assertEquals("RIGHT JOIN",v);
					break;
				case 17:
					assertEquals("RIGHT OUTER JOIN",v);
					break;
				case 18:
					assertEquals("JOIN",v);
					break;
				case 19:
					assertEquals("IS NOT NULL",v);
					break;
			}
			
		}
	}
	
	
}
