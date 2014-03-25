/*
 * MySQLTableSourceBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mysql;

import java.sql.SQLException;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.WbConnection;

import workbench.util.StringUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

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
		assertNotNull("No connection available", con);

		String sql =
			"create table tbl_isam (id integer primary key) engine = myisam\n comment = 'myisam table';\n" +
			"create table tbl_inno (id integer primary key) engine = innodb\n comment = 'innodb table';\n" +
			"commit;\n";
		TestUtil.executeScript(con, sql);

		TableIdentifier isam = con.getMetadata().findTable(new TableIdentifier("tbl_isam"));
		TableIdentifier inno = con.getMetadata().findTable(new TableIdentifier("tbl_inno"));
		MySQLTableSourceBuilder builder = (MySQLTableSourceBuilder)TableSourceBuilderFactory.getBuilder(con);

		builder.readTableOptions(isam, null);
		String options = isam.getSourceOptions().getTableOption();

		List<String> lines = StringUtil.getLines(options);
		assertEquals(2, lines.size());
		assertEquals("ENGINE=MyISAM", lines.get(0));
		assertEquals("COMMENT='myisam table'", lines.get(1));

		builder.readTableOptions(inno, null);
		options = inno.getSourceOptions().getTableOption();
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
		assertNotNull("No connection available", con);

		String sql =
			"create table foo (" +
			"  id integer primary key, " +
			"  foo varchar(10) default 'bar', \n" +
			"  bar date default '2014-01-01', " +
			"  dts datetime default '2014-01-01 01:02:03');\n" +
			"commit;\n";
		TestUtil.executeScript(con, sql);

		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("foo"));

		String create = tbl.getSource(con).toString();
		System.out.println(create);
		String[] lines = create.trim().split("\n");
		assertEquals(12, lines.length);
		assertEquals("CREATE TABLE foo", lines[0]);
		assertEquals("   foo  VARCHAR(10)   DEFAULT 'bar',", lines[3]);
		assertEquals("   bar  DATE          DEFAULT '2014-01-01',", lines[4]);
		assertEquals("   dts  DATETIME      DEFAULT '2014-01-01 01:02:03'", lines[5]);
	}

}
