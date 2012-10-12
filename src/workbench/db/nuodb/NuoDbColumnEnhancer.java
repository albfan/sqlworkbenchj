/*
 * FirebirdColumnEnhancer
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.nuodb;


import java.sql.PreparedStatement;
import java.sql.ResultSet;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * A class to read additional column level information for a table.
 *
 * @author Thomas Kellerer
 */
public class NuoDbColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		readIdentityColumns(table, conn);
	}

	private void readIdentityColumns(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		TableIdentifier tblId = table.getTable();

		String sql =
			"select field \n" +
			"from system.fields \n " +
			"where tablename = ? \n" +
			"and schema = ? \n" +
			"and generator_sequence is not null ";

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, tblId.getRawTableName());
			stmt.setString(2, tblId.getRawSchema());

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				ColumnIdentifier col = table.findColumn(colname);
				if (col != null)
				{
					col.setIsAutoincrement(true);
					col.setComputedColumnExpression("GENERATED ALWAYS AS IDENTITY");
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("NuoDbColumnEnhancer.readIdentityColumns()", "Error retrieving computed columns", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}
}
