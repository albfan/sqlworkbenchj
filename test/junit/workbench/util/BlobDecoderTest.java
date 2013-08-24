/*
 * BlobDecoderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;


import javax.xml.bind.DatatypeConverter;

import workbench.storage.BlobFormatterFactory;
import workbench.storage.BlobLiteralFormatter;
import workbench.storage.BlobLiteralType;
import workbench.storage.RowData;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class BlobDecoderTest
{

	@Test
	public void testDecodeString()
		throws Exception
	{
		BlobDecoder instance = new BlobDecoder();
		byte[] data = new byte[]
		{
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17
		};

		String base64 = DatatypeConverter.printBase64Binary(data);
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
