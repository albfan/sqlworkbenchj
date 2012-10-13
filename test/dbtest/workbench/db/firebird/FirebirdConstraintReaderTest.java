/*
 * FirebirdConstraintReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.firebird;

import java.util.List;
import workbench.db.ConstraintReader;
import workbench.db.TableConstraint;
import workbench.db.TableIdentifier;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.db.WbConnection;
import static org.junit.Assert.*;
import workbench.db.ReaderFactory;

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
		if (con == null) return;

		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("CHECK_TEST"));
		ConstraintReader reader = ReaderFactory.getConstraintReader(con.getMetadata());
		List<TableConstraint> cons = reader.getTableConstraints(con, tbl);
		assertNotNull(cons);
		assertEquals(1, cons.size());
		TableConstraint constraint = cons.get(0);
		assertEquals("POSITIVE_ID", constraint.getConstraintName());
		assertEquals("check (id > 42)", constraint.getExpression());
		assertEquals("check", constraint.getType());
		assertEquals("CONSTRAINT POSITIVE_ID check (id > 42)", constraint.getSql());
	}
}
