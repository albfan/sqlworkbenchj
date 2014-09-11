/*
 * PostgresBlobFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.storage;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;
import org.junit.Test;


/**
 * @author Thomas Kellerer
 */
public class PostgresBlobFormatterTest
{

	@Test
	public void testGetBlobLiteral()
		throws Exception
	{
		PostgresBlobFormatter formatter = new PostgresBlobFormatter(BlobLiteralType.pgEscape);
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		b.write(255);
		b.write(0);
		b.write(16);
		b.write(15);
		byte[] blob = b.toByteArray();
		String literal = formatter.getBlobLiteral(blob).toString();
		assertEquals("Wrong literal created", "E'\\\\377\\\\000\\\\020\\\\017'::bytea", literal);

		formatter = new PostgresBlobFormatter(BlobLiteralType.pgDecode);
		String decodeLiteral = formatter.getBlobLiteral(blob).toString();
		assertEquals("Wrong literal created", "decode('ff00100f', 'hex')", decodeLiteral);
	}
}
