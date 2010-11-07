/*
 * PostgresViewReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresViewReaderTest
	extends WbTestCase
{

	private static final String TEST_SCHEMA = "viewreadertest";
	
	public PostgresViewReaderTest()
	{
		super("PostgresViewReaderTeEst");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_SCHEMA);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;
		TestUtil.executeScript(con, "create table some_table (id integer, some_data varchar(100));\n" +
			"create view v_view as select * from some_table;\n" +
			"create rule insert_view AS ON insert to v_view do instead insert into some_table values (new.id, new.some_data);\n" +
			"commit;\n");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_SCHEMA);
	}

	@Test
	public void testGetExtendedViewSource()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TableIdentifier view = con.getMetadata().findObject(new TableIdentifier(TEST_SCHEMA, "v_view"));
		String sql = con.getMetadata().getViewReader().getExtendedViewSource(view).toString();
		assertTrue(sql.contains("CREATE RULE insert_view AS ON INSERT TO v_view DO"));
	}
}
