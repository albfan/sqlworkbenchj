/*
 * RowDataTest.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class RowDataTest extends TestCase
{

	public RowDataTest(String testName)
	{
		super(testName);
	}
	
	public void testChangeValues()
	{
		RowData row = new RowData(2);
		assertTrue(row.isNew());
		
		row.setValue(0, "123");
		row.setValue(1, new Integer(42));
		row.resetStatus();
		assertFalse(row.isModified());
		
		Object value = row.getValue(0);
		assertEquals(value, "123");
		value = row.getValue(1);
		assertEquals(value, new Integer(42));
		
		row.setValue(0, null);
		value = row.getValue(0);
		assertNull(value);
		assertEquals("123", row.getOriginalValue(0));
		assertTrue(row.isModified());
		
		row.resetStatus();
		row.setValue(0, "456");
		value = row.getValue(0);
		assertEquals(value, "456");
		assertNull(row.getOriginalValue(0));
		assertTrue(row.isColumnModified(0));
	}
}
