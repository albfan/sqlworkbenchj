/*
 * StrBufferTest.java
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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author  Thomas Kellerer
 */
public class StrBufferTest
{

	@Test
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

	@Test
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
