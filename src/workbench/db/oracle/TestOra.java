/*
 * TestOra.java
 *
 * Created on September 3, 2002, 9:35 AM
 */

package workbench.db.oracle;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author  workbench@kellerer.org
 */
public class TestOra
{
	
	public static void main(String args [])
		throws SQLException
	{
		try
		{
			//DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
			Class.forName("oracle.jdbc.OracleDriver");
			Connection con;
			//con = DriverManager.getConnection("jdbc:oracle:thin:@dbserver:1521:ora8i","scott", "tiger");
			con = DriverManager.getConnection("jdbc:oracle:thin:@DEMRDB34:1521:SBL1", "sadmin", "sadmin");
			con.setAutoCommit(false);

			Statement stmt = con.createStatement();

			DbmsOutput dbmsOutput = new DbmsOutput(con);

			dbmsOutput.enable(1000000);

			stmt.execute( "begin dbms_output.put_line('Hello, world'); end;"); 
			stmt.execute( "begin dbms_output.put_line('Hello, world2'); end;"); 
			
			System.out.println("Output=");
			System.out.println(dbmsOutput.getResult());

			dbmsOutput.close();
			stmt.close();
			con.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
}
