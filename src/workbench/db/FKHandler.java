/*
 * FKHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.sql.SQLException;

import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public interface FKHandler
{
	int COLUMN_IDX_FK_DEF_FK_NAME = 0;
	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the source column name
	 */
	int COLUMN_IDX_FK_DEF_COLUMN_NAME = 1;
	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the column name of the target table (as tablename.columnname)
	 */
	int COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME = 2;
	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the column where the delete rule is stored
	 */
	int COLUMN_IDX_FK_DEF_UPDATE_RULE = 3;
	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the column where the update rule is stored
	 */
	int COLUMN_IDX_FK_DEF_DELETE_RULE = 4;
	/**
	 * The column index in the DataStore returned by getForeignKeys() or getReferencedBy()
	 * indicating the column where the deferrable option is stored
	 */
	int COLUMN_IDX_FK_DEF_DEFERRABLE = 5;

	int COLUMN_IDX_FK_DEF_ENABLED = 6;
	int COLUMN_IDX_FK_DEF_VALIDATED = 7;

	int COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE = 8;
	int COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE = 9;
	int COLUMN_IDX_FK_DEF_DEFERRABLE_RULE_VALUE = 10;

	int COLUMN_IDX_DEFERRABILITY = 13;

	boolean supportsStatus();
	boolean containsStatusColumn();

	FkStatusInfo getFkEnabledFlag(TableIdentifier table, String fkname);

	/**
	 * Returns a DataStore with the exported keys with the raw information copied from the result
	 * of the DatabaseMetaData.getExportedKeys()
	 *
	 * These are "outgoing" foreign keys from the passed table
	 * @param source the table to check
	 * @return the defined foreign keys
	 * @throws SQLException
	 */
	DataStore getExportedKeys(TableIdentifier source)
		throws SQLException;

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
	DataStore getForeignKeys(TableIdentifier table, boolean includeNumericRuleValue);

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
	DataStore getImportedKeys(TableIdentifier target)
		throws SQLException;

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
	DataStore getReferencedBy(TableIdentifier table);

	void cancel();

	public static class FkStatusInfo
	{
		public final boolean validated;
		public final boolean enabled;

		public FkStatusInfo(boolean enabled, boolean validated)
		{
			this.validated = validated;
			this.enabled = enabled;
		}
	}

}
