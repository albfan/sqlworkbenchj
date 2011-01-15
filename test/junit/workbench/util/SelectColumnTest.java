/*
 * SelectColumnTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectColumnTest
{

	@Test
	public void testGetColumnTable()
	{
		SelectColumn col = new SelectColumn("first_name");
		assertNull(col.getColumnTable());

		col = new SelectColumn("p.first_name as fname");
		assertEquals("p", col.getColumnTable());
		assertEquals("first_name", col.getObjectName());

		col = new SelectColumn("p.first_name");
		assertEquals("p", col.getColumnTable());

		col = new SelectColumn("myschema.mytable.first_name");
		assertEquals("myschema.mytable", col.getColumnTable());

		col = new SelectColumn("\"MySchema\".\"MyTable\".first_name");
		assertEquals("\"MySchema\".\"MyTable\"", col.getColumnTable());

	}
}
