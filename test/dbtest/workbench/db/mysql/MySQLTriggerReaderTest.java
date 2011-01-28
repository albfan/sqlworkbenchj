/*
 * MySQLTriggerReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mysql;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
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
public class MySQLTriggerReaderTest
	extends WbTestCase
{

	public MySQLTriggerReaderTest()
	{
		super("MySQLTriggerReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		MySQLTestUtil.initTestcase("MySQLTriggerReaderTest");
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;

		String sql =
			"CREATE TABLE one (id integer, some_value integer);";
		TestUtil.executeScript(con, sql);

		sql =
			"CREATE TRIGGER testref BEFORE INSERT ON one \n" +
			"  FOR EACH ROW BEGIN \n" +
			"    SET NEW.some_value = NEW.id * 42; \n" +
			"  END;\n"+
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;
		String sql = "DROP TABLE one;";
		TestUtil.executeScript(con, sql);
	}

	@Test
	public void testReadTriggers()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;

		TriggerReader reader = TriggerReaderFactory.createReader(con);
		List<TriggerDefinition> list = reader.getTriggerList(null, null, "one");
		assertEquals(1, list.size());
		TriggerDefinition trigger = list.get(0);
		assertEquals("testref", trigger.getObjectName());
		assertEquals("INSERT", trigger.getTriggerEvent());
		assertEquals("BEFORE", trigger.getTriggerType());
	}
}
