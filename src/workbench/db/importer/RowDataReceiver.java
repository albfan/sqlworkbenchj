/*
 * RowDataReceiver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.sql.SQLException;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

/**
 *
 * @author support@sql-workbench.net
 */
public interface RowDataReceiver
{
	void processRow(Object[] row) throws SQLException;
	void setTableCount(int total);
	void setCurrentTable(int current);
	void setTargetTable(TableIdentifier table, ColumnIdentifier[] columns)	throws SQLException;
	void importFinished();
	void importCancelled();
	void tableImportError();
	
	/**
	 * Log an error with the receiver that might have occurred
	 * during parsing of the source data. 
	 */
	void recordRejected(String record);
}
