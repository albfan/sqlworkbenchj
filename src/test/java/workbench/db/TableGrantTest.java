/*
 * TableGrantTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableGrantTest
{

	@Test
	public void testCompareTo()
	{
		TableGrant g1 = new TableGrant("testuser", "DELETE", false);
		TableGrant g2 = new TableGrant("testuser", "DELETE", false);
		assertEquals("incorrect compareTo for equals objects", 0, g1.compareTo(g2));

		g1 = new TableGrant("testuser", "DELETE", true);
		g2 = new TableGrant("testuser", "DELETE", false);
		assertEquals("incorrect compareTo for equals objects", 1, g1.compareTo(g2));

	}

	@Test
	public void testEquals()
	{
		TableGrant g1 = new TableGrant("testuser", "DELETE", false);
		TableGrant g2 = new TableGrant("testuser", "DELETE", false);

		assertEquals("incorrect equals for equals objects", true, g1.equals(g2));

		g1 = new TableGrant("testuser", "DELETE", true);
		g2 = new TableGrant("testuser", "DELETE", false);

		assertEquals("incorrect equals for equals objects", false, g1.equals(g2));

		g1 = new TableGrant("someuser", "DELETE", false);
		g2 = new TableGrant("testuser", "DELETE", false);

		assertEquals("incorrect equals for equals objects", false, g1.equals(g2));

		g1 = new TableGrant("testuser", "INSERT", false);
		g2 = new TableGrant("testuser", "DELETE", false);

		assertEquals("incorrect equals for equals objects", false, g1.equals(g2));

		Set<TableGrant> grants = new HashSet<TableGrant>();
		g1 = new TableGrant("testuser", "DELETE", true);
		g2 = new TableGrant("testuser", "DELETE", false);
		grants.add(g1);
		grants.add(g2);
		assertEquals("Not all grants added", 2, grants.size());

		// This should not be added as it is equal to g2
		grants.add(new TableGrant("testuser", "DELETE", false));
		assertEquals("Not all grants added", 2, grants.size());
	}


}
