/*
 * ImportFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.interfaces;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.ImportFileHandler;
import workbench.db.importer.ImportFileLister;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.modifier.ImportValueModifier;

import workbench.storage.RowActionMonitor;

/**
 * @author Thomas Kellerer
 */
public interface ImportFileParser
	extends RowDataProducer
{
	/**
	 *  Return the encoding used to read input files
	 */
	String getEncoding();

	/**
	 * Set the file to be processed.
	 * @param file
	 */
	void setInputFile(File file);

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
	void setTargetSchema(String schema);

	/**
	 * Define the column structure to be used for the import
	 */
	void setColumns(List<ColumnIdentifier> columns)
		throws SQLException;

	/**
	 *	Returns the column list as a comma separated string
	 *  that can be used for the WbImport command.
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

	void setSourceFiles(ImportFileLister source);
  void setTrimValues(boolean trimValues);

	List<File> getProcessedFiles();

	void addColumnFilter(String colname, String regex);

	void setConnection(WbConnection connection);

	void setRowMonitor(RowActionMonitor monitor);

	void setIgnoreMissingColumns(boolean flag);

  void setCheckTargetWithQuery(boolean flag);
}
