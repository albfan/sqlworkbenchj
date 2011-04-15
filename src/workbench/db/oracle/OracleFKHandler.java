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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
		return super.getRawKeyList(tbl, exported);
	}

	private DataStore getExportedKeyList(TableIdentifier tbl)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 50);
		sql.append(baseSql);
		sql.append(" AND p.table_name = '");
		sql.append(tbl.getRawTableName());
		sql.append('\'');
		sql.append(" AND p.owner = '");
		sql.append(tbl.getRawSchema());
		sql.append('\'');
		sql.append(" ORDER BY fktable_schem, fktable_name, key_seq");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleFKHandler.getExportedKeyList()", "Using: " + sql);
		}

		Statement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.getConnection().createStatement();
			rs = stmt.executeQuery(sql.toString());
			result = processResult(rs);
		}
		finally
		{
			// the result set is closed by processResult
			SqlUtil.closeStatement(stmt);
		}
		return result;
	}

	private DataStore getImportedKeyList(TableIdentifier tbl)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 50);
		sql.append(baseSql);
		sql.append(" AND f.table_name = '");
		sql.append(tbl.getRawTableName());
		sql.append('\'');
		sql.append(" AND f.owner = '");
		sql.append(tbl.getRawSchema());
		sql.append('\'');
		sql.append("\n ORDER BY pktable_schem, pktable_name, key_seq");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleFKHandler.getImportedKeyList()", "Using: " + sql);
		}

		Statement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.getConnection().createStatement();
			rs = stmt.executeQuery(sql.toString());
			result = processResult(rs);
		}
		finally
		{
			// the result set is closed by processResult
			SqlUtil.closeStatement(stmt);
		}
		return result;
	}


}
