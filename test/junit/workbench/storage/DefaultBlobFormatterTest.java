/*
 * DefaultBlobFormatterTest.java
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

import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;

/**
 * @author Thomas Kellerer
 */
public class DefaultBlobFormatterTest
	extends TestCase
{
	public DefaultBlobFormatterTest(String testName)
	{
		super(testName);
	}

	public void testGetBlobLiteral()
		throws Exception
	{
		DefaultBlobFormatter formatter = new DefaultBlobFormatter();
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		b.write(255);
		b.write(0);
		b.write(16);
		b.write(15);
		byte[] blob = b.toByteArray();
		String literal = formatter.getBlobLiteral(blob);

		assertEquals("Wrong literal created", "ff00100f", literal);

		formatter.setPrefix("0x");
		formatter.setSuffix(null);
		literal = formatter.getBlobLiteral(blob);
		assertEquals("Wrong literal created", "0xff00100f", literal);

		formatter.setPrefix("'");
		formatter.setSuffix("'");
		literal = formatter.getBlobLiteral(blob);
		assertEquals("Wrong literal created", "'ff00100f'", literal);

		formatter.setPrefix("X'");
		formatter.setSuffix("'");
		formatter.setUseUpperCase(true);
		literal = formatter.getBlobLiteral(blob);
		assertEquals("Wrong literal created", "X'FF00100F'", literal);

		formatter.setUseUpperCase(true);
		formatter.setPrefix("to_lob(utl_raw.cast_to_raw('0x");
		formatter.setSuffix("'))");
		literal = formatter.getBlobLiteral(blob);
		assertEquals("Wrong literal created", "to_lob(utl_raw.cast_to_raw('0xFF00100F'))", literal);
		
		formatter.setUseUpperCase(false);
		formatter.setPrefix(null);
		formatter.setSuffix(null);
		formatter.setLiteralType(BlobLiteralType.octal);
		literal = formatter.getBlobLiteral(blob);
		assertEquals("Wrong literal created", "\\377\\000\\020\\017", literal);
	}
}
