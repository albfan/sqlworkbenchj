/*
 * ColumnExpressionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class ColumnExpressionTest
	extends TestCase
{

	public ColumnExpressionTest(String testName)
	{
		super(testName);
	}

	public void testComparators()
	{
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", "Beeblebrox");
		values.put("age", new Integer(43));
		values.put("spaceship", null);

		ColumnExpression col = new ColumnExpression("firstname", new IsNotNullComparator(), null);
		assertTrue(col.evaluate(values));

		col = new ColumnExpression("spaceship", new IsNullComparator(), null);
		assertTrue(col.evaluate(values));
		col = new ColumnExpression("firstname", new IsNullComparator(), null);
		assertFalse(col.evaluate(values));
		
		col = new ColumnExpression("spaceship", new IsNotNullComparator(), null);
		assertFalse(col.evaluate(values));
		col = new ColumnExpression("firstname", new IsNotNullComparator(), null);
		assertTrue(col.evaluate(values));

		col = new ColumnExpression("age", new LessOrEqualComparator(), new Integer(43));
		assertTrue(col.evaluate(values));
		col = new ColumnExpression("age", new LessOrEqualComparator(), new Integer(10));

		assertFalse(col.evaluate(values));
		col = new ColumnExpression("age", new LessThanComparator(), new Integer(100));
		assertTrue(col.evaluate(values));
		col = new ColumnExpression("age", new LessThanComparator(), new Integer(43));
		assertFalse(col.evaluate(values));

		col = new ColumnExpression("age", new NumberNotEqualsComparator(), new Integer(100));
		assertTrue(col.evaluate(values));
		col = new ColumnExpression("age", new NumberNotEqualsComparator(), new Integer(43));
		assertFalse(col.evaluate(values));

		col = new ColumnExpression("age", new NumberEqualsComparator(), new Integer(43));
		assertTrue(col.evaluate(values));
		col = new ColumnExpression("age", new NumberEqualsComparator(), new Integer(44));
		assertFalse(col.evaluate(values));
		
		col = new ColumnExpression("firstname", new NotStartsWithComparator(), "Tricia");
		col.setIgnoreCase(true);
		assertTrue(col.evaluate(values));

		col = new ColumnExpression("firstname", new NotStartsWithComparator(), "Zaphod");
		col.setIgnoreCase(false);
		assertTrue(col.evaluate(values));

		col.setIgnoreCase(true);
		assertFalse(col.evaluate(values));
		
		col = new ColumnExpression("firstname", new ContainsComparator(), "Pho");
		col.setIgnoreCase(true);
		assertTrue(col.evaluate(values));
		col.setIgnoreCase(false);
		assertFalse(col.evaluate(values));
		
		col = new ColumnExpression("firstname", new StringEqualsComparator(), "Zaphod");
		col.setIgnoreCase(true);
		assertTrue(col.evaluate(values));
		col.setIgnoreCase(false);
		assertFalse(col.evaluate(values));

		col = new ColumnExpression("firstname", new StringEqualsComparator(), "zaphod");
		col.setIgnoreCase(false);
		assertTrue(col.evaluate(values));
		col.setIgnoreCase(true);
		assertTrue(col.evaluate(values));
		
		col = new ColumnExpression("firstname", new StringNotEqualsComparator(), "Zaphod");
		col.setIgnoreCase(false);
		assertTrue(col.evaluate(values));
		col.setIgnoreCase(true);
		assertFalse(col.evaluate(values));
	}

	public void testDateComparison()
		throws Exception
	{
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("changed_on", f.parse("2006-11-01"));
		
		ColumnExpression expr = new ColumnExpression("changed_on", new GreaterThanComparator(), f.parse("2006-10-01"));
		assertTrue(expr.evaluate(values));

		expr = new ColumnExpression("changed_on", new DateEqualsComparator(), f.parse("2006-11-01"));
		assertTrue(expr.evaluate(values));

		expr = new ColumnExpression("changed_on", new DateEqualsComparator(), f.parse("2006-11-02"));
		assertFalse(expr.evaluate(values));

		expr = new ColumnExpression("changed_on", new LessThanComparator(), f.parse("2006-11-02"));
		assertTrue(expr.evaluate(values));
		
		expr = new ColumnExpression("changed_on", new LessThanComparator(), f.parse("2006-10-20"));
		assertFalse(expr.evaluate(values));
	}
	
}
