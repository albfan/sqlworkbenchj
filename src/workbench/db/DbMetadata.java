/*
 * DbMetadata.java
 *
 * Created on 16. Juli 2002, 13:09
 */

package workbench.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class DbMetadata
{
	private DatabaseMetaData metaData;

	/** Creates a new instance of DbMetadata */
	public DbMetadata(WbConnection aConnection)
		throws SQLException
	{
		Connection c = aConnection.getSqlConnection();
		this.metaData = c.getMetaData();
	}

	public ResultSet getTableDefinition(String aTablename)
		throws SQLException
	{
		ResultSet result = this.metaData.getColumns(null, null, aTablename, "%");
		return result;
	}

	public ResultSet getTables()
		throws SQLException
	{
		ResultSet result = this.metaData.getTables(null, null, null, null);
		return result;
	}

	public boolean storesUpperCaseIdentifiers()
	{
		try
		{
			return this.metaData.storesUpperCaseIdentifiers();
		}
		catch (SQLException e)
		{
			return false;
		}
	}
	
	public boolean storesLowerCaseIdentifiers()
	{
		try
		{
			return this.metaData.storesLowerCaseIdentifiers();
		}
		catch (SQLException e)
		{
			return false;
		}
	}
}
