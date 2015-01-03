/*
 * DefaultFKHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import workbench.log.LogMgr;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

import static java.sql.DatabaseMetaData.*;

/**
 * Retrieve FK information from the database.
 *
 * @author Thomas Kellerer
 */
public class DefaultFKHandler
	implements FKHandler
{
	private final WbConnection dbConnection;
	private boolean cancel;
	protected boolean containsStatusCol;
	protected boolean supportsStatus;

	protected DefaultFKHandler(WbConnection conn)
	{
		dbConnection = conn;
	}

	@Override
	public boolean containsStatusColumn()
	{
		return containsStatusCol;
	}

	@Override
	public boolean supportsStatus()
	{
		return containsStatusCol;
	}

	/**
	 * Returns a DataStore with the exported keys with the raw information copied from the result
	 * of the DatabaseMetaData.getExportedKeys().
	 *
	 * These are "outgoing" foreign keys from the passed table (foreign keys in the passed table referencing other tables).
	 *
	 * @param source the table to check
	 * @return the defined foreign keys
	 * @throws SQLException
	 */
	@Override
	public DataStore getExportedKeys(TableIdentifier source)
		throws SQLException
	{
		return getRawKeyList(source, true);
	}

	/**
	 * Returns a DataStore with the imported keys with the raw information copied from the result
	 * of the DatabaseMetaData.getImportedKeys().
	 *
	 * These are "incoming" foreign keys to the passed table (foreign key referencing the passed table).
	 *
	 * @param target the table to check
	 * @return foreign keys referencing the target table
	 * @throws SQLException
	 */
	@Override
	public DataStore getImportedKeys(TableIdentifier target)
		throws SQLException
	{
		return getRawKeyList(target, false);
	}

	/**
	 * Returns a list of foreign keys defined for the passed table.
	 *
	 * This will include all foreign key constraints on columns of the passed table that reference other tables.
	 *
	 * The column indexes of this datastore are defined by the COLUMN_IDX_xxx constants in this class
	 *
	 * @param table the table to check
	 * @param includeNumericRuleValue
	 * @return all "outgoing" foreign keys
	 */
	@Override
	public DataStore getForeignKeys(TableIdentifier table, boolean includeNumericRuleValue)
	{
		DataStore ds = this.getKeyList(table, true, includeNumericRuleValue);
		return ds;
	}

	/**
	 * Returns a list of foreign keys referencing the passed table.
	 *
	 * This will include all foreign key constraints from other tables that reference the passed table.
	 *
	 * The column indexes of this datastore are defined by the COLUMN_IDX_xxx constants in this class
	 *
	 * @param table the table to check
	 * @return all "incoming" foreign keys
	 */
	@Override
	public DataStore getReferencedBy(TableIdentifier table)
	{
		DataStore ds = this.getKeyList(table, false, false);
		return ds;
	}

	protected WbConnection getConnection()
	{
		return this.dbConnection;
	}

	protected DataStore getRawKeyList(TableIdentifier tbl, boolean exported)
		throws SQLException
	{
		if (this.dbConnection == null) return null;

		TableIdentifier table = tbl.createCopy();
		table.adjustCase(this.dbConnection);

		DatabaseMetaData meta = this.dbConnection.getSqlConnection().getMetaData();

		cancel = false;
		ResultSet rs;
		if (exported)
		{
			rs = meta.getExportedKeys(table.getRawCatalog(), table.getRawSchema(), table.getRawTableName());
		}
		else
		{
			rs = meta.getImportedKeys(table.getRawCatalog(), table.getRawSchema(), table.getRawTableName());
		}
		return processResult(rs);

	}

	protected DataStore processResult(ResultSet rs)
		throws SQLException
	{
		if (rs == null) return null;
		DataStore ds = new DataStore(rs, false);
		boolean useColumnNames = dbConnection.getDbSettings().useColumnNameForMetadata();

		int enabledCol = -1;
		int validCol = -1;

		if (containsStatusCol)
		{
			enabledCol = JdbcUtils.getColumnIndex(rs, "ENABLED");
			validCol = JdbcUtils.getColumnIndex(rs, "VALIDATED");
		}

		try
		{
			while (rs.next())
			{
				if (cancel)
				{
					LogMgr.logWarning("DefaultFKHandler.processResult()", "Processing of rows has been cancelled");
					break;
				}
				int row = ds.addRow();
				ds.setValue(row, 0, useColumnNames ? rs.getString("PKTABLE_CAT") : rs.getString(1)); // PKTABLE_CAT
				ds.setValue(row, 1, useColumnNames ? rs.getString("PKTABLE_SCHEM") : rs.getString(2)); // PKTABLE_SCHEM
				ds.setValue(row, 2, useColumnNames ? rs.getString("PKTABLE_NAME") : rs.getString(3)); // PKTABLE_NAME
				ds.setValue(row, 3, useColumnNames ? rs.getString("PKCOLUMN_NAME") : rs.getString(4)); // PKCOLUMN_NAME
				ds.setValue(row, 4, useColumnNames ? rs.getString("FKTABLE_CAT") : rs.getString(5)); // FKTABLE_CAT
				ds.setValue(row, 5, useColumnNames ? rs.getString("FKTABLE_SCHEM") : rs.getString(6)); // FKTABLE_SCHEM
				ds.setValue(row, 6, useColumnNames ? rs.getString("FKTABLE_NAME") : rs.getString(7)); // FKTABLE_NAME
				ds.setValue(row, 7, useColumnNames ? rs.getString("FKCOLUMN_NAME") : rs.getString(8)); // FKCOLUMN_NAME
				ds.setValue(row, 8, Integer.valueOf(useColumnNames ? rs.getInt("KEY_SEQ") : rs.getInt(9))); // KEY_SEQ

				int updRule = fixRule(useColumnNames ? rs.getInt("UPDATE_RULE") : rs.getInt(10));
				ds.setValue(row, 9, Integer.valueOf(updRule)); // UPDATE_RULE

				int delRule = fixRule(useColumnNames ? rs.getInt("DELETE_RULE") : rs.getInt(11));
				ds.setValue(row, 10, Integer.valueOf(delRule)); // DELETE_RULE
				ds.setValue(row, 11, useColumnNames ? rs.getString("FK_NAME") : rs.getString(12)); // FK_NAME
				ds.setValue(row, 12, useColumnNames ? rs.getString("PK_NAME") : rs.getString(13)); // PK_NAME

				int defer = fixDeferrableRule(useColumnNames ? rs.getInt("DEFERRABILITY") : rs.getInt(14));
				ds.setValue(row, 13, Integer.valueOf(defer)); // DEFERRABILITY
				if (enabledCol > 0)
				{
					ds.setValue(row, enabledCol - 1, rs.getString(enabledCol));
					ds.setValue(row, validCol - 1, rs.getString(validCol));
				}
			}
			ds.resetStatus();
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
	}

	private int fixDeferrableRule(int defer)
	{
		// Some drivers return wrong values here, so we need to sanitize this
		if (defer != importedKeyInitiallyDeferred && defer != importedKeyInitiallyImmediate)
		{
			return importedKeyNotDeferrable;
		}
		return defer;
	}

	private int fixRule(int rule)
	{
		if (rule != importedKeyCascade && rule != importedKeyRestrict && rule != importedKeySetDefault && rule != importedKeySetNull)
		{
			return importedKeyNoAction;
		}
		return rule;
	}

	protected DataStore getKeyList(TableIdentifier tbl, boolean getOwnFk, boolean includeNumericRuleValue)
	{
		if (cancel) return null;

		String cols[] = null;
		String refColName = null;
		DbSettings dbSettings = dbConnection.getDbSettings();

		if (getOwnFk)
		{
			refColName = "REFERENCES";
		}
		else
		{
			refColName = "REFERENCED BY";
		}
		int types[];
		int sizes[];

		if (includeNumericRuleValue)
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE", "DEFERRABLE", "ENABLED", "VALIDATED", "UPDATE_RULE_VALUE", "DELETE_RULE_VALUE", "DEFER_RULE_VALUE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER};
			sizes = new int[] {25, 10, 30, 12, 12, 15, 5, 5, 1, 1, 1};
		}
		else
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE", "DEFERRABLE", "ENABLED", "VALIDATED"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[] {25, 10, 30, 12, 12, 15, 5, 5};
		}
		DataStore ds = new DataStore(cols, types, sizes);
		if (tbl == null) return ds;

		DataStore rawList = null;

		try
		{
			int tableCol;
			int fkNameCol;
			int colCol;
			int fkColCol;
			int deleteActionCol = 10;
			int updateActionCol = 9;
			int schemaCol;
			int catalogCol;

			if (getOwnFk)
			{
				rawList = getRawKeyList(tbl, false);
				tableCol = 2;
				schemaCol = 1;
				catalogCol = 0;
				fkNameCol = 11;
				colCol = 7;
				fkColCol = 3;
			}
			else
			{
				rawList = getRawKeyList(tbl, true);
				tableCol = 6;
				schemaCol = 5;
				catalogCol = 4;
				fkNameCol = 11;
				colCol = 3;
				fkColCol = 7;
			}

			if (rawList == null)
			{
				// this means retrieval was cancelled
				this.cancel = true;
				return null;
			}

			for (int rawRow = 0; rawRow < rawList.getRowCount(); rawRow ++)
			{
				String tname = rawList.getValueAsString(rawRow, tableCol);
				String schema = rawList.getValueAsString(rawRow, schemaCol);
				String catalog = rawList.getValueAsString(rawRow, catalogCol);
				TableIdentifier tid = new TableIdentifier(catalog, schema, tname, false);
				tid.setNeverAdjustCase(true);
				String tableName = tid.getTableExpression(dbConnection);

				String fk_col = rawList.getValueAsString(rawRow, fkColCol);
				String col = rawList.getValueAsString(rawRow, colCol);
				String fk_name = rawList.getValueAsString(rawRow, fkNameCol);

				int updateAction = rawList.getValueAsInt(rawRow, updateActionCol, DatabaseMetaData.importedKeyNoAction);
				String updActionDesc = dbSettings.getRuleDisplay(updateAction);
				int deleteAction = rawList.getValueAsInt(rawRow, deleteActionCol, DatabaseMetaData.importedKeyNoAction);
				String delActionDesc = dbSettings.getRuleDisplay(deleteAction);

				int deferrableCode = rawList.getValueAsInt(rawRow, 13, DatabaseMetaData.importedKeyNoAction);
				String deferrable = dbSettings.getRuleDisplay(deferrableCode);

				int row = ds.addRow();
				ds.setValue(row, COLUMN_IDX_FK_DEF_FK_NAME, fk_name.trim());
				ds.setValue(row, COLUMN_IDX_FK_DEF_COLUMN_NAME, col.trim());
				ds.setValue(row, COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME, tableName + "." + fk_col);
				ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE, updActionDesc);
				ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE, delActionDesc);
				ds.setValue(row, COLUMN_IDX_FK_DEF_DEFERRABLE, deferrable);
				if (includeNumericRuleValue)
				{
					ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE, Integer.valueOf(deleteAction));
					ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE, Integer.valueOf(updateAction));
					ds.setValue(row, COLUMN_IDX_FK_DEF_DEFERRABLE_RULE_VALUE, Integer.valueOf(deferrableCode));
				}

				if (containsStatusCol)
				{
					ds.setValue(row, COLUMN_IDX_FK_DEF_ENABLED, rawList.getValue(rawRow, "ENABLED"));
					ds.setValue(row, COLUMN_IDX_FK_DEF_VALIDATED, rawList.getValue(rawRow, "VALIDATED"));
				}

				if (cancel)
				{
					LogMgr.logWarning("DefaultFKHandler.getKeyList()", "Processing of rows has been cancelled");
					break;
				}
			}
			ds.resetStatus();
		}
		catch (Exception e)
		{
			LogMgr.logError("FKHandler.getKeyList()", "Error when retrieving foreign keys", e);
			ds.reset();
		}
		return ds;
	}

	@Override
	public FkStatusInfo getFkEnabledFlag(TableIdentifier table, String fkname)
	{
		return null;
	}

	@Override
	public void cancel()
	{
		this.cancel = true;
	}

}
