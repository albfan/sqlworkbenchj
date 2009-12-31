/*
 * ValueDisplayTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class ValueDisplayTest
	extends TestCase
{

	public ValueDisplayTest(String testName)
	{
		super(testName);
	}

	public void testToString()
	{
		ValueDisplay value = new ValueDisplay((new Object[] { "one", "two", new Integer(42)} ));
		assertEquals("{[one],[two],[42]}", value.toString());
	}
}
