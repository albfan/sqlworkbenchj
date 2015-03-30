/*
 * Db2CompletionTest.java
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
package workbench.db.ibm;


import java.sql.SQLException;
import java.util.Set;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2CompletionTest
	extends WbTestCase
{
	public Db2CompletionTest()
	{
		super("Db2CompletionTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		Db2TestUtil.initTestCase();

		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;
		String schema = Db2TestUtil.getSchemaName();

		String sql =
			"CREATE TABLE " + schema + ".data1 (pid integer not null primary key, info varchar(100));\n"  +
			"CREATE TABLE " + schema + ".data2 (did integer not null primary key, info varchar(100));\n"  +
			"commit;\n";
		TestUtil.executeScript(con, sql, true);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		Db2TestUtil.cleanUpTestCase();
	}

	@Test
	public void testSelectCompletion()
		throws SQLException
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) fail("No connection available");

		Set<TableIdentifier> tables = con.getObjectCache().getTables(Db2TestUtil.getSchemaName().toLowerCase());
		assertEquals(2, tables.size());
		assertTrue(tables.contains(new TableIdentifier(Db2TestUtil.getSchemaName(), "DATA1")));
		assertTrue(tables.contains(new TableIdentifier(Db2TestUtil.getSchemaName(), "DATA2")));

		tables = con.getObjectCache().getTables(Db2TestUtil.getSchemaName());
		assertEquals(2, tables.size());
		assertTrue(tables.contains(new TableIdentifier(Db2TestUtil.getSchemaName(), "DATA1")));
		assertTrue(tables.contains(new TableIdentifier(Db2TestUtil.getSchemaName(), "DATA2")));
	}

}
