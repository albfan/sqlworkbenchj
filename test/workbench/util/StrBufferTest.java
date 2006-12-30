/*
 * StrBufferTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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

	/** Creates a new instance of StrBufferTest */
	public StrBufferTest()
	{
	}

	public void testBuffer()
	{
		StrBuffer buffer = new StrBuffer("0123456789");
		buffer.remove(5);
		buffer.remove(5);
		assertEquals("Remove not working", "01234789", buffer.toString());
	}


}
