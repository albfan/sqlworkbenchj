/*
 * MySQLTableSourceBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mysql;

import java.sql.SQLException;
import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.WbConnection;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLTableSourceBuilderTest
	extends WbTestCase
{

	public MySQLTableSourceBuilderTest()
	{
		super("MySQLTableSourceBuilderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		MySQLTestUtil.initTestcase("MySQLDataStoreTest");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;

		String sql =
			"drop table tbl_isam;\n" +
			"drop table tbl_inno;\n" +
			"drop table foo;\n" +
			"commit;\n";
		TestUtil.executeScript(con, sql);

		MySQLTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetTableOptions()
		throws SQLException
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;

		String sql =
			"create table tbl_isam (id integer primary key) engine = myisam\n comment = 'myisam table';\n" +
			"create table tbl_inno (id integer primary key) engine = innodb\n comment = 'innodb table';\n" +
			"commit;\n";
		TestUtil.executeScript(con, sql);

		TableIdentifier isam = con.getMetadata().findTable(new TableIdentifier("tbl_isam"));
		TableIdentifier inno = con.getMetadata().findTable(new TableIdentifier("tbl_inno"));
		MySQLTableSourceBuilder builder = (MySQLTableSourceBuilder)TableSourceBuilderFactory.getBuilder(con);

		String options = builder.getAdditionalTableOptions(isam, null, null);
		List<String> lines = StringUtil.getLines(options);
		assertEquals(2, lines.size());
		assertEquals("ENGINE=MyISAM", lines.get(0));
		assertEquals("COMMENT='myisam table'", lines.get(1));

		options = builder.getAdditionalTableOptions(inno, null, null);
		lines = StringUtil.getLines(options);
		assertEquals(2, lines.size());
		assertEquals("ENGINE=InnoDB", lines.get(0));
		assertEquals("COMMENT='innodb table'", lines.get(1));
	}

	@Test
	public void testDefaultValue()
		throws SQLException
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;

		String sql =
			"create table foo (id integer primary key, foo varchar(10) default 'bar');\n" +
			"commit;\n";
		TestUtil.executeScript(con, sql);

		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("foo"));

		String create = tbl.getSource(con).toString();
		System.out.println(create);
		String[] lines = create.trim().split("\n");
		assertEquals(9, lines.length);
		assertEquals("CREATE TABLE foo", lines[0]);
		assertEquals("   foo  VARCHAR(10)   DEFAULT 'bar'", lines[3]);
	}

}
