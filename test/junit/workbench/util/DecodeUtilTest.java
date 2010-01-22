/*
 * DecodeUtilTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import junit.framework.TestCase;
import workbench.storage.BlobFormatterFactory;
import workbench.storage.BlobLiteralFormatter;
import workbench.storage.BlobLiteralType;
import workbench.storage.RowData;

/**
 *
 * @author Thomas Kellerer
 */
public class DecodeUtilTest
	extends TestCase
{

	public DecodeUtilTest(String testName)
	{
		super(testName);
	}

	public void testDecodeString()
		throws Exception
	{
		BlobDecoder instance = new BlobDecoder();
		byte[] data = new byte[]
		{
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17
		};

		String base64 = Base64.encodeBytes(data);
		byte[] result = instance.decodeString(base64, BlobLiteralType.base64);
		assertTrue(RowData.objectsAreEqual(data, result));

		BlobLiteralFormatter octalFormat = BlobFormatterFactory.createInstance(BlobLiteralType.octal);
		String octal = octalFormat.getBlobLiteral(data).toString();
		result = instance.decodeString(octal, BlobLiteralType.octal);
		assertTrue(RowData.objectsAreEqual(data, result));

		BlobLiteralFormatter hexFormat = BlobFormatterFactory.createInstance(BlobLiteralType.hex);
		String hex = hexFormat.getBlobLiteral(data).toString();
		result = instance.decodeString(hex, BlobLiteralType.hex);
		assertTrue(RowData.objectsAreEqual(data, result));

		BlobLiteralFormatter ansiFormat = BlobFormatterFactory.createAnsiFormatter();
		String ansi = ansiFormat.getBlobLiteral(data).toString();
		result = instance.decodeString(ansi, BlobLiteralType.hex);
		assertTrue(RowData.objectsAreEqual(data, result));
	}
}
