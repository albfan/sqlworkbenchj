/*
 * DataStoreTest.java
 * JUnit based test
 *
 * Created on 20. Juni 2006, 22:48
 */

package workbench.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import junit.framework.*;
import java.sql.Types;
import workbench.TestUtil;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
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
	private String dbName;
	private String basedir;
	private final int rowcount = 10;
	
	public DataStoreTest(String testName)
	{
		super(testName);
		try
		{
			TestUtil util = new TestUtil();
			util.prepareEnvironment();
			this.basedir = util.getBaseDir();
			this.dbName = util.getDbName();
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	private WbConnection prepareDatabase()
		throws SQLException, ClassNotFoundException
	{
		File dir = new File(basedir);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			files[i].delete();
		}
		Class.forName("org.hsqldb.jdbcDriver");
		String url = "jdbc:hsqldb:" + dbName + ";shutdown=true";
		Connection con = DriverManager.getConnection(url, "sa", "");
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
		WbConnection wb = new WbConnection(con);
		
		// Create a dummy profiles because the profile
		// is used all around the Wb sources
		ConnectionProfile prof = new ConnectionProfile();
		prof.setAutocommit(false);
		prof.setEmptyStringIsNull(true);
		prof.setIgnoreDropErrors(true);
		prof.setIncludeNullInInsert(true);
		prof.setName("JunitTest");
		prof.setDriverName("HSQLDB");
		prof.setDriverclass("org.hsqldb.jdbcDriver");
		prof.setUrl(url);
		prof.setPassword("");
		wb.setProfile(prof);
		return wb;
	}

	public void testRetrieve()
	{
		WbConnection con = null;
		Statement stmt = null;
		try
		{
			con = prepareDatabase();
			
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, lastname, firstname from junit_test");
			DataStore ds = new DataStore(rs, con);
			ds.setUpdateTable("junit_test");
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
			assertEquals("Firstname not updated", "FirstName1", firstname);
			assertEquals("Lastname not updated", "LastName1", lastname);
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
			try { con.getSqlConnection().close(); } catch (Throwable th) {}
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
