/*
 * OracleFKHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.db.DefaultFKHandler;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 * A class to fix the bug in Oracle's JDBC that causes foreign keys that reference unique constraints
 * are not returned.
 *
 * @author Thomas Kellerer
 */
public class OracleFKHandler
	extends DefaultFKHandler
{
	// This is essentially a copy of the Statement used by the Oracle driver
	final String baseSql =
			"SELECT NULL AS pktable_cat, \n" +
			"       p.owner AS pktable_schem, \n" +
			"       p.table_name AS pktable_name, \n" +
			"       pc.column_name AS pkcolumn_name, \n" +
			"       NULL AS fktable_cat, \n" +
			"       f.owner AS fktable_schem, \n" +
			"       f.table_name AS fktable_name, \n" +
			"       fc.column_name AS fkcolumn_name, \n" +
			"       fc.position AS key_seq, \n" +
			"       NULL AS update_rule, \n" +
			"       decode (f.delete_rule, \n" +
			"              'CASCADE', 0, \n" +
			"              'SET NULL', 2, \n" +
			"              1 \n" +
			"       ) AS delete_rule, \n" +
			"       f.constraint_name AS fk_name, \n" +
			"       p.constraint_name AS pk_name, \n" +
			"       decode(f.deferrable, \n" +
			"             'DEFERRABLE',    5, \n" +
			"             'NOT DEFERRABLE',7, \n" +
			"             'DEFERRED',      6       \n" +
			"       ) deferrability \n" +
			"FROM all_cons_columns pc, \n" +
			"     all_constraints p, \n" +
			"     all_cons_columns fc, \n" +
			"     all_constraints f \n" +
			"WHERE f.constraint_type = 'R'  \n" +
			"AND p.owner = f.r_owner  \n" +
			"AND p.constraint_name = f.r_constraint_name  \n" +
			"AND p.constraint_type IN  ('P', 'U') \n" +  // this is the difference to the original statement from the Oracle driver (it uses = 'P')
			"AND pc.owner = p.owner  \n" +
			"AND pc.constraint_name = p.constraint_name  \n" +
			"AND pc.table_name = p.table_name  \n" +
			"AND fc.owner = f.owner  \n" +
			"AND fc.constraint_name = f.constraint_name  \n" +
			"AND fc.table_name = f.table_name  \n" +
			"AND fc.position = pc.position \n";

	public OracleFKHandler(WbConnection conn)
	{
		super(conn);
	}

	@Override
	protected DataStore getRawKeyList(TableIdentifier tbl, boolean exported)
		throws SQLException
	{
		try
		{
			if (exported)
			{
				return getExportedKeyList(tbl);
			}
			else
			{
				return getImportedKeyList(tbl);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleFKHandler.getRawKeyList()", "Could not retrieve foreign keys", e);
		}
		// something went wrong, use the driver's implementation
		return super.getRawKeyList(tbl, exported);
	}

	/**
	 * Adjust the baseSql query to reflect if a table for the current user is queried.
	 *
	 * If the table belongs to the current user, the user_XXX views can be used
	 * instead of the all_XXX views. Using the user_XXX views is faster (at least on my system) than the all_XXX
	 * views - although it  is still an awfully slow statement...
	 * <br>
	 * Querying user_constraints instead of all_constraints means that constraints between two schemas
	 * will not be shown. In order to still enable this, the config property:
	 * <br>
	 * <code>workbench.db.oracle.optimize_fk_query</code>
	 * <br>
	 * can be set to false, if all_constraints should always be queried.
	 *
	 * @param schema
	 * @return the query to use
	 */
	private String getQuery(TableIdentifier tbl)
	{
		boolean optimize = Settings.getInstance().getBoolProperty("workbench.db.oracle.optimize_fk_query", true);
		if (optimize)
		{
			String user = getConnection().getProfile().getUsername().toUpperCase();
			String schema = tbl.getRawSchema();
			boolean isOwner = schema.equals(user);
			if (isOwner)
			{
				String sql = baseSql.replace("all_constraints", "user_constraints");
				sql = sql.replace("all_cons_columns", "user_cons_columns");
				return sql;
			}
		}
		return baseSql;
	}

	private DataStore getExportedKeyList(TableIdentifier tbl)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 50);
		sql.append(getQuery(tbl));
		sql.append("AND p.table_name = ? \n");
		sql.append("AND p.owner = ? \n");
		sql.append("ORDER BY fktable_schem, fktable_name, key_seq");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleFKHandler.getExportedKeyList()", "Using: " + sql);
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			pstmt = this.getConnection().getSqlConnection().prepareStatement(sql.toString());
			pstmt.setString(1, tbl.getRawTableName());
			pstmt.setString(2, tbl.getRawSchema());
			rs = pstmt.executeQuery();
			result = processResult(rs);
		}
		finally
		{
			// the result set is closed by processResult
			SqlUtil.closeStatement(pstmt);
		}
		return result;
	}

	private DataStore getImportedKeyList(TableIdentifier tbl)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 50);
		sql.append(getQuery(tbl));
		sql.append("AND f.table_name = ? \n");
		sql.append("AND f.owner = ? \n");
		sql.append("ORDER BY pktable_schem, pktable_name, key_seq");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleFKHandler.getImportedKeyList()", "Using: " + sql);
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			pstmt = this.getConnection().getSqlConnection().prepareStatement(sql.toString());
			pstmt.setString(1, tbl.getRawTableName());
			pstmt.setString(2, tbl.getRawSchema());
			rs = pstmt.executeQuery();
			result = processResult(rs);
		}
		finally
		{
			// the result set is closed by processResult
			SqlUtil.closeStatement(pstmt);
		}
		return result;
	}


}
