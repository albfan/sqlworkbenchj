/*
 * RowDataListTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Thomas Kellerer
 */
public class RowDataListTest
	extends WbTestCase
{

	public RowDataListTest()
	{
		super("RowDataListTest");
	}

	@Test
	public void testAdd()
	{
		RowDataList list = new RowDataList();
		list.reset();

		// Make sure add() still works properly after calling reset()
		RowData row = new RowData(2);
		row.setValue(0, "Test");
		row.setValue(1, new Integer(42));
		list.add(row);
		RowData r = list.get(0);
		assertNotNull(r);
		assertEquals(r.getValue(0), "Test");
		assertEquals(r.getValue(1), new Integer(42));
	}
}
