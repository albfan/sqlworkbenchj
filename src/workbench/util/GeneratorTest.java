/*
 * GeneratorTest.java
 *
 * Created on 2. November 2002, 01:04
 */

package workbench.util;

import java.lang.ClassNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

public class GeneratorTest
{
	
	private static Connection getConnection()
		throws SQLException, ClassNotFoundException
	{
		Connection con;
		Class.forName("org.hsqldb.jdbcDriver");
		//Class.forName("com.inet.tds.TdsDriver");
		//Class.forName("oracle.jdbc.OracleDriver");
		//Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		//con = DriverManager.getConnection("jdbc:inetdae:demsqlvisa02:1433?database=visa_cpl_test", "visa", "savivisa");
		//con = DriverManager.getConnection("jdbc:inetdae:reosqlpro08:1433?database=visa", "visa", "savivisa");
		//con = DriverManager.getConnection("jdbc:oracle:thin:@DEMRDB34:1521:SBL1", "sadmin", "sadmin");
		//con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "auto", "auto");
		//con = DriverManager.getConnection("jdbc:oracle:oci8:@oradb", "auto", "auto");
		//con = DriverManager.getConnection("jdbc:odbc:Patsy");
		//con = DriverManager.getConnection("jdbc:hsqldb:d:\\daten\\db\\hsql\\test", "sa", null);
		con = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost", "sa", null);

		return con;
	}

	public static void generate()
	{
		WbConnection con = null;
		try
		{
			con = new WbConnection(getConnection());
			TableIdentifier t = new TableIdentifier(null, null, "expense");
			PersistenceClassGenerator g = new PersistenceClassGenerator();
			g.setConnection(con);
			//g.setOutputDir("d:\\projects\\java\\jworkbench\\src\\workbench\\util");
			//g.setPackageName("workbench.util");
			g.setTable(t);
			//System.out.println(g.generateTableClass());
			//System.out.println(g.generateValueClass());
			g.generateFiles(false);
			System.out.println("done.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { con.close(); } catch (Throwable th) {}
		}
	}
	public static void testPersistence()
	{
		Connection con = null;
		try
		{
			con = getConnection();
			//ExpensePersistence p = new ExpensePersistence();
			//p.setConnection(con);
			//ExpenseValueObject v = p.getExpensePkValueObject();
			//v.setExp_id(62);
			//ExpenseValueObject data = (ExpenseValueObject)p.selectRow(v);
			//System.out.println(data);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { con.close(); } catch (Throwable th) {}
		}
	}
	
	public static void main (String args[])
	{
		//generate();
		testPersistence();
	  /*
		try
		{
			
			ArrayList pks = new ArrayList();
			pks.add("nr");
			BaseTablePersistence b = new BaseTablePersistence();
			b.setTablename("test");
			b.addPkColumn("nr");
			b.addColumn("nr");
			b.addColumn("name");
			b.setConnection(con);
			
			BaseValueObject newValue = b.getValueObject();
			newValue.setColumnValue("name", "third row");
			newValue.setColumnValue("nr", new Integer(3));

			BaseValueObject oldValue = b.getPkValueObject();
			oldValue.setColumnValue("nr", new Integer(1));
			
			int count = 0;
			//count = b.insertRow(newValue);
			//count = b.deleteRow(oldValue);
			//count = b.updateRow(newValue, oldValue);
			//BaseValueObject data = b.selectRow(oldValue);
			//System.out.println("data=" + data);
			con.commit();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		finally
		{
			try { con.close(); } catch (Throwable th) {}
		}
		*/
	}
	
}
