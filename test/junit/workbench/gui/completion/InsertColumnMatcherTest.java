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

	}

}
