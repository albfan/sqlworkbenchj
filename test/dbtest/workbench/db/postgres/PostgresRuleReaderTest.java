/*
 * PostgresRuleReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.postgres.PostgresTestCase;
import static org.junit.Assert.*;
/**
 *
 * @author Thomas Kellerer
 */
public class PostgresRuleReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "ruletest";

	public PostgresRuleReaderTest()
	{
		super("PostgresRuleReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestCase.initTestCase(TEST_ID);
		WbConnection con = TestUtil.getPostgresConnection();
		TestUtil.executeScript(con,
			"CREATE table person (id integer, firstname varchar(50), lastname varchar(50));\n" +
			"COMMIT;\n" + 
			"CREATE RULE \"_INSERT\" AS ON INSERT TO person DO INSTEAD NOTHING;\n " +
			"COMMIT; \n"
		);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestCase.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void retrieveRules()
		throws Exception
	{
		WbConnection con = TestUtil.getPostgresConnection();
		List<TableIdentifier> objects = con.getMetadata().getObjectList(TEST_ID, new String[] { "RULE" });
		assertEquals(1, objects.size());
		TableIdentifier tbl = objects.get(0);
		assertEquals("RULE", tbl.getObjectType());
		String sql = tbl.getSource(con).toString();
		assertEquals("CREATE RULE \"_INSERT\" AS ON INSERT TO person DO INSTEAD NOTHING;", sql);
	}

}
