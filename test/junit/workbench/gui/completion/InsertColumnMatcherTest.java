/*
 * InsertColumnMatcherTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.completion;

import java.util.List;
import org.junit.Test;
import workbench.WbTestCase;
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
			"FROM my_table";

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
}
