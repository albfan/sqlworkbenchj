/*
 * RowDataReceiver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.sql.SQLException;

import workbench.db.ColumnIdentifier;

/**
 *
 * @author  thomas
 */
public interface RowDataReceiver
{
	void processRow(Object[] row) throws SQLException;
	void setTargetTable(String tableName, ColumnIdentifier[] columns)	throws SQLException;
	void importFinished();
	void importCancelled();
}
