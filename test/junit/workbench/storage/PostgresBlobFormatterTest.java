/*
 * PostgresBlobFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;

/**
 * @author support@sql-workbench.net
 */
public class PostgresBlobFormatterTest
	extends TestCase
{
	public PostgresBlobFormatterTest(String testName)
	{
		super(testName);
	}

	public void testGetBlobLiteral()
		throws Exception
	{
		PostgresBlobFormatter formatter = new PostgresBlobFormatter();
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		b.write(255);
		b.write(0);
		b.write(16);
		b.write(15);
		byte[] blob = b.toByteArray();
		String literal = formatter.getBlobLiteral(blob).toString();

		assertEquals("Wrong literal created", "'\\\\377\\\\000\\\\020\\\\017'", literal);
	}
}
