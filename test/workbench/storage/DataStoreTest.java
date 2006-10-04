/*
 * DataStoreTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import junit.framework.*;
import java.sql.Types;
import java.util.List;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.storage.filter.AndExpression;
import workbench.storage.filter.ComplexExpression;
import workbench.storage.filter.LessThanComparator;
import workbench.storage.filter.NumberEqualsComparator;
import workbench.storage.filter.OrExpression;
import workbench.storage.filter.StartsWithComparator;
import workbench.storage.filter.StringEqualsComparator;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class DataStoreTest 
	extends TestCase
{
	private final int rowcount = 10;
	private TestUtil util;
	
	public DataStoreTest(String testName)
		throws Exception
	{
		super(testName);
		this.util = new TestUtil();
		util.prepareEnvironment();
	}

	private WbConnection preparePkTestDb()
		throws Exception
	{
		util.emptyBaseDirectory();
		String basedir = util.getBaseDir();
		String dbName = util.getDbName();
		WbConnection wb = util.getConnection();
		Connection con = wb.getSqlConnection();
		Statement stmt = con.createStatement();
		stmt.executeUpdate("CREATE TABLE junit_test (nr integer primary key, firstname varchar(100), lastname varchar(100))");
		stmt.close();
		PreparedStatement pstmt = con.prepareStatement("insert into junit_test (nr, firstname, lastname) values (?,?,?)");
		for (int i=0; i < rowcount; i ++)
		{
			pstmt.setInt(1, i);
			pstmt.setString(2, "FirstName" + i);
			pstmt.setString(3, "LastName" + i);
			pstmt.executeUpdate();
		}
		con.commit();
		return wb;
	}
	
	public void testCasePreserving()
	{
		WbConnection con = null;
		Statement stmt = null;
		try
		{
			con = prepareRetrieveDatabase();

			stmt = con.createStatement();
			stmt.executeUpdate("delete from junit_test");
			con.commit();
			
			String sql = "select nr, lastname, firstname from JUnit_Test";
			ResultSet rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, con);
			ds.setGeneratingSql(sql);

			int row = ds.addRow();
			ds.setValue(row, 0, new Integer(1));
			ds.setValue(row, 1, "Dent");
			ds.setValue(row, 2, "Arthur");
			
			ds.checkUpdateTable(con);
			
			// Set to case preserving
			Settings.getInstance().setGeneratedSqlTableCase("original");
			List l = ds.getUpdateStatements(con);
			assertEquals("Wrong number of update statements", 1, l.size());
			
			DmlStatement dml = (DmlStatement)l.get(0);
			SqlLiteralFormatter f = new SqlLiteralFormatter(con);
			String insert = dml.getExecutableStatement(f);
			String verb = SqlUtil.getSqlVerb(insert);
			assertEquals("Wrong statement generated", "INSERT", verb.toUpperCase());
			String table = SqlUtil.getInsertTable(insert);
			assertEquals("JUnit_Test", table);
			
			// Set to upper case
			Settings.getInstance().setGeneratedSqlTableCase("upper");
			l = ds.getUpdateStatements(con);
			assertEquals("Wrong number of update statements", 1, l.size());
			
			dml = (DmlStatement)l.get(0);
			insert = dml.getExecutableStatement(f);
			table = SqlUtil.getInsertTable(insert);
			assertEquals("JUNIT_TEST", table);

			// Test lower case
			Settings.getInstance().setGeneratedSqlTableCase("lower");
			l = ds.getUpdateStatements(con);
			assertEquals("Wrong number of update statements", 1, l.size());
			
			dml = (DmlStatement)l.get(0);
			insert = dml.getExecutableStatement(f);
			table = SqlUtil.getInsertTable(insert);
			assertEquals("junit_test", table);
			
			ds.resetStatus();
			ds.setValue(0, 1, "Dent2");

			// Test uppercase 
			Settings.getInstance().setGeneratedSqlTableCase("upper");
			l = ds.getUpdateStatements(con);
			assertEquals("Wrong number of update statements", 1, l.size());
			
			dml = (DmlStatement)l.get(0);
			String update = dml.getExecutableStatement(f);
			verb = SqlUtil.getSqlVerb(update);
			table = SqlUtil.getUpdateTable(update);
			assertEquals("UPDATE", verb);
			assertEquals("JUNIT_TEST", table);
			
			// Test lower case
			Settings.getInstance().setGeneratedSqlTableCase("lower");
			l = ds.getUpdateStatements(con);
			assertEquals("Wrong number of update statements", 1, l.size());
			
			dml = (DmlStatement)l.get(0);
			update = dml.getExecutableStatement(f);
			table = SqlUtil.getUpdateTable(update);
			assertEquals("junit_test", table);

			// Test lower case
			Settings.getInstance().setGeneratedSqlTableCase("original");
			l = ds.getUpdateStatements(con);
			assertEquals("Wrong number of update statements", 1, l.size());
			
			dml = (DmlStatement)l.get(0);
			update = dml.getExecutableStatement(f);
			table = SqlUtil.getUpdateTable(update);
			assertEquals("JUnit_Test", table);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
	
	public void testPkDetection()
	{
		try
		{
			WbConnection con = preparePkTestDb();
			Statement stmt = con.createStatement();
			final String sql = "select nr, firstname, lastname from junit_test";
			ResultSet rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, con);
			rs.close();
			ds.setGeneratingSql(sql);
			
			assertEquals("Non-existing primary key found", false, ds.hasPkColumns());
			ds.checkUpdateTable();
			assertEquals("Primary key not found", true, ds.hasPkColumns());
			
			stmt.executeUpdate("DROP TABLE junit_test");
			stmt.executeUpdate("CREATE TABLE junit_test (nr integer, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("insert into junit_test (nr, firstname, lastname) values (42, 'Zaphod', 'Beeblebrox')");
			stmt.executeUpdate("insert into junit_test (nr, firstname, lastname) values (1, 'Mary', 'Moviestar')");
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
			
			rs = stmt.executeQuery("select firstname, lastname from junit_test where nr = 42");
			boolean hasRows = rs.next();
			assertEquals("No rows fetched", true, hasRows);
			
			String fname = rs.getString(1);
			String lname = rs.getString(2);
			assertEquals("Firstname incorrectly updated", "Arthur", fname);
			assertEquals("Lastname incorrectly updated", "Dent", lname);
			rs.close();
			
			rs = stmt.executeQuery("select firstname, lastname from junit_test where nr = 1");
			hasRows = rs.next();
			assertEquals("No rows fetched", true, hasRows);
			
			fname = rs.getString(1);
			lname = rs.getString(2);
			assertEquals("Incorrect firstname affected", "Mary", fname);
			assertEquals("Incorrect lastname affected", "Moviestar", lname);
			rs.close();
			
			String mapping = "junit_test=nr";
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
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
	

	private WbConnection prepareRetrieveDatabase()
		throws Exception
	{
		util.emptyBaseDirectory();
		String basedir = util.getBaseDir();
		String dbName = util.getDbName();
		WbConnection wb = util.getConnection();
		Connection con = wb.getSqlConnection();
		Statement stmt = con.createStatement();
		stmt.executeUpdate("CREATE TABLE junit_test (nr integer primary key, firstname varchar(100), lastname varchar(100))");
		stmt.close();
		PreparedStatement pstmt = con.prepareStatement("insert into junit_test (nr, firstname, lastname) values (?,?,?)");
		for (int i=0; i < rowcount; i ++)
		{
			pstmt.setInt(1, i);
			pstmt.setString(2, "FirstName" + i);
			pstmt.setString(3, "LastName" + i);
			pstmt.executeUpdate();
		}
		con.commit();
		return wb;
	}

	public void testUpdate()
	{
		WbConnection con = null;
		Statement stmt = null;
		try
		{
			con = prepareRetrieveDatabase();
			
			stmt = con.createStatement();
			String sql = "select nr, lastname, firstname from junit_test";
			ResultSet rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, con);
			SqlUtil.closeResult(rs);
			
			List tbl = SqlUtil.getTables(sql);
			assertEquals("Wrong number of tables retrieved from SQL", 1, tbl.size());
			
			String table = (String)tbl.get(0);
			assertEquals("Wrong update table returned", "junit_test", table);
			
			ds.setUpdateTable(table);
			assertEquals(rowcount, ds.getRowCount());
			
			ds.setValue(0, 1, "Dent");
			ds.setValue(0, 2, "Arthur");
			ds.updateDb(con, null);
			
			rs = stmt.executeQuery("select lastname, firstname from junit_test where nr = 0");
			boolean hasNext = rs.next();
			assertEquals("Updated row not found", true, hasNext);
			String lastname = rs.getString(1);
			String firstname = rs.getString(2);
			assertEquals("Firstname not updated", "Arthur", firstname);
			assertEquals("Lastname not updated", "Dent", lastname);
			SqlUtil.closeResult(rs);
			
			rs = stmt.executeQuery("select lastname, firstname from junit_test where nr = 1");
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
			
			rs = stmt.executeQuery("select lastname, firstname from junit_test where nr = 42");
			hasNext = rs.next();
			assertEquals("Updated row not found", true, hasNext);
			lastname = rs.getString(1);
			firstname = rs.getString(2);
			assertEquals("Firstname not updated", "Zaphod", firstname);
			assertEquals("Lastname not updated", "Beeblebrox", lastname);
			SqlUtil.closeAll(rs, stmt);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
	
	public void testFilter()
	{
		WbConnection con = null;
		Statement stmt = null;
		try
		{
			con = prepareRetrieveDatabase();
			
			stmt = con.createStatement();
			stmt.executeUpdate("insert into junit_test (nr, firstname, lastname) values (42, 'Zaphod', 'Beeblebrox')");
			con.commit();
			
			String sql = "select nr, lastname, firstname from junit_test";
			ResultSet rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, con);
			
			ComplexExpression expr = new AndExpression();
			expr.addColumnExpression("FIRSTNAME", new StringEqualsComparator(), "Zaphod");
			expr.addColumnExpression("LASTNAME", new StartsWithComparator(), "Bee");
			
			ds.applyFilter(expr);
			assertEquals("AND Filter not correct", 1, ds.getRowCount());
			
			expr = new AndExpression();
			expr.addColumnExpression("NR", new NumberEqualsComparator(), new Integer(100));
			ds.applyFilter(expr);
			assertEquals("Number Filter not correct", 0, ds.getRowCount());
			
			expr = new OrExpression();
			expr.addColumnExpression("NR", new LessThanComparator(), new Integer(1));
			expr.addColumnExpression("FIRSTNAME", new StringEqualsComparator(), "Zaphod");
			
			ds.applyFilter(expr);
			assertEquals("OR Filter not correct", 2, ds.getRowCount());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
		
	}

	public void testList()
	{
		try
		{
			String[] cols = new String[] { "CHAR", "INT", "DOUBLE"};
			int[] types = new int[] { Types.VARCHAR, Types.INTEGER, Types.DOUBLE };
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
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}
}
