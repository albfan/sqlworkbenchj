/*
 * TableChangeValidator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.dbobjects;

import java.sql.DatabaseMetaData;
import workbench.db.DbObjectChanger;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.gui.components.DataStoreTableModel;
import workbench.storage.InputValidator;

/**
 *
 * @author Thomas Kellerer
 */
public class ProcedureChangeValidator
	implements InputValidator
{
	private DbObjectChanger changer;

	public ProcedureChangeValidator()
	{
	}

	@Override
	public boolean isValid(Object newValue, int row, int col, DataStoreTableModel source)
	{
		if (changer == null) return false;

		String type = null;
		Object userObject = source.getDataStore().getRow(row).getUserObject();
		if (userObject instanceof ProcedureDefinition)
		{
			type = ((ProcedureDefinition)userObject).getObjectType();
		}
		else
		{
			// Fallback, should not happen
			int resultType = source.getDataStore().getValueAsInt(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureResultUnknown);
			if (resultType == DatabaseMetaData.procedureReturnsResult)
			{
				type = "FUNCTION";
			}
			else if (resultType == DatabaseMetaData.procedureNoResult)
			{
				type = "PROCEDURE";
			}
		}

		if (col == ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS)
		{
			return changer.getCommentSql(type) != null;
		}
		else if (col == ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA)
		{
			return changer.getChangeSchemaSql(type) != null;
		}
		else if (col == ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG)
		{
			return changer.getChangeCatalogSql(type) != null;
		}
		return false;
	}

	public void setConnection(WbConnection con)
	{
		if (con != null)
		{
			changer = new DbObjectChanger(con);
		}
		else
		{
			changer = null;
		}
	}
}
