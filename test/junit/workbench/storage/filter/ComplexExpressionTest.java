/*
 * ComplexExpressionTest.java
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

import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

/**
 * @author support@sql-workbench.net
 */
public class ComplexExpressionTest
	extends TestCase
{
	public ComplexExpressionTest(String name)
	{
		super(name);
	}

	public void testOrExpression()
	{
		ComplexExpression expr = new OrExpression();
		expr.addColumnExpression("firstname", new ContainsComparator(), "pho", true);
		expr.addColumnExpression("lastname", new ContainsComparator(), "ble", true);

		Map<String, Object> values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", "Beeblebrox");
		values.put("age", new Integer(43));
		values.put("spaceship", null);
		assertTrue(expr.evaluate(values));
	}
	
	public void testAndExpression()
		throws Exception
	{
		ComplexExpression expr = new AndExpression();
		expr.addColumnExpression("firstname", new StringEqualsComparator(), "Zaphod");
		expr.addColumnExpression("lastname", new StartsWithComparator(), "Bee");
		expr.addColumnExpression("age", new GreaterOrEqualComparator(), new Integer(42));

		Map<String, Object> values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", "Beeblebrox");
		values.put("age", new Integer(43));
		assertTrue(expr.evaluate(values));

		values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", "Beeblebrox");
		values.put("age", new Integer(40));
		assertFalse(expr.evaluate(values));

		values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", null);
		values.put("age", new Integer(40));

		expr = new AndExpression();
		expr.addColumnExpression("lastname", new IsNullComparator(), null);
		assertTrue(expr.evaluate(values));
	}
	
}
