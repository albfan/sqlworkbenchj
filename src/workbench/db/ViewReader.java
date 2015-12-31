/*
 * ViewReader.java
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

/**
 *
 * @author Thomas Kellerer
 */
public interface ViewReader
{

	CharSequence getExtendedViewSource(TableIdentifier tbl)
		throws SQLException;

	CharSequence getExtendedViewSource(TableIdentifier tbl, DropType dropType)
		throws SQLException;

	/**
	 * Returns a complete SQL statement to (re)create the given view.
	 *
	 * This method will extend the stored source to a valid CREATE VIEW.
	 *
	 * If no SQL Statement was configured to retrieve the source, an explanation
	 * on how to configure will be returned.
	 *
	 * @param view The view for which thee source should be created
	 * @param includeCommit if true, terminate the whole statement with a COMMIT
	 * @param includeDrop if true, add a DROP statement before the CREATE statement
	 *
	 * @see #getViewSource(workbench.db.TableIdentifier)
	 */
	CharSequence getExtendedViewSource(TableDefinition view, DropType dropType, boolean includeCommit)
		throws SQLException;

	/**
	 * Returns a complete SQL statement to create the given view.
	 *
	 * This method will extend the stored source to a valid CREATE VIEW.
	 *
	 * If no SQL Statement was configured to retrieve the source, an explanation
	 * on how to configure will be returned.
   *
   * Unlike getExtendedViewSource() this method will not add any addtional SQL statements like
   * the grants, a commit or a drop statement.
	 *
	 * @param view The view for which thee source should be created
   *
	 * @see #getExtendedViewSource(workbench.db.TableDefinition, boolean, boolean)
	 */
	CharSequence getFullViewSource(TableDefinition view)
		throws SQLException, NoConfigException;

	/**
	 * Return the source of a view definition as it is stored in the database.
	 * <br/>
	 * Usually (depending on how the meta data is stored in the database) the DBMS
	 * only stores the underlying SELECT statement (but not a full CREATE VIEW),
	 * and that will be returned by this method.
	 * <br/>
	 * To create a complete SQL to re-create a view, use {@link #getExtendedViewSource(workbench.db.TableIdentifier) }
	 *
	 * @return the view source as stored in the database.
	 */
	CharSequence getViewSource(TableIdentifier viewId)
		throws NoConfigException;

}
