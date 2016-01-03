/*
 * CreateTableTypeDefinitionTest.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import workbench.util.CollectionUtil;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class CreateTableTypeDefinitionTest
{

	public CreateTableTypeDefinitionTest()
	{
	}

	@Test
	public void testParsing()
	{
		String key = "workbench.db.h2.create.table.h2_localtemp";
		CreateTableTypeDefinition def = new CreateTableTypeDefinition(key);
		assertEquals("H2", def.getDatabase());
		assertEquals("h2_localtemp", def.getType());

		key = "workbench.db.great_other_database.create.table.globaltemp";
		def = new CreateTableTypeDefinition(key);
		assertEquals("Great Other Database", def.getDatabase());
		assertEquals("globaltemp", def.getType());
	}

	@Test
	public void testSorting()
	{
		List<String> types = CollectionUtil.arrayList(
			"workbench.db.postgresql.create.table.localtemp",
			"workbench.db.h2.create.table.localtemp",
			"workbench.db.postgresql.create.table.globaltemp",
			"workbench.db.db2.create.table.temp"
			);

		List<CreateTableTypeDefinition> result = new ArrayList<CreateTableTypeDefinition>(types.size());
		for (String type : types)
		{
			result.add(new CreateTableTypeDefinition(type));
		}
		Collections.sort(result);
		assertEquals(result.get(0).getDatabase(), DbSettings.getDBMSNames().get("db2"));
		assertEquals(result.get(1).getDatabase(), DbSettings.getDBMSNames().get("h2"));
		assertEquals(result.get(2).getDatabase(), DbSettings.getDBMSNames().get("postgresql"));
		assertEquals(result.get(3).getDatabase(), DbSettings.getDBMSNames().get("postgresql"));
	}

	@Test
	public void testCompare()
	{
		CreateTableTypeDefinition one = new CreateTableTypeDefinition("workbench.db.mydb.create.table.tempnolog");
		CreateTableTypeDefinition other = new CreateTableTypeDefinition("workbench.db.mydb.create.table.temp");
		assertNotSame(one, other);
		assertFalse(one.equals(other));
		assertTrue(one.hashCode() != other.hashCode());
		assertTrue(one.compareTo(other) != 0);
	}

}
