/*
 * OracleConstraintReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.oracle;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableConstraint;
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
public class OracleConstraintReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "oraconstraints";

	public OracleConstraintReaderTest()
	{
		super(TEST_ID);
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql =
			"create table person (id integer, constraint positive_id check (id > 0));\n" +
			"create table foo (id integer constraint foo_nn_id check (id is not null));";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testConstraintReader()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("PERSON"));
		OracleConstraintReader reader = new OracleConstraintReader(con.getDbId());
		List<TableConstraint> cons = reader.getTableConstraints(con, tbl);
		assertNotNull(cons);
		assertEquals(1, cons.size());
		TableConstraint constraint = cons.get(0);
		assertEquals("POSITIVE_ID", constraint.getConstraintName());
		assertEquals("(id > 0)", constraint.getExpression());
		assertEquals("CONSTRAINT POSITIVE_ID CHECK (id > 0)", constraint.getSql());
		assertEquals("check", constraint.getType());
	}

	@Test
	public void testNNConstraint()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("FOO"));
		String source = tbl.getSource(con).toString();
		assertTrue(source.toLowerCase().contains("constraint foo_nn_id check (id is not null)"));
	}

	@Test
	public void testIsDefaultNNConstraint()
	{
		OracleConstraintReader instance = new OracleConstraintReader("oracle");
		String definition = "\"MY_COL\" IS NOT NULL";
		boolean result = instance.isDefaultNNConstraint("SYS_C0013077", definition);
		assertTrue("Default NN not recognized", result);

		definition = "\"MY_COL\" IS NOT NULL OR COL2 IS NOT NULL";
		result = instance.isDefaultNNConstraint("chk_cols", definition);
		assertFalse("Default NN not recognized", result);
	}
}
