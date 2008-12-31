/*
 * StrBufferTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import junit.framework.*;

/**
 *
 * @author  support@sql-workbench.net
 */
public class StrBufferTest
	extends TestCase
{

	public void testRemove()
	{
		StrBuffer buffer = new StrBuffer("0123456789");
		buffer.remove(5);
		buffer.remove(5);
		assertEquals("Remove not working", "01234789", buffer.toString());

		buffer = new StrBuffer("0123456789");
		buffer.remove(1, 4);
		assertEquals("Remove not working", "0456789", buffer.toString());
	}


	public void testAppend()
	{
		StrBuffer buffer = new StrBuffer("0");
		buffer.append("1");
		assertEquals("append not working", "01", buffer.toString());

		buffer.append(new StringBuilder("2"));
		assertEquals("append not working", "012", buffer.toString());

		buffer.append(null);
		assertEquals("append not working", "012", buffer.toString());
	}

}
