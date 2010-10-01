/*
 * OracleConstraintReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableConstraint;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
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

		String sql = "create table person (id integer, constraint positive_id check (id > 0));";
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
		OracleConstraintReader reader = new OracleConstraintReader();
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
	public void testIsDefaultNNConstraint()
	{
		OracleConstraintReader instance = new OracleConstraintReader();
		String definition = "\"MY_COL\" IS NOT NULL";
		boolean result = instance.isDefaultNNConstraint(definition);
		assertEquals("Default NN not recognized", true, result);

		definition = "\"MY_COL\" IS NOT NULL OR COL2 IS NOT NULL";
		result = instance.isDefaultNNConstraint(definition);
		assertEquals("Default NN not recognized", false, result);
	}
}
