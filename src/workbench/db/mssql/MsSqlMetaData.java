/*
 * MsSqlMetaData.java
 *
 * Created on May 5, 2004, 5:59 PM
 */

package workbench.db.mssql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author  workbench@kellerer.org
 */
public class MsSqlMetaData
{
	private Connection dbConn = null;
	private PreparedStatement procStatement = null;
	
	/** Creates a new instance of MsSqlMetaData */
	public MsSqlMetaData(Connection con)
	{
		this.dbConn = con;
	}
	
	/**
	 *	The MS JDBC driver does not return the PROCEDURE_TYPE column correctly
	 *  so we implement it ourselves (MS always returns RESULT which is 
	 *  - strictly speaking - true, but as MS still distinguished between 
	 *  procedures and functions we need to return this correctly
	 */
	public ResultSet getProcedures(String catalog, String schema)
		throws SQLException
	{
		if (this.procStatement == null)
		{
			this.procStatement = this.dbConn.prepareStatement(this.GET_PROC_SQL);
		}
		if (catalog == null)
		{
			this.procStatement.setString(1, "%");
		}
		else
		{
			this.procStatement.setString(1, catalog);
		}
		ResultSet rs = this.procStatement.executeQuery();
		return rs;
	}
	
	public void closeStatement()
	{
		if (this.procStatement != null)
		{
			try
			{
				this.procStatement.close();
			}
			catch (Throwable e)
			{
			}
		}
	}

	public void done()
	{
		this.closeStatement();
		this.procStatement = null;
	}

	private static final String GET_PROC_SQL = 
					 "select db_name()  PROCEDURE_CAT, \n" + 
           "	  convert(sysname,user_name(o.uid))  PROCEDURE_SCHEM, \n" + 
           "	  convert(nvarchar(134),o.name) PROCEDURE_NAME, \n" + 
           "	  null, \n" + 
           "	  null, \n" + 
           "	  null, \n" + 
           "	  null REMARKS, \n" + 
           "	  case type  \n" + 
           "	    when 'P' then 1 \n" + 
           "	    when 'FN' then 2 \n" + 
					 "      else 0 \n " + 
           "	  end PROCEDURE_TYPE \n" + 
           "    from  sysobjects o  \n" + 
           "    where o.type in ('P', 'FN', 'TF', 'IF') \n" + 
           "      and permissions (o.id)&32 <> 0 \n" + 
           "      and user_name(o.uid) like ? \n" + 
           "	order by 2, 3 \n";	
}