/*
 * NumberStringCacheTest.java
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
package workbench.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author thomas
 */
public class NumberStringCacheTest
{
	@Test
	public void testGetNumberString()
	{
		String s = NumberStringCache.getNumberString(10);
		assertEquals(s, "10");

		char c = 32;
		s = NumberStringCache.getNumberString(c);
		assertEquals(s, "32");
	}

	@Test
	public void testHexString()
	{
		assertEquals("00", NumberStringCache.getHexString(0));
		assertEquals("0f", NumberStringCache.getHexString(15));
		assertEquals("12", NumberStringCache.getHexString(18));
		assertEquals("ff", NumberStringCache.getHexString(255));
		assertEquals("100", NumberStringCache.getHexString(256));
		assertEquals("200", NumberStringCache.getHexString(512));
	}
}
