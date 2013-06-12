/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceOptions;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class InformixTableSourceBuilder
	extends TableSourceBuilder
{

	public InformixTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	public void readTableOptions(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		TableSourceOptions option = table.getSourceOptions();
		if (!option.isInitialized())
		{
			readLockMode(table);
			option.setInitialized();
		}
	}

	private void readLockMode(TableIdentifier table)
	{
		String sql =
			"select locklevel \n" +
			"from systables \n" +
			"where tabname = ? \n" +
			"  and owner = ? \n";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("InformixSynonymReader.readLockMode()", "Using query=\n" + sql);
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try
		{
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getRawTableName());
			stmt.setString(2, table.getRawSchema());

			rs = stmt.executeQuery(sql.toString());
			if (rs.next())
			{
				TableSourceOptions option = table.getSourceOptions();
				String lvl = rs.getString(1);
				LogMgr.logDebug("InformixTableSourceBuilder.readLockMode()", "Lockmode for " + table.getTableExpression() + "is: " + lvl);
				if (StringUtil.isNonEmpty(lvl))
				{
					switch (lvl.charAt(0))
					{
						case 'B':
						case 'P':
							option.setTableOption("LOCK MODE PAGE");
							break;
						case 'R':
							option.setTableOption("LOCK MODE ROW");
					}
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("InformixTableSourceBuilder.readLockMode()", "Error when retrieving lock mode", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}
}
