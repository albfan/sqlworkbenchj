/*
 * Db2ConstraintReaderTest.java
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
public class Db2ConstraintReaderTest
{

	public Db2ConstraintReaderTest()
	{
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
			"create table " + schema + ".person (id integer, constraint positive_id check (id > 0));\n" +
			"commit;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		Db2TestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetTableConstraintSql()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) fail("No connection available");

		String schema = Db2TestUtil.getSchemaName();
		TableDefinition person = con.getMetadata().getTableDefinition(new TableIdentifier(schema, "PERSON"));
		assertNotNull(person);

		ConstraintReader reader = ReaderFactory.getConstraintReader(con.getMetadata());
		List<TableConstraint> constraints = reader.getTableConstraints(con, person);

		assertNotNull(constraints);
		assertEquals(1, constraints.size());
		TableConstraint check = constraints.get(0);
		assertEquals("POSITIVE_ID", check.getConstraintName());
		assertEquals("(ID > 0)", check.getExpression());
	}

}
