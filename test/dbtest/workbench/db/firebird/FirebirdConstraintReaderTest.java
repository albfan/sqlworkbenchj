/*
 * FirebirdConstraintReaderTest.java
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
package workbench.db.firebird;

import java.util.List;

import workbench.TestUtil;

import workbench.db.ConstraintReader;
import workbench.db.ReaderFactory;
import workbench.db.TableConstraint;
import workbench.db.TableDefinition;
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
public class FirebirdConstraintReaderTest
{

	public FirebirdConstraintReaderTest()
	{
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		FirebirdTestUtil.initTestCase();
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;

		String sql = "CREATE TABLE check_test (id integer, constraint positive_id check (id > 42));";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		FirebirdTestUtil.cleanUpTestCase();
	}

	@Test
	public void testReader()
		throws Exception
	{
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		assertNotNull("No connection available", con);

		TableDefinition def = con.getMetadata().getTableDefinition(new TableIdentifier("CHECK_TEST"));
		ConstraintReader reader = ReaderFactory.getConstraintReader(con.getMetadata());
		List<TableConstraint> cons = reader.getTableConstraints(con, def);
		assertNotNull(cons);
		assertEquals(1, cons.size());
		TableConstraint constraint = cons.get(0);
		assertEquals("POSITIVE_ID", constraint.getConstraintName());
		assertEquals("check (id > 42)", constraint.getExpression());
		assertEquals("check", constraint.getType());
		assertEquals("CONSTRAINT POSITIVE_ID check (id > 42)", constraint.getSql());
	}
}
