/*
 * RowDataTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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

	public void testBlobs()
	{
		RowData row = new RowData(2);
		row.setValue(0, new Integer(1));
		row.setValue(1, new byte[] {1,2,3});
		row.resetStatus();
		
		row.setValue(1, new byte[] {1,2,3});
		assertFalse(row.isColumnModified(1));
		assertFalse(row.isModified());
	}
	
	public void testResetStatus()
	{
		RowData row = new RowData(2);
		row.setValue(0, new Integer(42));
		row.setValue(1, "Test");
		row.resetStatus();

		row.setValue(0, new Integer(43));
		row.setValue(1, "Test2");
		assertTrue(row.isModified());
		
		row.resetStatusForColumn(1);
		assertTrue(row.isModified());
		assertTrue(row.isColumnModified(0));
		assertFalse(row.isColumnModified(1));
		
		row.resetStatusForColumn(0);
		assertFalse(row.isColumnModified(0));
		assertFalse(row.isColumnModified(1));
		assertFalse(row.isModified());
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
		
		row.setValue(0, "123");
		row.setValue(1, null);
		row.resetStatus();
		row.setValue(1, null);
		assertFalse(row.isModified());
	}
}
