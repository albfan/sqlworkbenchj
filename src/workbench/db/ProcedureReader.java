/*
 * ProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 */
package workbench.db;

import java.sql.SQLException;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;

/**
 * Read the definition (source, parameters etc) of a stored procedures from 
 * the database
 * @author info@sql-workbench.net
 */
public interface ProcedureReader
{
	// Column index definition for the procedure list
	public static final int COLUMN_IDX_PROC_LIST_NAME = 0;
	public static final int COLUMN_IDX_PROC_LIST_TYPE = 1;
	public static final int COLUMN_IDX_PROC_LIST_CATALOG = 2;
	public static final int COLUMN_IDX_PROC_LIST_SCHEMA = 3;
	public static final int COLUMN_IDX_PROC_LIST_REMARKS = 4;
	
	// column index definitions for the list of procedure columns
	public final static int COLUMN_IDX_PROC_COLUMNS_COL_NAME = 0;
	public final static int COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE = 1;
	public final static int COLUMN_IDX_PROC_COLUMNS_DATA_TYPE = 2;
	public final static int COLUMN_IDX_PROC_COLUMNS_REMARKS = 3;
	
	public static final String PROC_RESULT_UNKNOWN = "";
	public static final String PROC_RESULT_YES = "RESULT";
	public static final String PROC_RESULT_NO = "NO RESULT";
	
	StrBuffer getProcedureHeader(String catalog, String schema, String procName);
	DataStore getProcedures(String aCatalog, String aSchema)
		throws SQLException;
	DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException;
	
}
