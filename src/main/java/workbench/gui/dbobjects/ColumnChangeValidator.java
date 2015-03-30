/*
 * ColumnChangeValidator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.dbobjects;

import workbench.db.TableColumnsDatastore;
import workbench.db.WbConnection;
import workbench.db.sqltemplates.ColumnChanger;

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
