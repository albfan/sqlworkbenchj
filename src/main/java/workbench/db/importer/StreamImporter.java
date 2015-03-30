/*
 * StreamImporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.importer;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

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
	 * @param options   the import options
	 *
	 * @throws SQLException
	 * @throws IOException
	 */
	void setup(TableIdentifier table, List<ColumnIdentifier> columns, Reader in, TextImportOptions options, String encoding);

	long processStreamData()
		throws SQLException, IOException;

}
