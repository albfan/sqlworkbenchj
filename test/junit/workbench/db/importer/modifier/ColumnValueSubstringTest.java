/*
 * ColumnValueSubstringTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer.modifier;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnValueSubstringTest
{
	public ColumnValueSubstringTest()
	{
	}

	@Test
	public void testGetSubstring()
	{
		ColumnValueSubstring sub = new ColumnValueSubstring(5, 20);
		String s = sub.getSubstring("1");
		assertEquals("1", s);
		
		s = sub.getSubstring("1234567890");
		assertEquals("67890", s);
		
		s = sub.getSubstring("123456789012345678901234567890");
		assertEquals("678901234567890", s);
		
	}
}
