/*
 * MySqlEnumReaderTest
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
import workbench.db.ColumnIdentifier;
import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLColumnEnhancerTest
	extends WbTestCase
{

	public MySQLColumnEnhancerTest()
	{
		super("MySqlEnumReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		MySQLTestUtil.initTestcase("MySqlEnumReaderTest");

		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;

		String sql = "CREATE TABLE enum_test \n" +
								 "( \n" +
								 "   nr     INT, \n" +
								 "   color  enum('red','green','blue') \n" +
								 ");";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;
		String sql = "DROP TABLE enum_test;";
		TestUtil.executeScript(con, sql);
	}

	@Test
	public void testUpdateColumnDefinition()
		throws SQLException
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null)
		{
			return;
		}

		TableDefinition def = con.getMetadata().getTableDefinition(new TableIdentifier("enum_test"));
		assertNotNull(def);

		List<ColumnIdentifier> cols = def.getColumns();
		assertNotNull(cols);
		assertEquals(2, cols.size());
		String type = cols.get(1).getDbmsType();
		assertEquals("enum('red','green','blue')", type);
	}
}
