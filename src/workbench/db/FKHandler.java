/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
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
 * @author support@sql-workbench.net
 */
public class FKHandler
{
	public static final int COLUMN_IDX_FK_DEF_FK_NAME = 0;
	public static final int COLUMN_IDX_FK_DEF_COLUMN_NAME = 1;
	public static final int COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME = 2;
	public static final int COLUMN_IDX_FK_DEF_UPDATE_RULE = 3;
	public static final int COLUMN_IDX_FK_DEF_DELETE_RULE = 4;
	public static final int COLUMN_IDX_FK_DEF_DEFERRABLE = 5;
	public static final int COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE = 6;
	public static final int COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE = 7;
	public static final int COLUMN_IDX_FK_DEF_DEFERRABLE_RULE_VALUE = 8;
	
	private WbConnection dbConnection;

	public FKHandler(WbConnection conn)
	{
		dbConnection = conn;
	}

	public DataStore getExportedKeys(TableIdentifier tbl)
		throws SQLException
	{
		return getRawKeyList(tbl, true);
	}

	public DataStore getImportedKeys(TableIdentifier tbl)
		throws SQLException
	{
		return getRawKeyList(tbl, false);
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
				ds.setValue(row, 0, rs.getString(1));
				ds.setValue(row, 1, rs.getString(2));
				ds.setValue(row, 2, rs.getString(3));
				ds.setValue(row, 3, rs.getString(4));
				ds.setValue(row, 4, rs.getString(5));
				ds.setValue(row, 5, rs.getString(6));
				ds.setValue(row, 6, rs.getString(7));
				ds.setValue(row, 7, rs.getString(8));
				ds.setValue(row, 8, Integer.valueOf(rs.getInt(9)));
				ds.setValue(row, 9, Integer.valueOf(rs.getInt(10)));
				ds.setValue(row, 10, rs.getString(11));
				String fk_name = rs.getString(12);
				ds.setValue(row, 11, fk_name);
				ds.setValue(row, 12, rs.getString(13));
				ds.setValue(row, 13, Integer.valueOf(rs.getInt(14)));
			}
			ds.resetStatus();
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
	}

	public DataStore getForeignKeys(TableIdentifier table, boolean includeNumericRuleValue)
	{
		DataStore ds = this.getKeyList(table, true, includeNumericRuleValue);
		return ds;
	}

	public DataStore getReferencedBy(TableIdentifier table)
	{
		DataStore ds = this.getKeyList(table, false, false);
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
