/*
 * Db2ConstraintReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.ibm;

import workbench.db.TableConstraint;
import java.util.List;
import workbench.TestUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;
import workbench.db.ConstraintReader;
import workbench.db.ReaderFactory;

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
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		String schema = Db2TestUtil.getSchemaName();
		TableIdentifier person = con.getMetadata().findTable(new TableIdentifier(schema, "PERSON"));
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
