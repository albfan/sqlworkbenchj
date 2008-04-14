/*
 * ImportFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import java.sql.SQLException;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.importer.ImportFileHandler;
import workbench.db.importer.modifier.ImportValueModifier;

/**
 * @author support@sql-workbench.net
 */
public interface ImportFileParser
{
	/**
	 *  Return the encoding used to read input files
	 */
	String getEncoding();
	
	/**
	 *	Return the name of the input file
	 */
	String getSourceFilename();
	
	/**
	 *	Parse the file and return a list of column 
	 *  names defined in that file
	 */
	List<ColumnIdentifier> getColumnsFromFile();
	void setTableName(String table);
	
	/**
	 * Define the column structure to be used for the import
	 */
	void setColumns(List<ColumnIdentifier> columns)
		throws SQLException;
	
	/**
	 *	Returns the column list as a comma separated string
	 *  that can be used for the WbImport command
	 */
	String getColumns();
	
	ImportFileHandler getFileHandler();
	
	/**
	 * Define a modifier to change the values received
	 * from the text file before they are converted to 
	 * the correct datatype.
	 * 
	 * @param modifier the ImportValueModifier to apply to the values in the import file
	 */
	void setValueModifier(ImportValueModifier modifier);

	void setMultiFileImport(boolean flag);
	boolean isMultiFileImport();
	
}
