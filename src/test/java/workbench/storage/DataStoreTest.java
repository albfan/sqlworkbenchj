/*
 * DataStoreTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.storage;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.filter.AndExpression;
import workbench.storage.filter.ComplexExpression;
import workbench.storage.filter.LessThanComparator;
import workbench.storage.filter.NumberEqualsComparator;
import workbench.storage.filter.OrExpression;
import workbench.storage.filter.StartsWithComparator;
import workbench.storage.filter.StringEqualsComparator;

import workbench.util.Alias;
import workbench.util.SqlUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DataStoreTest
	extends WbTestCase
{
	private final int rowcount = 10;
	private TestUtil util;

	public DataStoreTest()
		throws Exception
	{
		super("DataStoreTest");
		util = getTestUtil();
	}

	@Test
	public void testRetrieveGenerated()
		throws Exception
	{
		util.emptyBaseDirectory();
		WbConnection con = util.getConnection("pkTestDb");

		TestUtil.executeScript(con,
			"create table gen_test(id integer identity, some_data varchar(100)); \n" +
			"insert into gen_test (some_data) values ('foobar'); \n" +
			"commit;");

		String sql = "select id, some_data from gen_test";

		DataStore ds;
		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			ds = new DataStore(rs, con);
		}

		ds.setGeneratingSql(sql);
		ds.checkUpdateTable(con);

		ds.setValue(0, 1, "bar");
		int row = ds.addRow();
		ds.setValue(row, 1, "foo");
		ds.updateDb(con, null);
		int id = ds.getValueAsInt(row, 0, Integer.MIN_VALUE);
		assertEquals(2, id);

		id = ds.getValueAsInt(0, 0, Integer.MIN_VALUE);
		assertEquals(1, id);

		ds.deleteRow(0);
		ds.updateDb(con, null);
		id = ds.getValueAsInt(0, 0, Integer.MIN_VALUE);
		assertEquals(2, id);
	}

	@Test
	public void testMissingPkColumns()
		throws Exception
	{
		util.emptyBaseDirectory();
		WbConnection con = util.getConnection("pkTestDb");
		TestUtil.executeScript(con,
			"CREATE TABLE junit_test (id1 integer, id2 integer, id3 integer, some_data varchar(100), primary key (id1, id2, id3));\n" +
			"insert into junit_test (id1,id2,id3, some_data) values (1,2,3,'bla');\n" +
			"commit;");

		String sql = "select id1, id2, some_data from JUnit_Test";
		DataStore ds;
		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			ds = new DataStore(rs, con);
		}
		ds.setGeneratingSql(sql);
		ds.checkUpdateTable(con);
		assertEquals("Missing PK columns not detected", false, ds.pkColumnsComplete());
		List<ColumnIdentifier> cols = ds.getMissingPkColumns();
		assertEquals("Not all missing columns detected", 1, cols.size());
		String col1 = cols.get(0).getColumnName();
		assertEquals("Wrong column detected", true, col1.equals("ID3"));
	}

	@Test
	public void testDefinePK()
		throws Exception
	{
		util.emptyBaseDirectory();
		WbConnection con = util.getConnection("pkTestDb");
		Statement stmt = con.createStatement();
		stmt.executeUpdate("CREATE TABLE junit_test (id1 integer, some_data varchar(100))");
		stmt.executeUpdate("insert into junit_test (id1, some_data) values (1,'General Failure')");
		stmt.executeUpdate("insert into junit_test (id1, some_data) values (2,'Major Bug')");
		con.commit();

		String sql = "select id1, some_data from JUnit_Test";

		ResultSet rs = stmt.executeQuery(sql);
		DataStore ds = new DataStore(rs, con);
		SqlUtil.closeAll(rs, stmt);

		ds.setGeneratingSql(sql);

		PkMapping.getInstance().addMapping("JUNIT_TEST", "id1");
		ds.updatePkInformation();
		assertTrue(ds.hasPkColumns());
		ds.setValue(0, 1, "Corporal Clegg");
		ds.updateDb(con, null);
		stmt = con.createStatement();
		rs = stmt.executeQuery("select some_data from junit_test where id1 = 1");
		if (rs.next())
		{
			String data = rs.getString(1);
			assertEquals("Corporal Clegg", data);
		}
		else
		{
			fail("No rows selected");
		}
		SqlUtil.closeAll(rs, stmt);
	}

	@Test
	public void testQuotedKeyColumns()
		throws Exception
	{
		try
		{
			WbConnection con = prepareDatabase();

			String[] cols = new String[] { "\"KEY\"", "LASTNAME", "FIRSTNAME", "LASTNAME" };
			int[] types = new int[] { java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.VARCHAR };

			int[] sizes = new int[]
			{
				20, 20, 20, 20
			};
			ResultInfo info = new ResultInfo(cols, types, sizes);
			DataStore ds = new DataStore(info);
			ds.setGeneratingSql("SELECT \"KEY\", LASTNAME, FIRSTNAME FROM JUNIT_TEST");
			TableIdentifier tbl = new TableIdentifier("JUNIT_TEST");
			ds.setUpdateTableToBeUsed(tbl);
			ds.checkUpdateTable(con);

			ResultInfo newInfo = ds.getResultInfo();
			assertEquals(newInfo.getColumnCount(), 4);
			assertEquals(newInfo.isPkColumn(0), true);
			assertEquals(ds.pkColumnsComplete(), true);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testPkDetection()
		throws Exception
	{
		try
		{
			WbConnection con = prepareDatabase();
			Statement stmt = con.createStatement();
			final String sql = "select key, firstname, lastname from junit_test";
			ResultSet rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, con);
			rs.close();
			ds.setGeneratingSql(sql);

			assertEquals("Non-existing primary key found", false, ds.hasPkColumns());
			ds.checkUpdateTable();
			assertEquals("Primary key not found", true, ds.hasPkColumns());
			assertEquals("Not all PK columns detected", ds.pkColumnsComplete(), true);

			stmt.executeUpdate("DROP TABLE junit_test");
			stmt.executeUpdate("CREATE TABLE junit_test (key integer, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("insert into junit_test (key, firstname, lastname) values (42, 'Zaphod', 'Beeblebrox')");
			stmt.executeUpdate("insert into junit_test (key, firstname, lastname) values (1, 'Mary', 'Moviestar')");
			con.commit();

			rs = stmt.executeQuery(sql);
			ds = new DataStore(rs, con);
			ds.setGeneratingSql(sql);
			rs.close();

			ds.updatePkInformation();
			assertEquals("Non-existing primary key found", false, ds.hasPkColumns());

			ResultInfo info = ds.getResultInfo();
			info.setIsPkColumn(0, true);
			assertEquals("Primary key not recognized", true, ds.hasPkColumns());

			ds.setValue(0, 1, "Arthur");
			ds.setValue(0, 2, "Dent");
			int rows = ds.updateDb(con, null);
			assertEquals("Incorrect number of rows updated", 1, rows);

			rs = stmt.executeQuery("select firstname, lastname from junit_test where key = 42");
			boolean hasRows = rs.next();
			assertEquals("No rows fetched", true, hasRows);

			String fname = rs.getString(1);
			String lname = rs.getString(2);
			assertEquals("Firstname incorrectly updated", "Arthur", fname);
			assertEquals("Lastname incorrectly updated", "Dent", lname);
			rs.close();

			rs = stmt.executeQuery("select firstname, lastname from junit_test where key = 1");
			hasRows = rs.next();
			assertEquals("No rows fetched", true, hasRows);

			fname = rs.getString(1);
			lname = rs.getString(2);
			assertEquals("Incorrect firstname affected", "Mary", fname);
			assertEquals("Incorrect lastname affected", "Moviestar", lname);
			rs.close();

			String mapping = "junit_test=key";
			File f = new File(util.getBaseDir(), "pk_mapping.properties");
			FileWriter w = new FileWriter(f);
			w.write(mapping);
			w.close();
			PkMapping.getInstance().loadMapping(f.getAbsolutePath());

			rs = stmt.executeQuery(sql);
			ds = new DataStore(rs, con);
			ds.setGeneratingSql(sql);
			rs.close();

			ds.updatePkInformation();
			assertEquals("Primary key from mapping not found", true, ds.hasPkColumns());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	private WbConnection prepareDatabase()
		throws Exception
	{
		util.emptyBaseDirectory();
		WbConnection wb = util.getConnection();
		Connection con = wb.getSqlConnection();
		TestUtil.executeScript(wb,
			"drop table if exists junit_test;\n" +
			"CREATE TABLE junit_test (key integer primary key, firstname varchar(100), lastname varchar(100));"
		);

		PreparedStatement pstmt = con.prepareStatement("insert into junit_test (key, firstname, lastname) values (?,?,?)");
		for (int i = 0; i < rowcount; i++)
		{
			pstmt.setInt(1, i);
			pstmt.setString(2, "FirstName" + i);
			pstmt.setString(3, "LastName" + i);
			pstmt.executeUpdate();
		}
		con.commit();
		return wb;
	}

	@Test
	public void testUpdate()
		throws Exception
	{
		WbConnection con = null;
		Statement stmt = null;
		try
		{
			con = prepareDatabase();

			stmt = con.createStatement();
			String sql = "select key, lastname, firstname from junit_test";
			ResultSet rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, con);
			SqlUtil.closeResult(rs);

			List<Alias> tbl = SqlUtil.getTables(sql, true, null);
			assertEquals("Wrong number of tables retrieved from SQL", 1, tbl.size());

			String table = tbl.get(0).getObjectName();
			assertEquals("Wrong update table returned", "junit_test", table);

			TableIdentifier id = new TableIdentifier(table);
			ds.setUpdateTable(id);
			assertEquals(rowcount, ds.getRowCount());

			ds.setValue(0, 1, "Dent");
			ds.setValue(0, 2, "Arthur");
			ds.updateDb(con, null);

			rs = stmt.executeQuery("select lastname, firstname from junit_test where key = 0");
			boolean hasNext = rs.next();
			assertEquals("Updated row not found", true, hasNext);
			String lastname = rs.getString(1);
			String firstname = rs.getString(2);
			assertEquals("Firstname not updated", "Arthur", firstname);
			assertEquals("Lastname not updated", "Dent", lastname);
			SqlUtil.closeResult(rs);

			rs = stmt.executeQuery("select lastname, firstname from junit_test where key = 1");
			hasNext = rs.next();
			assertEquals("Updated row not found", true, hasNext);
			lastname = rs.getString(1);
			firstname = rs.getString(2);
			assertEquals("Firstname updated", "FirstName1", firstname);
			assertEquals("Lastname updated", "LastName1", lastname);
			SqlUtil.closeResult(rs);

			int row = ds.addRow();
			ds.setValue(row, 0, new Integer(42));
			ds.setValue(row, 1, "Beeblebrox");
			assertEquals("Row not inserted", rowcount + 1, ds.getRowCount());
			ds.setValue(row, 2, "Zaphod");
			ds.updateDb(con, null);

			rs = stmt.executeQuery("select lastname, firstname from junit_test where key = 42");
			hasNext = rs.next();
			assertEquals("Updated row not found", true, hasNext);
			lastname = rs.getString(1);
			firstname = rs.getString(2);
			assertEquals("Firstname not updated", "Zaphod", firstname);
			assertEquals("Lastname not updated", "Beeblebrox", lastname);
			SqlUtil.closeResult(rs);

			stmt.executeUpdate("update junit_test set firstname = null where key = 42");
			con.commit();
			rs = stmt.executeQuery("select key, lastname, firstname from junit_test where key = 42");
			ds = new DataStore(rs, con);
			SqlUtil.closeResult(rs);
			ds.setUpdateTable(id);
			ds.setValue(0, 2, "Arthur");
			ds.updateDb(con, null);
			rs = stmt.executeQuery("select firstname from junit_test where key = 42");
			hasNext = rs.next();
			assertEquals("Updated row not found", true, hasNext);
			firstname = rs.getString(1);
			assertEquals("Arthur", firstname);

			rs = stmt.executeQuery("select key, lastname, firstname from junit_test where key = 42");
			ds = new DataStore(rs, con);
			SqlUtil.closeResult(rs);
			ds.setUpdateTable(id);
			ds.deleteRow(0);

			List<DmlStatement> statements = ds.getUpdateStatements(con);
			assertEquals(1, statements.size());

			ds.updateDb(con, null);

			rs = stmt.executeQuery("select count(*) from junit_test where key = 42");
			rs.next();
			int count = rs.getInt(1);
			assertEquals(0, count);

			SqlUtil.closeAll(rs, stmt);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testCascadingDelete()
		throws Exception
	{
		WbConnection con = null;
		try
		{
			con = util.getConnection();
			TestUtil.executeScript(con,
				"CREATE TABLE person (id integer primary key, firstname varchar(20), lastname varchar(20));\n" +
				"insert into person (id, firstname, lastname) values (42, 'Zaphod', 'Beeblebrox');\n" +
				"insert into person (id, firstname, lastname) values (1, 'Mary', 'Moviestar');\n" +
				"create table detail (did integer primary key, person_id integer, detail_info varchar(100));\n" +
				"alter table detail ADD CONSTRAINT fk_pers FOREIGN KEY (person_id) REFERENCES person (id);\n" +
				"insert into detail (did, person_id, detail_info) values (1, 42, 'some stuff');\n" +
				"insert into detail (did, person_id, detail_info) values (2, 42, 'more stuff');\n" +
				"insert into detail (did, person_id, detail_info) values (3, 1, 'mary1');\n" +
				"insert into detail (did, person_id, detail_info) values (4, 1, 'mary2');\n" +
				"commit;");

			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select id, firstname, lastname from person order by id");
			DataStore ds = new DataStore(rs, true);
			rs.close();
			assertEquals(2, ds.getRowCount());

			ds.setOriginalConnection(con);
			ds.setGeneratingSql("select id, firstname, lastname from person order by id");
			ds.deleteRowWithDependencies(0);
			ds.updateDb(con, null);
			rs = stmt.executeQuery("select count(*) from person");
			rs.next();
			int count = rs.getInt(1);
			assertEquals(1, count);
			rs.close();

			rs = stmt.executeQuery("select count(*) from detail");
			rs.next();
			count = rs.getInt(1);
			assertEquals(2, count);
			rs.close();
		}
		finally
		{
			con.disconnect();
		}
	}

	@Test
	public void testFilter()
		throws Exception
	{
		WbConnection con = null;
		Statement stmt = null;
		try
		{
			con = prepareDatabase();

			stmt = con.createStatement();
			stmt.executeUpdate("insert into junit_test (key, firstname, lastname) values (42, 'Zaphod', 'Beeblebrox')");
			con.commit();

			String sql = "select key, lastname, firstname from junit_test";
			ResultSet rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, con);

			ComplexExpression expr = new AndExpression();
			expr.addColumnExpression("FIRSTNAME", new StringEqualsComparator(), "Zaphod");
			expr.addColumnExpression("LASTNAME", new StartsWithComparator(), "Bee");

			ds.applyFilter(expr);
			assertEquals("AND Filter not correct", 1, ds.getRowCount());

			expr = new AndExpression();
			expr.addColumnExpression("KEY", new NumberEqualsComparator(), new Integer(100));
			ds.applyFilter(expr);
			assertEquals("Number Filter not correct", 0, ds.getRowCount());

			expr = new OrExpression();
			expr.addColumnExpression("KEY", new LessThanComparator(), new Integer(1));
			expr.addColumnExpression("FIRSTNAME", new StringEqualsComparator(), "Zaphod");

			ds.applyFilter(expr);
			assertEquals("OR Filter not correct", 2, ds.getRowCount());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}

	}

	@Test
	public void testUpdateCaseSensitive()
		throws Exception
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			util.emptyBaseDirectory();
			WbConnection con = util.getConnection();
			stmt = con.createStatement();
			stmt.executeUpdate("create table \"case_test\" (nr integer primary key, text_data varchar(100))");
			String sql = "select * from \"case_test\"";
			rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, con, true);
			ds.setGeneratingSql(sql);
			rs.close();

			ds.checkUpdateTable(con);
			int row = ds.addRow();
			ds.setValue(row, 0, new Integer(42));
			ds.setValue(row, 1, "TestData");
			ds.updateDb(con, null);

			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals(42, nr);

				String s = rs.getString(2);
				assertEquals("TestData", s);
			}
			else
			{
				fail("New rows not updated");
			}
			rs.close();
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testList()
		throws Exception
	{
		try
		{
			String[] cols = new String[] {"CHAR", "INT", "DOUBLE"};
			int[] types = new int[] {Types.VARCHAR, Types.INTEGER, Types.DOUBLE};
			DataStore ds = new DataStore(cols, types);
			ds.addRow();
			assertEquals(1, ds.getRowCount());
			String s = "Testvalue";
			Integer i = new Integer(42);
			Double d = new Double(4.2);
			ds.setValue(0, 0, s);
			ds.setValue(0, 1, i);
			ds.setValue(0, 2, d);
			assertEquals(s, ds.getValue(0, 0));
			assertEquals(i, ds.getValue(0, 1));
			assertEquals(i.intValue(), ds.getValueAsInt(0, 1, -1));
			assertEquals(d, ds.getValue(0, 2));
			ds.resetStatus();

			String newValue = "Newvalue";
			ds.setValue(0, 0, newValue);
			assertEquals(newValue, ds.getValue(0, 0));
			assertEquals(s, ds.getOriginalValue(0, 0));
			assertEquals(true, ds.isModified());
			ds.resetStatus();
			assertEquals(false, ds.isModified());
			assertEquals(newValue, ds.getOriginalValue(0, 0));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testSort()
		throws Exception
	{
		int[] types = new int[] {Types.VARCHAR};
		String[] cols = new String[] { "NAME" };
		DataStore ds = new DataStore(cols, types);

		ds.addRow();
		ds.setValue(0, 0, "ZZZ");

		ds.addRow();
		ds.setValue(1, 0, "AAA");

		ds.addRow();
		ds.setValue(2, 0, "WWW");

		ds.addRow();
		ds.setValue(3, 0, null);
		ds.sortByColumn(0, true);

		assertEquals("AAA", ds.getValueAsString(0, 0));
	}

 @Test
  public void testDuplicateNames()
    throws Exception
  {
		WbConnection con = util.getConnection();

    String sql =
      "CREATE TABLE one (ident int, refid int, PRIMARY KEY(ident));\n" +
      "CREATE TABLE two (ident int, refid int, PRIMARY KEY(ident));\n" +
      "INSERT INTO one VALUES (1, 10), (2, 11), (3, 12);\n" +
      "INSERT INTO two VALUES (3, 10), (4, 11), (5, 12);\n" +
      "commit;";

		TestUtil.executeScript(con, sql);

    // Test 1 with outdated joins and without table aliases
    String query =
      "SELECT one.ident, two.ident \n" +
      "FROM one, two \n" +
      "WHERE one.refid = two.refid \n" +
      "ORDER BY 1";

    DataStore ds = null;
		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(query))
		{
      ds = new DataStore(rs, con, true);
		}
    ds.setGeneratingSql(query);

//    DataStorePrinter printer = new DataStorePrinter(ds);
//    printer.printTo(System.out);

    assertEquals(3, ds.getRowCount());
    ds.deleteRow(1);
    ds.setUpdateTable("two", con);
    List<DmlStatement> updateStatements = ds.getUpdateStatements(con);
    assertEquals(1, updateStatements.size());
    SqlLiteralFormatter formatter = new SqlLiteralFormatter(con);

    updateStatements.get(0).setFormatSql(false);
    String delete = updateStatements.get(0).getExecutableStatement(formatter).toString();
    assertEquals("DELETE FROM TWO WHERE IDENT = 4", delete.toUpperCase());

    // Test2 with proper JOINs and table aliases
    query =
      "SELECT o.ident, t.ident \n" +
      "FROM one o \n" +
      "  JOIN two t ON o.refid = t.refid \n" +
      "ORDER BY 1";

    // no need to re-run the query.
    // when checking the update table, only the generating SQL is used to detect the tables for the columns
    // the deleted row is still marked as deleted
    ds.setGeneratingSql(query);

    ds.setUpdateTable("two", con);
    updateStatements = ds.getUpdateStatements(con);
    assertEquals(1, updateStatements.size());

    updateStatements.get(0).setFormatSql(false);
    delete = updateStatements.get(0).getExecutableStatement(formatter).toString();
    assertEquals("DELETE FROM TWO WHERE IDENT = 4", delete.toUpperCase());

    // Test 3 with using a fully qualified table without alias
    query =
      "SELECT o.ident, two.ident \n" +
      "FROM one o \n" +
      "  JOIN public.two ON o.refid = two.refid \n" +
      "ORDER BY 1";

		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(query))
		{
      ds = new DataStore(rs, con, true);
		}
    ds.setGeneratingSql(query);
    assertEquals(3, ds.getRowCount());

    ds.deleteRow(1);
    ds.setUpdateTable("public.two", con);
    updateStatements = ds.getUpdateStatements(con);
    assertEquals(1, updateStatements.size());

    updateStatements.get(0).setFormatSql(false);
    delete = updateStatements.get(0).getExecutableStatement(formatter).toString();
    assertEquals("DELETE FROM TWO WHERE IDENT = 4", delete.toUpperCase());
  }
}
