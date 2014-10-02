/*
 * OracleFKHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DefaultFKHandler;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.SortDefinition;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to fix the bug in Oracle's JDBC that causes foreign keys that reference unique constraints
 * are not returned.
 *
 * It also uses USER_XXXX tables rather than the ALL_XXX tables as that is faster in most cases
 *
 * @author Thomas Kellerer
 */
public class OracleFKHandler
	extends DefaultFKHandler
{
	final String baseSql;

	private PreparedStatement retrievalStatement;
	private final String currentUser;

	public OracleFKHandler(WbConnection conn)
	{
		super(conn);
		currentUser = conn.getCurrentUser();
		containsStatusCol = true;

		// This is essentially a copy of the Statement used by the Oracle driver
		// the driver does not take unique constraints into account, and this statement does.
		// Otherwise foreign keys referencing unique constraints (rather than primary keys) would
		// not be displayed (DbExplorer, WbSchemaReport) or correctly processed (TableDependency)
		baseSql =
			"SELECT /* SQLWorkbench */ NULL AS pktable_cat, \n" +
			"       p.owner AS pktable_schem, \n" +
			"       p.table_name AS pktable_name, \n" +
			"       pc.column_name AS pkcolumn_name, \n" +
			"       NULL AS fktable_cat, \n" +
			"       f.owner AS fktable_schem, \n" +
			"       f.table_name AS fktable_name, \n" +
			"       fc.column_name AS fkcolumn_name, \n" +
			"       fc.position AS key_seq, \n" +
			"       3 AS update_rule, \n" +
			"       decode (f.delete_rule, \n" +
			"              'CASCADE', 0, \n" +
			"              'SET NULL', 2, \n" +
			"              1 \n" +
			"       ) AS delete_rule, \n" +
			"       f.constraint_name AS fk_name, \n" +
			"       p.constraint_name AS pk_name, \n" +
			"       decode(f.deferrable, \n" +
			"             'DEFERRABLE', decode(f.deferred, 'IMMEDIATE', " + DatabaseMetaData.importedKeyInitiallyImmediate + ", " + DatabaseMetaData.importedKeyInitiallyDeferred + ") , \n" +
			"             'NOT DEFERRABLE'," + DatabaseMetaData.importedKeyNotDeferrable + " \n" +
			"       ) deferrability, \n" +
			"       case when f.status = 'ENABLED' then 'YES' else 'NO' end as enabled, \n" +
			"       case when f.validated = 'VALIDATED' then 'YES' else 'NO' end as validated \n " +
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
	 * @param tbl the table for which the query should be generated
	 * @return the query to use
	 */
	private String getQuery(TableIdentifier tbl)
	{
		if (OracleUtils.optimizeCatalogQueries())
		{
			String schema = tbl.getRawSchema();
			if (StringUtil.isEmptyString(schema) || schema.equalsIgnoreCase(currentUser))
			{
				return baseSql.replace(" all_c", " user_c");
			}
		}
		return baseSql;
	}

	private DataStore getExportedKeyList(TableIdentifier tbl)
		throws SQLException
	{
		// I'm not adding an order by because the statement is terribly slow anyway
		// and an order by makes it even slower for large results
		StringBuilder sql = new StringBuilder(baseSql.length() + 50);
		sql.append(getQuery(tbl));
		sql.append("AND p.table_name = ? \n");
		sql.append("AND p.owner = ? \n");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleFKHandler.getExportedKeyList()", "Using:\n " + SqlUtil.replaceParameters(sql, tbl.getRawTableName(), tbl.getRawSchema()));
		}

		ResultSet rs;
		DataStore result = null;
		try
		{
			retrievalStatement = this.getConnection().getSqlConnection().prepareStatement(sql.toString());
			retrievalStatement.setString(1, tbl.getRawTableName());
			retrievalStatement.setString(2, tbl.getRawSchema());
			rs = retrievalStatement.executeQuery();
			result = processResult(rs);
		}
		finally
		{
			// the result set is closed by processResult
			SqlUtil.closeStatement(retrievalStatement);
			retrievalStatement = null;
		}
		sortResult(result);
		return result;
	}

	private void sortResult(DataStore ds)
	{
		if (ds == null) return;
		// sort by the second and third column
		SortDefinition def = new SortDefinition(new int[] {1,2}, new boolean[] {true, true});
		ds.sort(def);
	}

	private DataStore getImportedKeyList(TableIdentifier tbl)
		throws SQLException
	{
		// I'm not adding an order by because the statement is terribly slow anyway
		// and an order by makes it even slower for large results
		StringBuilder sql = new StringBuilder(baseSql.length() + 50);
		sql.append(getQuery(tbl));
		sql.append("AND f.table_name = ? \n");
		sql.append("AND f.owner = ? \n");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleFKHandler.getImportedKeyList()", "Using:\n" + SqlUtil.replaceParameters(sql, tbl.getRawTableName(), tbl.getRawSchema()));
		}

		ResultSet rs;
		DataStore result = null;
		try
		{
			retrievalStatement = this.getConnection().getSqlConnection().prepareStatement(sql.toString());
			retrievalStatement.setString(1, tbl.getRawTableName());
			retrievalStatement.setString(2, tbl.getRawSchema());
			rs = retrievalStatement.executeQuery();
			result = processResult(rs);
		}
		finally
		{
			// the result set is closed by processResult
			SqlUtil.closeStatement(retrievalStatement);
			retrievalStatement = null;
		}
		sortResult(result);
		return result;
	}

	@Override
	public void cancel()
	{
		super.cancel();
		if (retrievalStatement != null)
		{
			try
			{
				retrievalStatement.cancel();
			}
			catch (Exception sql)
			{
				// nothing to do
			}
		}
	}

}
