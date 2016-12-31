/*
 * DefaultBlobFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
public class DefaultBlobFormatterTest
{

	@Test
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
