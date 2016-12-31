/*
 * MessageBufferTest.java
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

import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class MessageBufferTest
	extends WbTestCase
{

	public MessageBufferTest()
	{
		super("MessageBufferTest");
	}

	@Test
	public void testMaxSize()
	{
		int max = 5;
		MessageBuffer b = new MessageBuffer(5);
		for (int i = 0; i < max * 2; i++)
		{
			b.append("Line" + i + "\n");
		}
		String content = b.getBuffer().toString();
		String expected = "(...)\nLine5\nLine6\nLine7\nLine8\nLine9\n";
		assertEquals(expected, content);
	}

	@Test
	public void testAppendBuffer()
	{
		MessageBuffer b1 = new MessageBuffer();
		b1.append("Line one");
		int l = b1.getLength();
		MessageBuffer b2 = new MessageBuffer();
		b2.append(b1);
		assertEquals("Wrong length", b2.getLength(), l);

		b2 = new MessageBuffer();
		b2.append("Some stuff");
		int l2 = b2.getLength();
		b2.append(b1);
		assertEquals("Wrong length", b2.getLength(), l + l2);
	}

	@Test
	public void testBuffer()
	{
		MessageBuffer b = new MessageBuffer();
		b.append("Hello, world");
		b.appendNewLine();
		b.append("how are you?");

		CharSequence s = b.getBuffer();
		assertEquals("Hello, world\nhow are you?", s.toString());
	}

}
