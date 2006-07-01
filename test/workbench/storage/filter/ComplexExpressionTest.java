/*
 * FilterTest.java
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

import java.util.HashMap;
import java.util.Map;
import workbench.util.WbPersistence;
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
}
