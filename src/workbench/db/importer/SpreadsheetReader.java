/*
 * SpreadsheetReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.importer;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
public interface SpreadsheetReader
{
	List<String> getHeaderColumns();
	void setActiveWorksheet(int index);
	List<String> getRowValues(int row);
	void setNullString(String nullString);

	/**
	 * Return the total row count in the spreadsheet including a possible header row.
	 *
	 * @return the row count
	 */
	int getRowCount();
	void done();
	void load()
		throws IOException;

	List<String> getSheets();
}
