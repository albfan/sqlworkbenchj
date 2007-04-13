/*
 * ColumnIdentifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Types;

import junit.framework.*;

public class ColumnIdentifierTest
	extends TestCase
{
	public ColumnIdentifierTest(String testName)
	{
		super(testName);
	}

	
	public void testCopy()
	{
		ColumnIdentifier col = new ColumnIdentifier("mycol", Types.VARCHAR, true);
		ColumnIdentifier copy = col.createCopy();
		assertEquals("Copy not equals", true, col.equals(copy));
		
	}

	public void testCompare()
	{
		ColumnIdentifier col1 = new ColumnIdentifier("mycol", Types.VARCHAR, true);
		ColumnIdentifier col2 = new ColumnIdentifier("\"mycol\"", Types.VARCHAR, true);
		assertEquals("Columns are not equal", true, col1.equals(col2));
		assertEquals("Columns are not equal", 0, col1.compareTo(col2));
		assertEquals("Columns are not equal", true, col1.hashCode() == col2.hashCode());
		
		col1 = new ColumnIdentifier("mycol", Types.VARCHAR, true);
		col2 = new ColumnIdentifier("MYCOL", Types.VARCHAR, true);
		assertEquals("Columns are not equal", true, col1.equals(col2));
		assertEquals("Columns are not equal", 0, col1.compareTo(col2));
		assertEquals("Columns are not equal", true, col1.hashCode() == col2.hashCode());
		
		col1 = new ColumnIdentifier("Pr\u00e4fix", Types.VARCHAR, true);
		col2 = new ColumnIdentifier("\"PR\u00c4FIX\"", Types.VARCHAR, true);
		assertEquals("Columns are not equal", true, col1.equals(col2));
		assertEquals("Columns are not equal", 0, col1.compareTo(col2));
		assertEquals("Columns are not equal", true, col1.hashCode() == col2.hashCode());

	}
	
}
