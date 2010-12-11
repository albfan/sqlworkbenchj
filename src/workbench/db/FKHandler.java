/*
 * FKHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
public class FKHandler
{
	public static final int COLUMN_IDX_FK_DEF_FK_NAME = 0;

	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the source column name
	 */
	public static final int COLUMN_IDX_FK_DEF_COLUMN_NAME = 1;

	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the column name of the target table (as tablename.columnname)
	 */
	public static final int COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME = 2;

	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the column where the delete rule is stored
	 */
	public static final int COLUMN_IDX_FK_DEF_UPDATE_RULE = 3;

	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the column where the update rule is stored
	 */
	public static final int COLUMN_IDX_FK_DEF_DELETE_RULE = 4;

	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the column where the deferrable option is stored
	 */
	public static final int COLUMN_IDX_FK_DEF_DEFERRABLE = 5;


	public static final int COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE = 6;
	public static final int COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE = 7;
	public static final int COLUMN_IDX_FK_DEF_DEFERRABLE_RULE_VALUE = 8;

	private WbConnection dbConnection;

	public FKHandler(WbConnection conn)
	{
		dbConnection = conn;
	}

	/**
	 * Returns a DataStore with the exported keys with the raw information copied from the result
	 * of the DatabaseMetaData.getExportedKeys()
	 *
	 * @param source the table to check
	 * @return the defined foreign keys
	 * @throws SQLException
	 */
	public DataStore getExportedKeys(TableIdentifier source)
		throws SQLException
	{
		return getRawKeyList(source, true);
	}

	/**
	 * Returns a DataStore with the imported keys with the raw information copied from the result
	 * of the DatabaseMetaData.getImportedKeys()
	 *
	 * These are "incoming" foreign keys to the passed table.
	 *
	 * @param target the table to check
	 * @return foreign keys referencing the target table
	 * @throws SQLException
	 */
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
	 * @param table the table to check
	 * @param includeNumericRuleValue
	 * @return all "outgoing" foreign keys
	 */
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
	 * @param table the table to check
	 * @param includeNumericRuleValue
	 * @return all "incoming" foreign keys
	 */
	public DataStore getReferencedBy(TableIdentifier table)
	{
		DataStore ds = this.getKeyList(table, false, false);
		return ds;
	}

	private DataStore getRawKeyList(TableIdentifier tbl, boolean exported)
		throws SQLException
	{
		TableIdentifier table = tbl.createCopy();
		table.adjustCase(this.dbConnection);

		DatabaseMetaData meta = this.dbConnection.getSqlConnection().getMetaData();

		ResultSet rs;
		if (exported)
			rs = meta.getExportedKeys(table.getCatalog(), table.getSchema(), table.getTableName());
		else
			rs = meta.getImportedKeys(table.getCatalog(), table.getSchema(), table.getTableName());

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
				ds.setValue(row, 10, rs.getString(11)); // DELETE_RULE
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

	private DataStore getKeyList(TableIdentifier tableId, boolean getOwnFk, boolean includeNumericRuleValue)
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
		if (tableId == null) return ds;

		ResultSet rs = null;

		try
		{
			DatabaseMetaData meta = this.dbConnection.getSqlConnection().getMetaData();
			TableIdentifier tbl = tableId.createCopy();
			tbl.adjustCase(this.dbConnection);

			int tableCol;
			int fkNameCol;
			int colCol;
			int fkColCol;
			int deleteActionCol = 11;
			int updateActionCol = 10;
			int schemaCol;

			if (getOwnFk)
			{
				rs = meta.getImportedKeys(tbl.getRawCatalog(), tbl.getRawSchema(), tbl.getRawTableName());
				tableCol = 3;
				schemaCol = 2;
				fkNameCol = 12;
				colCol = 8;
				fkColCol = 4;
			}
			else
			{
				rs = meta.getExportedKeys(tbl.getRawCatalog(), tbl.getRawSchema(), tbl.getRawTableName());
				tableCol = 7;
				schemaCol = 6;
				fkNameCol = 12;
				colCol = 4;
				fkColCol = 8;
			}

			while (rs.next())
			{
				String table = rs.getString(tableCol);
				String fk_col = rs.getString(fkColCol);
				String col = rs.getString(colCol);
				String fk_name = rs.getString(fkNameCol);
				String schema = rs.getString(schemaCol);
				if (!this.dbConnection.getMetadata().ignoreSchema(schema))
				{
					table = schema + "." + table;
				}
				int updateAction = rs.getInt(updateActionCol);
				String updActionDesc = dbSettings.getRuleDisplay(updateAction);
				int deleteAction = rs.getInt(deleteActionCol);
				String delActionDesc = dbSettings.getRuleDisplay(deleteAction);

				int deferrableCode = rs.getInt(14);
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
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
	}

}
