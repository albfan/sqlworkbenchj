/*
 * ColumnRemoverTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.sql.Types;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnRemoverTest
	extends WbTestCase
{
	public ColumnRemoverTest(String testName)
	{
		super(testName);
	}

	public void testRemoveColumns()
	{
		String[] cols = new String[] {"NAME", "TYPE", "CATALOG", "SCHEMA", "REMARKS"};
		int types[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		int sizes[] = {30,12,10,10,20};

		DataStore ds = new DataStore(cols, types, sizes);
		int row = ds.addRow();
		ds.setValue(row, 0, "Name");
		ds.setValue(row, 1, "Type");
		ds.setValue(row, 2, "some_cat");
		ds.setValue(row, 3, "some_schema");
		ds.setValue(row, 4, "my comment");

		ColumnRemover remove = new ColumnRemover(ds);
		DataStore newDs = remove.removeColumnsByName("CATALOG", "SCHEMA");
		assertEquals(1, newDs.getRowCount());

		assertFalse(newDs.isModified());
		assertEquals(-1, newDs.getColumnIndex("CATALOG"));
		assertEquals(-1, newDs.getColumnIndex("SCHEMA"));
		assertEquals(0, newDs.getColumnIndex("NAME"));
		assertEquals(1, newDs.getColumnIndex("TYPE"));
		assertEquals(2, newDs.getColumnIndex("REMARKS"));

		assertEquals("Name", newDs.getValue(0, 0));
		assertEquals("Name", newDs.getValue(0, "NAME"));

		assertEquals("my comment", newDs.getValue(0, "REMARKS"));
		assertEquals("my comment", newDs.getValue(0, 2));

		assertEquals("Type", newDs.getValue(0, "TYPE"));
		assertEquals("Type", newDs.getValue(0, 1));
	}
}
