/*
 * ConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.util.Collections;
import java.util.List;

import workbench.util.StringUtil;

/**
 * An interface to read column and table constraints from the database.
 *
 * @author Thomas Kellerer
 */
public interface ConstraintReader
{
	/**
	 *	Retrieve the column constraints for the given table and stores them in the
	 * list of columns.
	 *
	 * The key to the returned Map is	the column name, the value is the full expression which can be appended
	 * to the column definition inside a CREATE TABLE statement.
	 *
	 * @param dbConnection the connection to use
	 * @param table        the table to check
	 */
	void retrieveColumnConstraints(WbConnection dbConnection, TableDefinition table);


	/**
	 * Returns the table level constraints for the table (usually these are check constraints).
	 *
	 * @param dbConnection  the connection to use
	 * @param table        the table to check
	 *
	 * @return a list of table constraints or an empty list if nothing was found
	 */
	List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableDefinition table);

	/**
	 * Rebuild the source of the given constraints.
	 *
	 * @param constraints  the constraints for which to build the source
	 * @param indent       a line indent to be used
	 */
	String getConstraintSource(List<TableConstraint> constraints, String indent);

	/**
	 * A ConstraintReader which does nothing.
	 */
	ConstraintReader NULL_READER = new ConstraintReader()
	{
		@Override
		public void retrieveColumnConstraints(WbConnection dbConnection,TableDefinition table)
		{
		}

		@Override
		public List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableDefinition table)
		{
			return Collections.emptyList();
		}

		@Override
		public String getConstraintSource(List<TableConstraint> constraints, String indent)
		{
			return StringUtil.EMPTY_STRING;
		}
	};
}
