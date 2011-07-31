/*
 * FKHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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

/**
 * Retrieve FK information from the database.
 *
 * @author Thomas Kellerer
 */
public class DefaultFKHandler
	implements FKHandler
{
	private WbConnection dbConnection;

	protected DefaultFKHandler(WbConnection conn)
	{
		dbConnection = conn;
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
	 * @param includeNumericRuleValue
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
		TableIdentifier table = tbl.createCopy();
		table.adjustCase(this.dbConnection);

		DatabaseMetaData meta = this.dbConnection.getSqlConnection().getMetaData();

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
		DataStore ds = new DataStore(rs, false);
		try
		{
			while (rs.next())
			{
				int row = ds.addRow();
				ds.setValue(row, 0, rs.getString(1)); // PKTABLE_CAT
				ds.setValue(row, 1, rs.getString(2)); // PKTABLE_SCHEM
				ds.setValue(row, 2, rs.getString(3)); // PKTABLE_NAME
				ds.setValue(row, 3, rs.getString(4)); // PKCOLUMN_NAME
				ds.setValue(row, 4, rs.getString(5)); // FKTABLE_CAT
				ds.setValue(row, 5, rs.getString(6)); // FKTABLE_SCHEM
				ds.setValue(row, 6, rs.getString(7)); // FKTABLE_NAME
				ds.setValue(row, 7, rs.getString(8)); // FKCOLUMN_NAME
				ds.setValue(row, 8, Integer.valueOf(rs.getInt(9))); // KEY_SEQ
				ds.setValue(row, 9, Integer.valueOf(rs.getInt(10))); // UPDATE_RULE
				ds.setValue(row, 10, Integer.valueOf(rs.getInt(11))); // DELETE_RULE
				ds.setValue(row, 11, rs.getString(12)); // FK_NAME
				ds.setValue(row, 12, rs.getString(13)); // PK_NAME
				ds.setValue(row, 13, Integer.valueOf(rs.getInt(14))); // DEFERRABILITY
			}
			ds.resetStatus();
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
	}

	private DataStore getKeyList(TableIdentifier tbl, boolean getOwnFk, boolean includeNumericRuleValue)
	{
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
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE", "DEFERRABLE", "UPDATE_RULE_VALUE", "DELETE_RULE_VALUE", "DEFER_RULE_VALUE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER};
			sizes = new int[] {25, 10, 30, 12, 12, 15, 1, 1, 1};
		}
		else
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE", "DEFERRABLE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[] {25, 10, 30, 12, 12, 15};
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

			if (getOwnFk)
			{
				//rs = meta.getImportedKeys(tbl.getRawCatalog(), tbl.getRawSchema(), tbl.getRawTableName());
				rawList = getRawKeyList(tbl, false);
				tableCol = 2;
				schemaCol = 1;
				fkNameCol = 11;
				colCol = 7;
				fkColCol = 3;
			}
			else
			{
				//rs = meta.getExportedKeys(tbl.getRawCatalog(), tbl.getRawSchema(), tbl.getRawTableName());
				rawList = getRawKeyList(tbl, true);
				tableCol = 6;
				schemaCol = 5;
				fkNameCol = 11;
				colCol = 3;
				fkColCol = 7;
			}

			String currentSchema = dbConnection.getMetadata().getCurrentSchema();
			for (int rawRow = 0; rawRow < rawList.getRowCount(); rawRow ++)
			{
				String table = rawList.getValueAsString(rawRow, tableCol);
				String fk_col = rawList.getValueAsString(rawRow, fkColCol);
				String col = rawList.getValueAsString(rawRow, colCol);
				String fk_name = rawList.getValueAsString(rawRow, fkNameCol);
				String schema = rawList.getValueAsString(rawRow, schemaCol);
				if (!this.dbConnection.getMetadata().ignoreSchema(schema, currentSchema))
				{
					table = schema + "." + table;
				}
				int updateAction = rawList.getValueAsInt(rawRow, updateActionCol, DatabaseMetaData.importedKeyNoAction);
				String updActionDesc = dbSettings.getRuleDisplay(updateAction);
				int deleteAction = rawList.getValueAsInt(rawRow, deleteActionCol, DatabaseMetaData.importedKeyNoAction);
				String delActionDesc = dbSettings.getRuleDisplay(deleteAction);

				int deferrableCode = rawList.getValueAsInt(rawRow, 13, DatabaseMetaData.importedKeyNoAction);
				String deferrable = dbSettings.getRuleDisplay(deferrableCode);

				int row = ds.addRow();
				ds.setValue(row, COLUMN_IDX_FK_DEF_FK_NAME, fk_name);
				ds.setValue(row, COLUMN_IDX_FK_DEF_COLUMN_NAME, col);
				ds.setValue(row, COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME, table + "." + fk_col);
				ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE, updActionDesc);
				ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE, delActionDesc);
				ds.setValue(row, COLUMN_IDX_FK_DEF_DEFERRABLE, deferrable);
				if (includeNumericRuleValue)
				{
					ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE, Integer.valueOf(deleteAction));
					ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE, Integer.valueOf(updateAction));
					ds.setValue(row, COLUMN_IDX_FK_DEF_DEFERRABLE_RULE_VALUE, Integer.valueOf(deferrableCode));
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

}
