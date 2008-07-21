/*
 * ResultInfoTest.java
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
import workbench.db.ColumnIdentifier;

/**
 *
 * @author support@sql-workbench.net
 */
public class ResultInfoTest extends TestCase
{
	
	public ResultInfoTest(String testName)
	{
		super(testName);
	}

	public void testFindColumn()
	{
		try
		{
			ColumnIdentifier col1 = new ColumnIdentifier("\"KEY\"", java.sql.Types.VARCHAR, true);
			ColumnIdentifier col2 = new ColumnIdentifier("\"Main Cat\"", java.sql.Types.VARCHAR, false);
			ColumnIdentifier col3 = new ColumnIdentifier("firstname", java.sql.Types.VARCHAR, false);
			ResultInfo info = new ResultInfo(new ColumnIdentifier[] { col1, col2, col3 } );
			assertEquals(3, info.getColumnCount());
			assertEquals(true, info.hasPkColumns());

			int index = info.findColumn("key");
			assertEquals(0, index);

			index = info.findColumn("\"KEY\"");
			assertEquals(0, index);

			index = info.findColumn("\"key\"");
			assertEquals(0, index);

			index = info.findColumn("\"Main Cat\"");
			assertEquals(1, index);
			
			index = info.findColumn("firstname");
			assertEquals(2, index);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	
}
