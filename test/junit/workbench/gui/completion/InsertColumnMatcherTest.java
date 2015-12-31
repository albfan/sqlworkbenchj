/*
 * InsertColumnMatcherTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.completion;

import java.util.List;

import workbench.WbTestCase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class InsertColumnMatcherTest
	extends WbTestCase
{

	public InsertColumnMatcherTest()
	{
		super("InsertColumnMatcherTest");
	}


	@Test
	public void testMerge()
	{
		String sql =
			"MERGE INTO foobar\n" +
			"USING\n" +
			"(\n" +
			"  SELECT a,\n" +
			"         b,\n" +
			"         c\n" +
			"  FROM foo\n" +
			"    JOIN bar USING (x)\n" +
			") t ON (t.a = foobar.x)\n" +
			"WHEN MATCHED THEN UPDATE\n" +
			"  SET y = t.b,\n" +
			"      z = t.c\n" +
			"WHEN NOT MATCHED THEN\n" +
			"  INSERT\n" +
			"    (x, y, z)\n" +
			"  VALUES\n" +
			"    (t.a, t.b, t.c);";

		InsertColumnMatcher matcher = new InsertColumnMatcher(sql);
		int pos = sql.indexOf("(t.a, t.b") + 1;
		String column = matcher.getTooltipForPosition(pos);
		assertEquals("x", column);
		pos = sql.indexOf("y, z)");
		String value = matcher.getTooltipForPosition(pos);
		assertEquals("t.b", value);
	}

	@Test
	public void testMultipleValues()
	{
		// multi-value inserts currently not supported
		String sql =
			"insert into person " +
			"  (id, firstname, lastname, birthday) \n" +
			"values \n" +
			"  (1, 'arthur', 'dent', DATE '1960-04-02'), \n" +
			"  (2, 'ford', 'prefect', DATE '1960-04-02'), \n" +
			"  (3, 'tricia', 'mcmillan', DATE '1960-04-02')";
		InsertColumnMatcher matcher = new InsertColumnMatcher(sql);
		int pos = sql.indexOf("'prefect',");
		String column = matcher.getTooltipForPosition(pos);
		assertEquals("lastname", column);

		sql =
		"insert into address (id, person_id, address_info)  \n" +
		"values \n" +
		"  (1, 1, 'foo'), \n" +
		"  (2, 1, 'bar'), \n" +
		"  (3, 2, 'foobar'), \n" +
		"  (4, 3, 'fubar')";

		matcher = new InsertColumnMatcher(sql);
		pos = sql.indexOf("1, 'bar");
		column = matcher.getTooltipForPosition(pos);
		assertEquals("person_id", column);

		pos = sql.indexOf("person_id,");
		String value = matcher.getTooltipForPosition(pos);
		assertEquals("[1, 1, 2, 3]", value);

		pos = sql.indexOf("id,");
		value = matcher.getTooltipForPosition(pos);
		assertEquals("[1, 2, 3, 4]", value);

		value = matcher.getValueForColumn("id");
		assertEquals("[1, 2, 3, 4]", value);

		sql =
		"insert into address (id, person_id, address_info)  \n" +
		"values \n" +
		"  (1, 1, 'foo'), \n" +
		"  (2, 1, ), \n" +
		"  (3, 2, 'foobar'), \n" +
		"  (4, 3, 'fubar')";

		matcher = new InsertColumnMatcher(sql);
		pos = sql.indexOf("address_info") + 2;
		value = matcher.getTooltipForPosition(pos);
		assertEquals("['foo', <No value>, 'foobar', 'fubar']", value);

		value = matcher.getValueForColumn("address_info");
		assertEquals("['foo', <No value>, 'foobar', 'fubar']", value);
	}

	@Test
	public void testGetColumns()
	{
		String sql =
			"insert into person (id, magic_value, firstname, lastname, birthday) \n" +
			"values \n" +
			"(1, my_function(4,2), 'arthur', 'dent', DATE '1960-04-02')";
		InsertColumnMatcher matcher = new InsertColumnMatcher(sql);
		List<String> columns = matcher.getColumns();
		assertNotNull(columns);
		assertEquals(5, columns.size());
		assertEquals("id", columns.get(0));
		assertEquals("magic_value", columns.get(1));
		assertEquals("firstname", columns.get(2));
		assertEquals("lastname", columns.get(3));
		assertEquals("birthday", columns.get(4));
		assertEquals("1", matcher.getValueForColumn("id"));
		assertEquals("'arthur'", matcher.getValueForColumn("firstname"));
		assertEquals("'dent'", matcher.getValueForColumn("lastname"));
		assertEquals("my_function(4,2)", matcher.getValueForColumn("magic_value"));
		assertEquals("DATE '1960-04-02'", matcher.getValueForColumn("birthday"));
		assertEquals(null, matcher.getTooltipForPosition(0));
		int pos = sql.indexOf("(id") + 1;
		assertEquals("1", matcher.getTooltipForPosition(pos));  // id value
		pos = sql.indexOf("lastname");
		assertEquals("'dent'", matcher.getTooltipForPosition(pos));
		pos = sql.indexOf("'arthur'") + 1;
		assertEquals("firstname", matcher.getTooltipForPosition(pos));
		pos = sql.indexOf("'dent'") - 1;
		assertEquals("lastname", matcher.getTooltipForPosition(pos));
		pos = sql.indexOf("birthday") + 1;
		assertEquals("DATE '1960-04-02'", matcher.getTooltipForPosition(pos));

		sql =
			"insert into person (id, magic_value, firstname, lastname, birthday, no_value) \n" +
			"values \n" +
			"(1, my_function(4,2), 'arthur', 'dent', DATE '1960-04-02')";
		matcher = new InsertColumnMatcher(sql);
		columns = matcher.getColumns();
		assertNotNull(columns);
		assertEquals(6, columns.size());
		assertEquals(null, matcher.getValueForColumn("no_value"));
		assertTrue(matcher.inColumnList(sql.indexOf("no_value") + 3));
		assertFalse(matcher.inValueList(sql.indexOf("no_value") + 3));
		assertTrue(matcher.inValueList(sql.indexOf("my_function") + 3));
		assertFalse(matcher.inColumnList(sql.indexOf("my_function") + 3));
	}

	@Test
	public void testSubselect()
	{
		String sql =
			"insert into person (id, magic_value, firstname, lastname, birthday, no_value) \n" +
			"select some_id, some_value, 'Arthur', 'Dent', current_date from my_table";
		InsertColumnMatcher matcher = new InsertColumnMatcher(sql);
		List<String> columns = matcher.getColumns();
		assertNotNull(columns);
		assertEquals(6, columns.size());
		assertEquals("some_id", matcher.getValueForColumn("id"));
		assertEquals("some_value", matcher.getValueForColumn("magic_value"));
		assertEquals("'Arthur'", matcher.getValueForColumn("firstname"));
		assertEquals("'Dent'", matcher.getValueForColumn("lastname"));
		assertEquals("current_date", matcher.getValueForColumn("birthday"));
		assertEquals(null, matcher.getValueForColumn("no_value"));
		assertEquals("magic_value", matcher.getTooltipForPosition(sql.indexOf("some_value,")));
		assertEquals(null, matcher.getTooltipForPosition(sql.indexOf("no_value")));

		sql =
			"INSERT INTO person \n" +
			"( \n" +
			"  id, \n" +
			"  value_column, \n" +
			"  firstname, \n" +
			"  lastname, \n" +
			"  birthday \n" +
			") \n" +
			"SELECT some_id, \n" +
			"       some_value, \n" +
			"       'Arthur', \n" +
			"       'Dent', \n" +
			"       CURRENT_DATE, \n" +
			"       foobar_value \n" +
			"FROM my_table \n" +
			" where not exists (select 1 from foobar)" ;

		matcher = new InsertColumnMatcher(sql);
		columns = matcher.getColumns();
		assertNotNull(columns);
		assertEquals(6, columns.size());
		assertEquals("some_id", matcher.getValueForColumn("id"));
		assertEquals("some_value", matcher.getValueForColumn("value_column"));
		assertEquals("'Arthur'", matcher.getValueForColumn("firstname"));
		assertEquals("'Dent'", matcher.getValueForColumn("lastname"));
		assertEquals("CURRENT_DATE", matcher.getValueForColumn("birthday"));
		assertEquals("value_column", matcher.getTooltipForPosition(sql.indexOf("some_value,")));
		assertEquals(null, matcher.getTooltipForPosition(sql.indexOf("foobar_value")));
		assertEquals("firstname", matcher.getTooltipForPosition(sql.indexOf("  'Arthur'")));

	}

	@Test
	public void testSelectWithCTE()
	{
		String sql =
			"insert into foo (col1, col2, col3) \n" +
			" -- generate some data \n" +
			"with cte1 as (\" +" +
			"  select x1, x2, x3 from some_table \n" +
			") \n" +
			"select c.x1, c.x2, f.col1 \n" +
			"from cte1 c \n " +
			"  join foo f on c.x3 = f.id;";

		InsertColumnMatcher matcher = new InsertColumnMatcher(sql);
		int pos = sql.indexOf(" c.x1") + 1;
		String column = matcher.getTooltipForPosition(pos);
		assertEquals("col1", column);

		pos = sql.indexOf("col2, col3") + 1;
		column = matcher.getTooltipForPosition(pos);
		assertEquals("c.x2", column);
	}

	@Test
	public void testEmptyValue()
	{
		String insert  = "insert into foo (c1, c2, c3, c4) values (10,20,   )";
		InsertColumnMatcher matcher = new InsertColumnMatcher(insert);
		int pos = insert.indexOf(" c2,") + 5;
		assertFalse(matcher.inValueList(pos));
		assertTrue(matcher.inColumnList(pos));
		assertNull(matcher.getTooltipForPosition(pos));
	}

}
