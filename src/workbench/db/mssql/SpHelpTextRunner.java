/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.mssql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.db.NoConfigException;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SpHelpTextRunner
{

	public CharSequence getSource(WbConnection connection, String dbName, String schemaName, String objectName)
		throws NoConfigException
	{
		String currentDb = connection.getCurrentCatalog();
		CharSequence sql = null;

		boolean changeCatalog = !StringUtil.equalString(currentDb, dbName) && StringUtil.isNonBlank(dbName);
		Statement stmt = null;
		ResultSet rs = null;

		String query = "sp_helptext [" + schemaName + "." + objectName + "]";

		try
		{
			if (changeCatalog)
			{
				setCatalog(connection, dbName);
			}
			stmt = connection.createStatement();

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("SpHelpTextRunner.getSource()", "Using query: " + query);
			}

			boolean hasResult = stmt.execute(query);

			if (hasResult)
			{
				rs = stmt.getResultSet();
				StringBuilder source = new StringBuilder(1000);
				while (rs.next())
				{
					String line = rs.getString(1);
					if (line != null)
					{
						source.append(rs.getString(1));
					}
				}
				StringUtil.trimTrailingWhitespace(source);
				sql = source;
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("SpHelpTextRunner.getSource()", "Could not run: " + query, ex);
			sql = ex.getMessage();
		}
		finally
		{
			if (changeCatalog)
			{
				setCatalog(connection, currentDb);
			}
			SqlUtil.closeAll(rs, stmt);
		}
		return sql;
	}

	private void setCatalog(WbConnection connection, String newCatalog)
	{
		try
		{
			connection.getSqlConnection().setCatalog(newCatalog);
		}
		catch (SQLException ex)
		{
			LogMgr.logWarning("SpHelpTextRunner.setCatalog()", "Could not change database", ex);
		}
	}

}
