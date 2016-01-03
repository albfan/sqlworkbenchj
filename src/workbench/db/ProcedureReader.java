/*
 * ProcedureReader.java
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
package workbench.db;

import java.sql.SQLException;
import java.util.List;

import workbench.storage.DataStore;

/**
 * Read the definition (source, parameters etc) of a stored procedures from
 * the database
 * @author Thomas Kellerer
 */
public interface ProcedureReader
{
	// Column index definition for the procedure list
	int COLUMN_IDX_PROC_LIST_NAME = 0;
	int COLUMN_IDX_PROC_LIST_TYPE = 1;
	int COLUMN_IDX_PROC_LIST_CATALOG = 2;
	int COLUMN_IDX_PROC_LIST_SCHEMA = 3;
	int COLUMN_IDX_PROC_LIST_REMARKS = 4;
	int COLUMN_IDX_PROC_LIST_SPECIFIC_NAME = 5;

	// column index definitions for the list of procedure columns

	int COLUMN_IDX_PROC_COLUMNS_COL_NAME = 0;
	int COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE = 1;
	int COLUMN_IDX_PROC_COLUMNS_DATA_TYPE = 2;
	int COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE = 3;
	int COLUMN_IDX_PROC_COLUMNS_REMARKS = 4;
	int COLUMN_IDX_PROC_COLUMNS_COL_NR = 5;

	String PROC_RESULT_UNKNOWN = "";
	String PROC_RESULT_YES = "RESULT";
	String PROC_RESULT_NO = "NO RESULT";

	StringBuilder getProcedureHeader(ProcedureDefinition def);

	DataStore getProcedures(String catalog, String schema, String name)
		throws SQLException;

	DataStore getProcedureColumns(ProcedureDefinition def)
		throws SQLException;

  void readProcedureParameters(ProcedureDefinition def)
    throws SQLException;

	void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException;

	void readProcedureSource(ProcedureDefinition def, String catalogForSource, String schemaForSource)
		throws NoConfigException;

	List<ProcedureDefinition> getProcedureList(String catalog, String schema, String name)
		throws SQLException;

	ProcedureDefinition findProcedureByName(DbObject procName)
		throws SQLException;

	ProcedureDefinition findProcedureDefinition(ProcedureDefinition def);

  boolean isRecreateStatement(CharSequence sql);

  CharSequence getPackageSource(String catalog, String schema, String packageName);

  /**
   * Retrieve the source for a single procedure from a package.
   *
   * @param procedureName  the procedure to get the source for
   *
   * @return the source code, null if no such procedure exists or the DBMS does not support packages
   */
  CharSequence getPackageProcedureSource(ProcedureDefinition def);

  boolean supportsPackages();

  void clearCache();
}
