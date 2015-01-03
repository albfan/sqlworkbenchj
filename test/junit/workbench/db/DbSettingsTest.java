/*
 * DbSettingsTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db;

import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 * @author Thomas Kellerer
 */
public class DbSettingsTest
	extends WbTestCase
{

	public DbSettingsTest()
	{
		super("DbSettingsTest");
	}

	@Test
	public void testOraDefaults()
	{
		DbSettings pg = new DbSettings("postgresql");
		assertFalse(pg.getConvertDateInExport());
	}

	@Test
	public void testPgDefaults()
	{
		DbSettings pg = new DbSettings("postgresql");
		assertFalse(pg.getConvertDateInExport());
	}

	@Test
	public void testGetIdentifierCase()
	{
		DbSettings test = new DbSettings("dummy");

		IdentifierCase idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.unknown, idCase);

		test.setObjectNameCase("mixed");
		idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.mixed, idCase);

		test.setObjectNameCase("gaga");
		idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.unknown, idCase);

		test.setObjectNameCase("lower");
		idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.lower, idCase);
	}

	@Test
	public void testTruncate()
	{
		DbSettings db = new DbSettings("oracle");
		assertTrue(db.supportsTruncate());
		assertFalse(db.supportsCascadedTruncate());
		assertFalse(db.truncateNeedsCommit());

		db = new DbSettings("postgresql");
		assertTrue(db.supportsTruncate());
		assertTrue(db.supportsCascadedTruncate());
		assertTrue(db.truncateNeedsCommit());

		db = new DbSettings("microsoft_sql_server");
		assertTrue(db.supportsTruncate());
		assertFalse(db.supportsCascadedTruncate());
		assertTrue(db.truncateNeedsCommit());

		db = new DbSettings("mysql");
		assertTrue(db.supportsTruncate());

		db = new DbSettings("h2");
		assertTrue(db.supportsTruncate());

		db = new DbSettings("hsql_database_engine");
		assertTrue(db.supportsTruncate());

		db = new DbSettings("apache_derby");
		assertTrue(db.supportsTruncate());

	}

}
