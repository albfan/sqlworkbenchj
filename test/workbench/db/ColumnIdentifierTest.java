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

/*
 * TableIdentifierTest.java
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
	
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
	
}
