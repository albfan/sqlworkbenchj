/*
 * HsqlConstraintReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.hsqldb;

import java.util.List;
import workbench.db.TableConstraint;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.ConstraintReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlConstraintReaderTest
	extends WbTestCase
{

	public HsqlConstraintReaderTest()
	{
		super("HsqlConstraintReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testReader()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection conn = util.getHSQLConnection("constraintreader");
		TestUtil.executeScript(conn, "create table cons_test (id integer, nr integer, constraint min_value check (id > 42), check (nr < 100));");
		ConstraintReader reader = conn.getMetadata().getConstraintReader();
		assertTrue(reader instanceof HsqlConstraintReader);
		HsqlConstraintReader hsqlReader = (HsqlConstraintReader)reader;
		TableIdentifier tbl = new TableIdentifier("CONS_TEST");
		List<TableConstraint> constraints = reader.getTableConstraints(conn, tbl);
		assertEquals(2, constraints.size());
		TableConstraint minValue = constraints.get(0);
		// HSQLDB 2.0 includes the table's schema
		assertEquals("(PUBLIC.CONS_TEST.ID>42)", minValue.getExpression());

		TableConstraint maxValue = constraints.get(1);
		assertEquals("(PUBLIC.CONS_TEST.NR<100)", maxValue.getExpression());
		assertTrue(hsqlReader.isSystemConstraintName(maxValue.getConstraintName()));
	}

}
