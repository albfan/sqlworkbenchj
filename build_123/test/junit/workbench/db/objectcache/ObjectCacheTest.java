/*
 * ObjectCacheTest.java
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
package workbench.db.objectcache;

import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectCacheTest
	extends WbTestCase
{

	public ObjectCacheTest()
	{
		super("ObjectCacheTest");
	}

	@Test
	public void testCache()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection dbConn = util.getHSQLConnection("CACHE_TEST");
		String script =
			"create table one (id_one integer, one_col integer);\n" +
			"create table two (id_two integer, two_col integer);\n" +
			"commit;";
		TestUtil.executeScript(dbConn, script);

		ObjectCache cache = new ObjectCache(dbConn);
		Set<TableIdentifier> tables = cache.getTables(dbConn, "PUBLIC", null);
		assertNotNull(tables);
		assertEquals(2, tables.size());

		TableIdentifier one = cache.findEntry(dbConn, new TableIdentifier("ONE"));
		assertNotNull(one);
		assertNotNull(one.getCatalog());
		assertEquals("PUBLIC", one.getSchema());
		assertEquals("ONE", one.getTableName());

		TableIdentifier two = cache.findEntry(dbConn, new TableIdentifier("PUBLIC", "TWO"));
		assertNotNull(two);
		assertNotNull(two.getCatalog());
		assertEquals("PUBLIC", two.getSchema());
		assertEquals("TWO", two.getTableName());

		// make sure that tables that do carry a schema/catalog are not mistakenly returned from the cache
		TableIdentifier foo = cache.findEntry(dbConn, new TableIdentifier("FOO", "BAR", "ONE"));
		assertNull(foo);

		TableIdentifier bar = cache.findEntry(dbConn, new TableIdentifier("BAR", "ONE"));
		assertNull(bar);
	}

}
