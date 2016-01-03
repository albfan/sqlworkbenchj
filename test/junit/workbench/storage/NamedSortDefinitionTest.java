/*
 * NamedSortDefinitionTest.java
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
package workbench.storage;

import java.sql.Types;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class NamedSortDefinitionTest
{

	public NamedSortDefinitionTest()
	{
	}

	@Test
	public void testGetDefinitionString()
	{
		String[] columns = new String[] { "lastname", "firstname" };
		boolean[] asc = new boolean[] { true, false };

		NamedSortDefinition def = new NamedSortDefinition(columns, asc);

		String result = def.getDefinitionString();
		String expected = "\"lastname;a\",\"firstname;d\"";
		assertEquals(expected, result);

		NamedSortDefinition newDef = NamedSortDefinition.parseDefinitionString(result);
		assertEquals(expected, newDef.getDefinitionString());
	}

	@Test
	public void testInvalidSort()
	{
		String[] columns = new String[] { "lastname", "firstname" };
		boolean[] asc = new boolean[] { true, false };

		NamedSortDefinition def = new NamedSortDefinition(columns, asc);
		DataStore ds = new DataStore(new String[] {"first_name", "lastname"}, new int[] { Types.VARCHAR, Types.VARCHAR } );
		SortDefinition sort = def.getSortDefinition(ds);
		assertTrue(sort.isEmpty());
	}

}
