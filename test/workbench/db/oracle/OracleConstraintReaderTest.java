/*
 * OracleConstraintReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import junit.framework.TestCase;

/**
 *
 * @author tkellerer
 */
public class OracleConstraintReaderTest extends TestCase
{
	
	public OracleConstraintReaderTest(String testName)
	{
		super(testName);
	}
	
	protected void setUp() throws Exception
	{
		super.setUp();
	}
	
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
	
	public void testIsDefaultNNConstraint()
	{
		OracleConstraintReader instance = new OracleConstraintReader();
		String definition = "\"MY_COL\" IS NOT NULL";
		boolean result = instance.isDefaultNNConstraint(definition);
		assertEquals("Default NN not recognized", true, result);
		
		definition = "\"MY_COL\" IS NOT NULL OR COL2 IS NOT NULL";
		result = instance.isDefaultNNConstraint(definition);
		assertEquals("Default NN not recognized", false, result);
		
	}
	
}
