/*
 * TableDefinitionReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db;

import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
public interface TableDefinitionReader
{
	/**
	 * Return the definition of the given table.
	 * <br/>
	 * To display the columns for a table in a DataStore create an
	 * instance of {@link TableColumnsDatastore}.
	 *
	 * @param toRead The table for which the definition should be retrieved (it should have a PK assigned)
	 * @param typeResolver the data type resolver that should be used to "clean up" data types returned from the driver
	 *
	 * @throws SQLException
	 * @return the definition of the table. If toRead was null, null is returned
	 *
	 * @see TableColumnsDatastore
	 * @see TableIdentifier#getPrimaryKey()
	 */
	List<ColumnIdentifier> getTableColumns(TableIdentifier toRead, DataTypeResolver typeResolver)
		throws SQLException;

	TableDefinition getTableDefinition(TableIdentifier toRead, boolean includePkInformation)
		throws SQLException;

}
