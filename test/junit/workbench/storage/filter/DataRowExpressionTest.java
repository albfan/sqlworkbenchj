/*
 * DataRowExpressionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
 *
 * @author support@sql-workbench.net
 */
public class DataRowExpressionTest extends TestCase {
    
	public DataRowExpressionTest(String testName) 
	{
		super(testName);
	}

	public void testEvaluate()
		throws Exception
	{
		DataRowExpression expr = new DataRowExpression(new ContainsComparator(), "Zapho");
		expr.setIgnoreCase(true);
		assertTrue(expr.isIgnoreCase());
		
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", "Beeblebrox");
		values.put("age", new Integer(43));
		values.put("spaceship", null);

		assertTrue(expr.evaluate(values));

		expr.setIgnoreCase(false);
		assertFalse(expr.isIgnoreCase());
		
		assertFalse(expr.evaluate(values));

		expr = new DataRowExpression(new ContainsComparator(), "Arthur");
		expr.setIgnoreCase(true);
		assertFalse(expr.evaluate(values));
		expr.setIgnoreCase(false);
		assertFalse(expr.evaluate(values));
	}

}
