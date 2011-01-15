/*
 * RowDataReceiver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.sql.SQLException;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public interface RowDataReceiver
{
	/**
	 * Returns true if the receiver will create the target table "on the fly"
	 */
	boolean getCreateTarget();
	boolean shouldProcessNextRow(); 
	void nextRowSkipped();
	
	/**
	 * Set the list of tables that will be processed by the 
	 * row data producer
	 * 
	 * @param targetTables
	 */
	void setTableList(List<TableIdentifier> targetTables);
	
	void deleteTargetTables()
		throws SQLException;
	
	void beginMultiTable()
		throws SQLException;
		
	void endMultiTable();
	
	void processRow(Object[] row) throws SQLException;
	void setTableCount(int total);
	void setCurrentTable(int current);
	void setTargetTable(TableIdentifier table, List<ColumnIdentifier> columns)	throws SQLException;
	void importFinished();
	void importCancelled();
	void tableImportError();
	void tableImportFinished()
		throws SQLException;
	
	/**
	 * Log an error with the receiver that might have occurred
	 * during parsing of the source data. 
	 */
	void recordRejected(String record, long importRow, Throwable e);
}
