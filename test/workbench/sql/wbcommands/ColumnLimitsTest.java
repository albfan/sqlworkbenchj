/*
 * ColumnLimitsTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.util.Map;
import junit.framework.TestCase;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author support@sql-workbench.net
 */
public class ColumnLimitsTest extends TestCase
{
	
	public ColumnLimitsTest(String testName)
	{
		super(testName);
	}
	
	public void testGetLimits()
	{
		System.out.println("getLimits");
		ColumnLimits instance = new ColumnLimits("column1 = 20 , column2 = 40");
		Map<ColumnIdentifier, Integer> limits = instance.getLimits();
		ColumnIdentifier c1 = new ColumnIdentifier("COLUMN1");
		Integer s1 = limits.get(c1);
		assertNotNull("No max length found", s1);
		assertEquals("Wrong max length", 20, s1.intValue());

		ColumnIdentifier c2 = new ColumnIdentifier("COLUMN2");
		Integer s2 = limits.get(c2);
		assertNotNull("No max length found", s2);
		assertEquals("Wrong max length", 40, s2.intValue());
		
	}
	
}
