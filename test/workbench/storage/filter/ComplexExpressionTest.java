/*
 * ComplexExpressionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import junit.framework.*;

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
	
	public void testFilter()
	{
		try
		{
			ComplexExpression expr = new AndExpression();
			expr.addColumnExpression("firstname", new StringEqualsComparator(), "Zaphod");
			expr.addColumnExpression("lastname", new StartsWithComparator(), "Bee");
			expr.addColumnExpression("age", new GreaterOrEqualComparator(), new Integer(42));
			
			Map values = new HashMap();
			values.put("firstname", "zaphod");
			values.put("lastname", "Beeblebrox");
			values.put("age", new Integer(43));
			assertEquals(true, expr.evaluate(values));

			values = new HashMap();
			values.put("firstname", "zaphod");
			values.put("lastname", "Beeblebrox");
			values.put("age", new Integer(40));			
			assertEquals(false, expr.evaluate(values));
			
			values = new HashMap();
			values.put("firstname", "zaphod");
			values.put("lastname", null);
			values.put("age", new Integer(40));			
			
			expr = new AndExpression();
			expr.addColumnExpression("lastname", new IsNullComparator(), null);
			assertEquals(true, expr.evaluate(values));
			
		}
		catch (Throwable th)
		{
			fail();
		}
	}
	
	public void testDateComparison()
	{
		try
		{
			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
			ComplexExpression expr = new AndExpression();
			expr.addColumnExpression("changed_on", new GreaterThanComparator(), f.parse("2006-10-01"));
			
			Map values = new HashMap();
			values.put("changed_on", f.parse("2006-11-01"));
			assertEquals(true, expr.evaluate(values));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
