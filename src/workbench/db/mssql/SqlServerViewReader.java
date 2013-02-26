/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.mssql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.DefaultViewReader;
import workbench.db.NoConfigException;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerViewReader
	extends DefaultViewReader
{

	public SqlServerViewReader(WbConnection con)
	{
		super(con);
	}

	@Override
	public CharSequence getViewSource(TableIdentifier viewId)
		throws NoConfigException
	{
		String currentDb = this.connection.getCurrentCatalog();
		String viewDb = viewId.getCatalog();
		CharSequence sql = null;

		boolean changeCatalog = !StringUtil.equalString(currentDb, viewDb) && StringUtil.isNonBlank(viewDb);
		Statement stmt = null;
		ResultSet rs = null;

		try
		{
			if (changeCatalog)
			{
				setCatalog(viewDb);
			}
			stmt = connection.createStatement();

			String query = "sp_helptext [" + viewId.getRawSchema() + "." + viewId.getRawTableName() + "]";
			boolean hasResult = stmt.execute(query);

			if (hasResult)
			{
				rs = stmt.getResultSet();
				StringBuilder source = new StringBuilder(1000);
				while (rs.next())
				{
					source.append(rs.getString(1));
				}
				sql = source;
			}
		}
		catch (SQLException ex)
		{
			sql = ex.getMessage();
		}
		finally
		{
			if (changeCatalog)
			{
				setCatalog(currentDb);
			}
			SqlUtil.closeAll(rs, stmt);
		}
		return sql;
	}

	private void setCatalog(String newCatalog)
	{
		try
		{
			connection.getSqlConnection().setCatalog(newCatalog);
		}
		catch (SQLException ex)
		{
			LogMgr.logWarning("SqlServerViewReader.setCatalog()", "Could not change database", ex);
		}
	}


}
