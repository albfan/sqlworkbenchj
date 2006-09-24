/*
 * HexBlobFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.ByteArrayOutputStream;
import junit.framework.*;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * @author support@sql-workbench.net
 */
public class HexBlobFormatterTest extends TestCase
{
	
	public HexBlobFormatterTest(String testName)
	{
		super(testName);
	}

	public void testGetBlobLiteral() throws Exception
	{
		HexBlobFormatter formatter = new HexBlobFormatter();
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
		
	}
	
}
