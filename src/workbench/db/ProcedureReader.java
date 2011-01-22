/*
 * ProcedureReader.java
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

	final String PROC_RESULT_UNKNOWN = "";
	final String PROC_RESULT_YES = "RESULT";
	final String PROC_RESULT_NO = "NO RESULT";

	StringBuilder getProcedureHeader(String catalog, String schema, String procName, int procType);


	boolean procedureExists(ProcedureDefinition def);

	DataStore getProcedures(String catalog, String schema, String name)
		throws SQLException;

	DataStore getProcedureColumns(ProcedureDefinition def)
		throws SQLException;

	void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException;

	List<ProcedureDefinition> getProcedureList(String catalog, String schema, String name)
		throws SQLException;
}
