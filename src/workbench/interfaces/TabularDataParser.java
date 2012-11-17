/*
 * TabularDataParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.interfaces;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public interface TabularDataParser
	extends ImportFileParser
{
	void setContainsHeader(boolean flag);
	void checkTargetTable()
		throws SQLException;

	void setupFileColumns(List<ColumnIdentifier> importColumns)
		throws SQLException, IOException;

	void setColumns(List<ColumnIdentifier> fileColumns, List<ColumnIdentifier> columnsToImport)
		throws SQLException;

}
