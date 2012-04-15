/*
 * ColumnChangeValidator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import workbench.db.sqltemplates.ColumnChanger;
import workbench.db.TableColumnsDatastore;
import workbench.db.WbConnection;
import workbench.gui.components.DataStoreTableModel;
import workbench.storage.InputValidator;
import workbench.storage.RowData;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnChangeValidator
	implements InputValidator
{
	private ColumnChanger changer;

	public ColumnChangeValidator()
	{
	}

	@Override
	public boolean isValid(Object newValue, int row, int col, DataStoreTableModel source)
	{
		if (changer == null) return false;

		if (source.getDataStore().getRowStatus(row) == RowData.NEW)
		{
			return changer.canAddColumn();
		}

		switch (col)
		{
			case TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE:
				return changer.canAlterType();
			case TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME:
				return changer.canRenameColumn();
			case TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_REMARKS:
				return changer.canChangeComment();
			case TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DEFAULT:
				return changer.canChangeDefault();
			case TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_NULLABLE:
				return changer.canChangeNullable();
			default:
				return false;
		}
	}

	public void setConnection(WbConnection con)
	{
		if (con == null)
		{
			changer = null;
		}
		else
		{
			changer = new ColumnChanger(con);
		}
	}
}
