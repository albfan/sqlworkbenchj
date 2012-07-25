/*
 * StreamImporter.java
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
import java.io.Reader;
import java.sql.SQLException;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.gui.dialogs.dataimport.TextImportOptions;

/**
 *
 * @author Thomas Kellerer
 */
public interface StreamImporter
{

	/**
	 * Import a complete file in a single operation into the specified table.
	 *
	 * This is intended for importers that can send a whole file to the database
	 * rather than using a row-by-row strategy.
	 *
	 * @param table     the table to import
	 * @param columns   the columns to be used
	 * @param data      the source data
	 * @param options   the import options
	 * @param encoding  the encoding of the source file
	 * @return the number of rows imported
	 *
	 * @throws SQLException
	 * @throws IOException
	 */
	void setup(TableIdentifier table, List<ColumnIdentifier> columns, Reader in, TextImportOptions options);

	long processStreamData()
		throws SQLException, IOException;

}
