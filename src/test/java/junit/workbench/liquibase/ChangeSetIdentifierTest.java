/*
 * ChangeSetIdentifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.liquibase;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ChangeSetIdentifierTest
{

	public ChangeSetIdentifierTest()
	{
	}

	@Test
	public void testIsEqualTo()
	{
		ChangeSetIdentifier one = new ChangeSetIdentifier("Arthur::1");

		ChangeSetIdentifier other = new ChangeSetIdentifier("Arthur::2");
		assertFalse(one.isEqualTo(other));

		one = new ChangeSetIdentifier("Arthur::*");

		other = new ChangeSetIdentifier("Arthur::2");
		assertTrue(one.isEqualTo(other));

		one = new ChangeSetIdentifier("Arthur::*");
		other = new ChangeSetIdentifier("Ford::*");
		assertFalse(one.isEqualTo(other));

		one = new ChangeSetIdentifier("*::*");
		other = new ChangeSetIdentifier("Ford::*");
		assertTrue(one.isEqualTo(other));

		one = new ChangeSetIdentifier("*::1");
		other = new ChangeSetIdentifier("Ford::1");
		assertTrue(one.isEqualTo(other));
	}

	@Test
	public void testParsing()
	{
		ChangeSetIdentifier csi = new ChangeSetIdentifier("Arthur::1");
		assertEquals("Arthur", csi.getAuthor());
		assertEquals("1", csi.getId());

		csi = new ChangeSetIdentifier("Arthur::*");
		assertEquals("Arthur", csi.getAuthor());
		assertEquals("*", csi.getId());

		csi = new ChangeSetIdentifier("*::1");
		assertEquals("*", csi.getAuthor());
		assertEquals("1", csi.getId());

		csi = new ChangeSetIdentifier("1");
		assertEquals("*", csi.getAuthor());
		assertEquals("1", csi.getId());

		csi = new ChangeSetIdentifier("Arthur", "1");
		assertEquals("Arthur", csi.getAuthor());
		assertEquals("1", csi.getId());

		csi = new ChangeSetIdentifier("Arthur", null);
		assertEquals("Arthur", csi.getAuthor());
		assertEquals("*", csi.getId());
	}

}
