/*
 * SQLLexerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.formatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import workbench.WbTestCase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SQLLexerTest
	extends WbTestCase
{

	public SQLLexerTest()
	{
		super("SQLLexerTest");
	}

	private List<SQLToken> getTokenList(String sql)
	{
		ArrayList<SQLToken> result = new ArrayList<>();

		SQLLexer l = new SQLLexer(sql);
		SQLToken t = l.getNextToken(false, false);
		while (t != null)
		{
			result.add(t);
			t = l.getNextToken(false, false);
		}
		return result;
	}

	@Test
	public void testNonStandardIdentifiers()
	{
		String sql = "select * from bar outer apply fn(x) st";
		List<SQLToken> l = getTokenList(sql);
		assertNotNull(l.get(4));
		assertTrue(l.get(4).isReservedWord());

		// Table variables
		sql = "select * from @@table_var";
		l = getTokenList(sql);
		assertEquals(4, l.size());
		assertEquals("@@table_var", l.get(3).getText());

		sql = "select * from @table_var";
		l = getTokenList(sql);
		assertEquals(4, l.size());
		assertEquals("@table_var", l.get(3).getText());

		// temp tables
		sql = "select * from #temp";
		l = getTokenList(sql);
		assertEquals(4, l.size());
		assertEquals("#temp", l.get(3).getText());

		sql = "select * from ##temp";
		l = getTokenList(sql);
		assertEquals(4, l.size());
		assertEquals("##temp", l.get(3).getText());
	}

	@Test
	public void testWbVar()
	{
		String sql = "select $[?wbvar] from table";
		List<SQLToken> l = getTokenList(sql);
		assertEquals(4, l.size());
		assertTrue(l.get(1).isWbVar());

		sql = "select ${wbvar} from '$[table]'";
		l = getTokenList(sql);
		assertEquals(4, l.size());
		assertTrue(l.get(0).isReservedWord());
		assertTrue(l.get(1).isWbVar());
		assertTrue(l.get(2).isReservedWord());
		assertTrue(l.get(3).isLiteral());

		sql = "select * from mytable where id=$[var_id]";
		l = getTokenList(sql);
		assertEquals(8, l.size());
		assertTrue(l.get(6).isOperator());
		assertTrue(l.get(7).isWbVar());
	}

	@Test
	public void testUnicode()
	{
		String sql = "insert into mytable (col1, col2, col3) values ('\u32A5\u0416','col2_value', 1234)";
		List<SQLToken> l = getTokenList(sql);
		assertEquals(18, l.size());
		assertEquals("'\u32A5\u0416'", l.get(12).getText());
		assertEquals("'col2_value'", l.get(14).getText());
		assertEquals("1234", l.get(16).getText());
	}

	@Test
	public void testQuotedIdentifier()
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

	@Test
	public void testLexer()
	{
		// Test if the multi-word keywords are detected properly

		String sql = "-- create a view\nCREATE\nOR\nREPLACE view my_view as (SELECT * FROM bal);";

		List<SQLToken> tokens = getTokenList(sql);

		SQLToken t = tokens.get(0);

		assertEquals(true, t.isReservedWord());
		assertEquals("CREATE OR REPLACE", t.getContents());

		sql = "SELECT * FROM bla INNER JOIN blub ON (x = y)";

		tokens = getTokenList(sql);
		t = tokens.get(0);
		assertEquals("SELECT", t.getContents());

		t = tokens.get(4);
		assertEquals("INNER JOIN", t.getContents());
		assertEquals(true, t.isReservedWord());

		sql = "SELECT * FROM bla INNER JOIN blub ON (x = y)\nOUTER JOIN blub2 on (y = y)";

		tokens = getTokenList(sql);
		t = tokens.get(0);
		assertEquals("SELECT", t.getContents());

		t = tokens.get(4);
		assertEquals("INNER JOIN", t.getContents());
		assertEquals(true, t.isReservedWord());

		t = tokens.get(12);
		assertEquals("OUTER JOIN", t.getContents());
		assertEquals(true, t.isReservedWord());

		sql = "SELECT * FROM bla LEFT OUTER JOIN blub ON (x = y)";

		tokens = getTokenList(sql);
		t = tokens.get(4);
		assertEquals("LEFT OUTER JOIN", t.getContents());
		assertTrue(t.isReservedWord());
	}

	@Test
	public void testMultilineLiterals()
		throws IOException
	{
		String sql = "values (\n   'line 1 \n x \n   line2;\n');";
		ArrayList<SQLToken> tokens = new ArrayList<>();

		SQLLexer l = new SQLLexer(sql);
		SQLToken t = null;
		while ((t = l.getNextToken()) != null)
		{
			tokens.add(t);
		}
		assertTrue(tokens.get(0).isReservedWord());
		assertTrue(tokens.get(1).isWhiteSpace());
		assertEquals("'line 1 \n x \n   line2;\n'", tokens.get(4).getText());
		assertTrue(tokens.get(4).isLiteral());
	}

	@Test
	public void testKeywords()
	{
		String sql = "union\nall \ngroup    by something\n"+
					"order\tby\n" +
					"create or replace package body \n" +
					"materialized view\n" +
					"start  with \n" +
				  "outer \t join \n" +
					"cross join  full join \t full\touter join\n" +
					"inner join\n"+
					"left join\n"+
					"left        outer join\n"+
					"right join\n" +
					"right \nouter\n\n join\njoin\n" +
			    "like";

		List<SQLToken> tokens = getTokenList(sql);
		for (int i = 0; i < tokens.size(); i++)
		{
			SQLToken t = tokens.get(i);
			if (i != 2) assertTrue(t.isReservedWord());
			String v = t.getContents();
			//System.out.println(i  + ": " + v);
			switch (i)
			{
				case 0:
					assertEquals("UNION ALL",v);
					break;
				case 1:
					assertEquals("GROUP BY",v);
					break;
				case 3:
					assertEquals("ORDER BY",v);
					break;
				case 4:
					assertEquals("CREATE OR REPLACE",v);
					break;
				case 5:
					assertEquals("PACKAGE BODY",v);
					break;
				case 6:
					assertEquals("MATERIALIZED VIEW",v);
					break;
				case 7:
					assertEquals("START WITH",v);
					break;
				case 8:
					assertEquals("OUTER JOIN",v);
					break;
				case 9:
					assertEquals("CROSS JOIN",v);
					break;
				case 10:
					assertEquals("FULL JOIN",v);
					break;
				case 11:
					assertEquals("FULL OUTER JOIN",v);
					break;
				case 12:
					assertEquals("INNER JOIN",v);
					break;
				case 13:
					assertEquals("LEFT JOIN",v);
					break;
				case 14:
					assertEquals("LEFT OUTER JOIN",v);
					break;
				case 15:
					assertEquals("RIGHT JOIN",v);
					break;
				case 16:
					assertEquals("RIGHT OUTER JOIN",v);
					break;
				case 17:
					assertEquals("JOIN",v);
					break;
				case 18:
					assertEquals("LIKE",v);
					break;
			}
		}
	}

	@Test
	public void testNumbers()
	{
		String sql = "-1";
		List<SQLToken> tokens = getTokenList(sql);
		assertEquals(1, tokens.size());
		assertEquals("-1", tokens.get(0).getText());
		assertTrue(tokens.get(0).isNumberLiteral());
		assertTrue(tokens.get(0).isIntegerLiteral());

		tokens = getTokenList("1234567890");
		assertEquals(1, tokens.size());
		assertEquals("1234567890", tokens.get(0).getText());
		assertTrue(tokens.get(0).isNumberLiteral());
		assertTrue(tokens.get(0).isIntegerLiteral());

		tokens = getTokenList("0.1E-12");
		assertEquals(1, tokens.size());
		assertEquals("0.1E-12", tokens.get(0).getText());
		assertTrue(tokens.get(0).isNumberLiteral());
		assertFalse(tokens.get(0).isIntegerLiteral());

		tokens = getTokenList("-1.1E+12");
		assertEquals(1, tokens.size());
		assertEquals("-1.1E+12", tokens.get(0).getText());
		assertTrue(tokens.get(0).isNumberLiteral());
		assertFalse(tokens.get(0).isIntegerLiteral());

		tokens = getTokenList("+0.123");
		assertEquals(1, tokens.size());
		assertEquals("+0.123", tokens.get(0).getText());
		assertTrue(tokens.get(0).isNumberLiteral());
		assertFalse(tokens.get(0).isIntegerLiteral());

	}


	@Test
	public void testQuotes()
		throws Exception
	{
		String sql = "call \"schema\".\"proc\"";
		List<SQLToken> tokens = getTokenList(sql);

		assertNotNull(tokens);
		assertEquals(4, tokens.size());
		assertEquals("\"schema\"", tokens.get(1).getText());
		assertEquals("\"proc\"", tokens.get(3).getText());

		sql = "call schema.proc";
		tokens = getTokenList(sql);
		assertNotNull(tokens);
		assertEquals(2, tokens.size());
		assertEquals("schema.proc", tokens.get(1).getText());
	}

	@Test
	public void testStringLiterals()
		throws Exception
	{
		String sql = "select 'arthur''s house' from some_table";
		List<SQLToken> tokens = getTokenList(sql);
		System.out.println(tokens);
		assertNotNull(tokens);
		assertEquals(4, tokens.size());
		assertTrue(tokens.get(0).isReservedWord());
		assertTrue(tokens.get(1).isLiteral());
		assertTrue(tokens.get(2).isReservedWord());
		assertTrue(tokens.get(3).isIdentifier());
		assertEquals("'arthur''s house'", tokens.get(1).getContents());
	}

}
