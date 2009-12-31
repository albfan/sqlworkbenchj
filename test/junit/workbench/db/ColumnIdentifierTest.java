/*
 * ColumnIdentifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Types;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import workbench.util.StringUtil;

public class ColumnIdentifierTest
	extends TestCase
{
	public ColumnIdentifierTest(String testName)
	{
		super(testName);
	}

	public void testAdjustQuotes()
	{
		QuoteHandler ansi = new QuoteHandler()
		{
			public boolean isQuoted(String name)
			{
				return name.startsWith("\"");
			}

			public String removeQuotes(String name)
			{
				if (isQuoted(name))
				{
					return StringUtil.trimQuotes(name);
				}
				return name;
			}

			public String quoteObjectname(String name)
			{
				if (isQuoted(name)) return name;
				return "\"" + name + "\"";
			}
		};


		QuoteHandler wrongQuoting = new QuoteHandler()
		{
			public boolean isQuoted(String name)
			{
				return name.startsWith("`");
			}

			public String removeQuotes(String name)
			{
				if (isQuoted(name))
				{
					return StringUtil.removeQuotes(name, "`");
				}
				return name;
			}

			public String quoteObjectname(String name)
			{
				if (isQuoted(name)) return name;
				return "`" + name + "`";
			}
		};

		QuoteHandler wrongQuoting2 = new QuoteHandler()
		{
			public boolean isQuoted(String name)
			{
				return name.startsWith("[") && name.contains("]");
			}

			public String removeQuotes(String name)
			{
				if (isQuoted(name))
				{
					String t = name.trim();
					return t.substring(1, t.length() - 1);
				}
				return name;
			}

			public String quoteObjectname(String name)
			{
				if (isQuoted(name)) return name;
				return "[" + name + "]";
			}
		};

		ColumnIdentifier col1 = new ColumnIdentifier("`Special Name`");
		col1.adjustQuotes(wrongQuoting, ansi);
		assertEquals("\"Special Name\"", col1.getColumnName());

		col1 = new ColumnIdentifier("noquotes");
		col1.adjustQuotes(wrongQuoting, ansi);
		assertEquals("noquotes", col1.getColumnName());

		col1 = new ColumnIdentifier("\"Need quote\"");
		col1.adjustQuotes(ansi, wrongQuoting);
		assertEquals("`Need quote`", col1.getColumnName());

		col1 = new ColumnIdentifier("[Very Wrong Quote]");
		col1.adjustQuotes(wrongQuoting2, ansi);
		assertEquals("\"Very Wrong Quote\"", col1.getColumnName());

	}

	public void testCopy()
	{
		ColumnIdentifier col = new ColumnIdentifier("mycol", Types.VARCHAR, true);
		ColumnIdentifier copy = col.createCopy();
		assertEquals("Copy not equals", true, col.equals(copy));
	}
	
	public void testSort()
	{
		ColumnIdentifier col1 = new ColumnIdentifier("one", Types.VARCHAR, true);
		col1.setPosition(1);

		ColumnIdentifier col2 = new ColumnIdentifier("two", Types.VARCHAR, true);
		col2.setPosition(2);

		ColumnIdentifier col3 = new ColumnIdentifier("three", Types.VARCHAR, true);
		col3.setPosition(3);

		ColumnIdentifier col4 = new ColumnIdentifier("four", Types.VARCHAR, true);
		col4.setPosition(4);

		ColumnIdentifier col5 = new ColumnIdentifier("five", Types.VARCHAR, true);
		col5.setPosition(5);

		ColumnIdentifier col6 = new ColumnIdentifier("six", Types.VARCHAR, true);
		col6.setPosition(6);
		
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
		cols.add(col3);
		cols.add(col6);
		cols.add(col2);
		cols.add(col5);
		cols.add(col1);
		cols.add(col4);
		ColumnIdentifier.sortByPosition(cols);
		for (int i=0; i < cols.size(); i++)
		{
			assertEquals("Wrong position in sorted list", i+1, cols.get(i).getPosition());
		}
		
	}

	public void testCompare()
	{
		ColumnIdentifier col1 = new ColumnIdentifier("mycol", Types.VARCHAR, true);
		ColumnIdentifier col2 = new ColumnIdentifier("\"mycol\"", Types.VARCHAR, true);
		assertEquals("Columns are not equal", true, col1.equals(col2));
		assertEquals("Columns are not equal", 0, col1.compareTo(col2));
		assertEquals("Columns are not equal", true, col1.hashCode() == col2.hashCode());
		
		col1 = new ColumnIdentifier("mycol", Types.VARCHAR, true);
		col2 = new ColumnIdentifier("MYCOL", Types.VARCHAR, true);
		assertEquals("Columns are not equal", true, col1.equals(col2));
		assertEquals("Columns are not equal", 0, col1.compareTo(col2));
		assertEquals("Columns are not equal", true, col1.hashCode() == col2.hashCode());
		
		col1 = new ColumnIdentifier("Pr\u00e4fix", Types.VARCHAR, true);
		col2 = new ColumnIdentifier("\"PR\u00c4FIX\"", Types.VARCHAR, true);
		assertEquals("Columns are not equal", true, col1.equals(col2));
		assertEquals("Columns are not equal", 0, col1.compareTo(col2));
		assertEquals("Columns are not equal", true, col1.hashCode() == col2.hashCode());
	}
	
}
