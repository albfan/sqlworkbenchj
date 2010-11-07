/*
 * PostgresTriggerReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
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
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.WbConnection;
import workbench.sql.DelimiterDefinition;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTriggerReaderTest
	extends WbTestCase
{
	private static final String TEST_SCHEMA = "trgreadertest";

	public PostgresTriggerReaderTest()
	{
		super("PostgresTriggerReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_SCHEMA);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con, "create table some_table (id integer, some_data varchar(100));\n" +
			"commit;\n");

		String sql =
			"CREATE OR REPLACE FUNCTION my_trigger_func()  \n" +
			"RETURNS trigger AS  \n" +
			"$body$ \n" +
			"BEGIN \n" +
			"    if new.comment IS NULL then \n" +
			"        new.comment = 'n/a'; \n" +
			"    end if; \n" +
			"    RETURN NEW; \n" +
			"END; \n" +
			"$body$  \n" +
			"LANGUAGE plpgsql; \n" +
			"/" +
			"\n" +
			"CREATE TRIGGER some_trg BEFORE UPDATE ON some_table \n" +
			"    FOR EACH ROW EXECUTE PROCEDURE my_trigger_func()\n" +
			"/";

		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_SCHEMA);
	}

	@Test
	public void testGetDependentSource()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TableIdentifier tbl = new TableIdentifier(TEST_SCHEMA, "some_table");
		TriggerReader reader = TriggerReaderFactory.createReader(con);
		assertTrue(reader instanceof PostgresTriggerReader);
		List<TriggerDefinition> triggers = reader.getTriggerList(null, TEST_SCHEMA, "some_table");
		assertEquals(1, triggers.size());

		TriggerDefinition trg = triggers.get(0);
		assertEquals("some_trg", trg.getObjectName());

		String sql = trg.getSource(con, false).toString();
		assertTrue(sql.startsWith("CREATE TRIGGER some_trg"));

		sql = reader.getDependentSource(null, TEST_SCHEMA, trg.getObjectName(), trg.getRelatedTable()).toString();
		assertNotNull(sql);
		assertTrue(sql.contains("CREATE OR REPLACE FUNCTION my_trigger_func()"));
	}
}
