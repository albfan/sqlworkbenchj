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

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

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
	
	public void testSort()
	{
		ColumnIdentifier col1 = new ColumnIdentifier("one", Types.VARCHAR, true);
		col1.setPosition(1);

		ColumnIdentifier col2 = new ColumnIdentifier("two", Types.VARCHAR, true);
		col2.setPosition(2);

		ColumnIdentifier col3 = new ColumnIdentifier("three", Types.VARCHAR, true);
		col3.setPosition(3);

		ColumnIdentifier col4 = new ColumnIdentifier("four", Types.VARCHAR, true);
		col4.setPosition(4);

		ColumnIdentifier col5 = new ColumnIdentifier("five", Types.VARCHAR, true);
		col5.setPosition(5);

		ColumnIdentifier col6 = new ColumnIdentifier("six", Types.VARCHAR, true);
		col6.setPosition(6);
		
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
		cols.add(col3);
		cols.add(col6);
		cols.add(col2);
		cols.add(col5);
		cols.add(col1);
		cols.add(col4);
		ColumnIdentifier.sortByPosition(cols);
		for (int i=0; i < cols.size(); i++)
		{
			assertEquals("Wrong position in sorted list", i+1, cols.get(i).getPosition());
		}
		
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
