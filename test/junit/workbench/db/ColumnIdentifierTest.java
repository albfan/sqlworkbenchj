/*
 * ColumnIdentifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import workbench.util.StringUtil;

import org.junit.Test;

import static org.junit.Assert.*;

import workbench.util.SqlUtil;


public class ColumnIdentifierTest
{

	@Test
	public void testAdjustQuotes()
	{
		QuoteHandler wrongQuoting1 = new QuoteHandler()
		{
			@Override
			public boolean isQuoted(String name)
			{
				return name.startsWith("`");
			}

			@Override
			public String removeQuotes(String name)
			{
				if (isQuoted(name))
				{
					return StringUtil.removeQuotes(name, "`");
				}
				return name;
			}

			@Override
			public String quoteObjectname(String name)
			{
				if (isQuoted(name)) return name;
				return "`" + name + "`";
			}

      @Override
      public boolean isLegalIdentifier(String name)
      {
        Matcher m = SqlUtil.SQL_IDENTIFIER.matcher(name);
        return m.matches();
      }

			@Override
			public boolean needsQuotes(String name)
			{
				return name.indexOf(' ') > -1;
			}
		};

		QuoteHandler wrongQuoting2 = new QuoteHandler()
		{
			@Override
			public boolean isQuoted(String name)
			{
				return name.startsWith("[") && name.contains("]");
			}

			@Override
			public String removeQuotes(String name)
			{
				if (isQuoted(name))
				{
					String t = name.trim();
					return t.substring(1, t.length() - 1);
				}
				return name;
			}

			@Override
			public String quoteObjectname(String name)
			{
				if (isQuoted(name)) return name;
				return "[" + name + "]";
			}

			@Override
			public boolean needsQuotes(String name)
			{
				return name.indexOf(' ') > -1;
			}

      @Override
      public boolean isLegalIdentifier(String name)
      {
        Matcher m = SqlUtil.SQL_IDENTIFIER.matcher(name);
        return m.matches();
      }

		};

		ColumnIdentifier col1 = new ColumnIdentifier("`Special Name`");
		col1.adjustQuotes(wrongQuoting1, QuoteHandler.STANDARD_HANDLER);
		assertEquals("\"Special Name\"", col1.getColumnName());

		col1 = new ColumnIdentifier("noquotes");
		col1.adjustQuotes(wrongQuoting1, QuoteHandler.STANDARD_HANDLER);
		assertEquals("noquotes", col1.getColumnName());

		col1 = new ColumnIdentifier("\"Need quote\"");
		col1.adjustQuotes(QuoteHandler.STANDARD_HANDLER, wrongQuoting1);
		assertEquals("`Need quote`", col1.getColumnName());

		col1 = new ColumnIdentifier("[Very Wrong Quote]");
		col1.adjustQuotes(wrongQuoting2, QuoteHandler.STANDARD_HANDLER);
		assertEquals("\"Very Wrong Quote\"", col1.getColumnName());
	}

	@Test
	public void testCopy()
	{
		ColumnIdentifier col = new ColumnIdentifier("mycol", Types.VARCHAR, true);
		col.setComputedColumnExpression("other_col * 2");
		ColumnIdentifier copy = col.createCopy();
		assertEquals("Copy not equals", true, col.equals(copy));
		assertEquals("other_col * 2", copy.getComputedColumnExpression());

		col = new ColumnIdentifier("count(*)", Types.INTEGER, false);
		assertEquals("count(*)", col.getDisplayName());
		col.setColumnAlias("NUM_ORDERS");
		assertEquals("NUM_ORDERS", col.getDisplayName());
		assertEquals("count(*)", col.getColumnName());

		copy = col.createCopy();
		assertEquals("NUM_ORDERS", copy.getDisplayName());
		assertEquals("NUM_ORDERS", copy.getColumnAlias());

		col = new ColumnIdentifier("ID", Types.INTEGER, false);
		col.setDefaultValue("42");
		assertEquals("42", col.getDefaultValue());

		copy = col.createCopy();
		assertEquals("42", copy.getDefaultValue());

		col = new ColumnIdentifier("NON_NULL", Types.VARCHAR, true);
		col.setDbmsType("VARCHAR(50)");
		col.setIsNullable(false);

		copy = col.createCopy();
		assertEquals(col.isNullable(), copy.isNullable());

		col.setIsNullable(true);
		copy = col.createCopy();
		assertEquals(col.isNullable(), copy.isNullable());

		col.setPosition(35);
		copy = col.createCopy();
		assertEquals(col.getPosition(), copy.getPosition());
		assertEquals("DEFAULT", col.getDefaultClause());

		col.setDefaultClause("DEFAULT ON NULL");
		copy = col.createCopy();
		assertEquals(col.getDefaultClause(), copy.getDefaultClause());
	}

	@Test
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

	@Test
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
