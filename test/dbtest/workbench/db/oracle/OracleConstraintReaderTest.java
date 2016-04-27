/*
 * OracleConstraintReaderTest.java
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
package workbench.db.oracle;

import java.sql.Types;
import java.util.Collections;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableConstraint;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import workbench.db.ConstraintType;

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
		assertNotNull(con);

		String sql =
			"create table person (id integer, constraint positive_id check (id > 0));\n";
		TestUtil.executeScript(con, sql);

		TableDefinition def = con.getMetadata().getTableDefinition(new TableIdentifier("PERSON"));
		OracleConstraintReader reader = new OracleConstraintReader(con.getDbId());
		List<TableConstraint> cons = reader.getTableConstraints(con, def);
		assertNotNull(cons);
		assertEquals(1, cons.size());
		TableConstraint constraint = cons.get(0);
		assertEquals("POSITIVE_ID", constraint.getConstraintName());
		assertEquals("(id > 0)", constraint.getExpression());
		assertEquals("CONSTRAINT POSITIVE_ID CHECK (id > 0)", constraint.getSql());
		assertEquals(ConstraintType.Check, constraint.getConstraintType());
	}

	@Test
	public void testNNConstraint()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);

		String sql =
			"create table foo1 (id1 integer not null); \n" +
			"create table foo2 (id2 integer constraint id2_not_null not null); \n" +
			"create table foo3 (id3 integer check (id3 is not null)); \n" +
			"create table foo4 (id4 integer constraint id4_not_null check (id4 is not null));";
		TestUtil.executeScript(con, sql);

		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("FOO1"));
		String source = tbl.getSource(con).toString();
//		System.out.println(source);
		assertTrue(source.contains("ID1  NUMBER   NOT NULL"));

		tbl = con.getMetadata().findTable(new TableIdentifier("FOO2"));
		source = tbl.getSource(con).toString();
//		System.out.println(source);
		assertTrue(source.contains("ID2  NUMBER   CONSTRAINT ID2_NOT_NULL NOT NULL"));

		tbl = con.getMetadata().findTable(new TableIdentifier("FOO3"));
		source = tbl.getSource(con).toString();
//		System.out.println(source);
		assertTrue(source.contains("ID3  NUMBER   CHECK (id3 is not null)"));

		tbl = con.getMetadata().findTable(new TableIdentifier("FOO4"));
		source = tbl.getSource(con).toString();
//		System.out.println(source);
		assertTrue(source.contains("ID4  NUMBER   CONSTRAINT ID4_NOT_NULL CHECK (id4 is not null)"));
	}

	@Test
	public void testIsDefaultNNConstraint()
	{
		OracleConstraintReader instance = new OracleConstraintReader("oracle");
		String definition = "\"MY_COL\" IS NOT NULL";
		ColumnIdentifier myCol = new ColumnIdentifier("MY_COL", Types.VARCHAR);
		myCol.setIsNullable(false);

		List<ColumnIdentifier> cols =  Collections.singletonList(myCol);

		boolean shouldHide = instance.hideTableConstraint("SYS_C0013077", definition, cols);
		assertTrue("Default NN not recognized", shouldHide);

		definition = "\"MY_COL\" IS NOT NULL OR COL2 IS NOT NULL";
		shouldHide = instance.hideTableConstraint("chk_cols", definition, cols);
		assertFalse("Default NN not recognized", shouldHide);
	}

}
