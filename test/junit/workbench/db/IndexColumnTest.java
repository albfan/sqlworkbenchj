/*
 * IndexColumnTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class IndexColumnTest
{

	public IndexColumnTest()
	{
	}

	@Test
	public void testEquals()
	{
		IndexColumn col1 = new IndexColumn("name", "ASC");
		IndexColumn col2 = new IndexColumn("name", "asc");
		assertTrue(col1.equals(col2));
		assertTrue(col2.equals(col1));

		col1 = new IndexColumn("name", null);
		col2 = new IndexColumn("name", null);
		assertTrue(col1.equals(col2));
		assertTrue(col2.equals(col1));

		col1 = new IndexColumn("name", "asc");
		col2 = new IndexColumn("name", "desc");
		assertFalse(col1.equals(col2));
		assertFalse(col2.equals(col1));
	}


}
