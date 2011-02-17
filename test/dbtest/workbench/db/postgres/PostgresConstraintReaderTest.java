/*
 * PostgresConstraintReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

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
public class PostgresConstraintReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "pgconstrainttest";

	public PostgresConstraintReaderTest()
	{
		super("PostgresConstraintReader");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		String sql =
			"CREATE TABLE check_test " +
			"(\n" +
			"   id integer, \n" +
			"   constraint aaa_check_id check (id > 42), \n" +
			"   constraint bbb_exclusion exclude (id WITH = )\n" +
			");\n"+
			"commit;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testGetConstraints()
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("check_test"));
		List<TableConstraint> cons = con.getMetadata().getTableConstraints(tbl);
		assertNotNull(cons);
		assertEquals(2, cons.size());
		TableConstraint check = cons.get(0);
		assertEquals("aaa_check_id", check.getConstraintName());
		assertEquals("(id > 42)", check.getExpression());
		assertEquals("check", check.getType());
		assertEquals("CONSTRAINT aaa_check_id CHECK (id > 42)", check.getSql());

		TableConstraint exclusion = cons.get(1);
		assertEquals("bbb_exclusion", exclusion.getConstraintName());
		assertEquals("EXCLUDE USING btree (id WITH =)", exclusion.getExpression());
		assertEquals("exclusion", exclusion.getType());
		assertEquals("CONSTRAINT bbb_exclusion EXCLUDE USING btree (id WITH =)", exclusion.getSql());

	}

}
