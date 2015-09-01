/*
 * DbObject.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
public interface DbObject
{
	String getCatalog();

	String getSchema();

	/**
	 * Returns the type of the object.
	 *
	 * e.g. TABLE, PROCEDURE, VIEW, ...
	 *
	 * @return the object's type
	 */
	String getObjectType();

	/**
	 * Return the name of the object as it should be displayed to the end-user
	 * (without quoting or catalog/schema prefix).
	 *
	 * @return the object's name
	 */
	String getObjectName();

	/**
	 * Return the name of the object to be used in SQL Statements.
   *
	 * This will consider quoting of special characters if necessary.
	 *
	 * @param conn The connection for which the correct name should be returned
	 * @return the name of the object, quoted with respect to the passed connection
	 */
	String getObjectName(WbConnection conn);

	/**
	 * Get a fully qualified name of the object.
	 * The name might not be fully qualified if it is not necessary for the current
	 * schema or catalog.
	 * To get an always fully qualified name use, getFullyQualifiedName()
	 *
	 * @param conn The connection for which the qualified name should be created, may be null
	 * @return the qualified name including catalog and schema if applicable
	 * @see #getFullyQualifiedName(workbench.db.WbConnection)
	 */
	String getObjectExpression(WbConnection conn);

	/**
	 * Return the fully qualified name of this object including
	 * catalog and schema if available.
	 *
	 * This will always return a fully qualified name, even if the current schema/user wouldn't require it.
	 *
	 * @param conn the connection for which the qualified name should be created, may be null
	 * @see #getObjectExpression(workbench.db.WbConnection)
	 */
	String getFullyQualifiedName(WbConnection conn);

	/**
	 * Return the SQL source for this object. This is not necessariyl a valid SQL Statement that can be run (e.g. for a
	 * column definition)
	 *
	 * @param con the connection for which to create the source
	 * @return the course to re-create this object
	 * @throws java.sql.SQLException
	 */
	CharSequence getSource(WbConnection con)
		throws SQLException;

	/**
	 * Returns the name of the object that should be used when generating a DROP
	 * statement. This might be different to the object name e.g. for functions
	 * where the parameters to the function must be listed.
	 *
	 * @param con
	 * @return the name of the object to be used in a DROP statement
	 */
	String getObjectNameForDrop(WbConnection con);

	/**
	 * Return the comment on the DbObject if defined .
	 *
	 * @return the object's comment if any (might be null)
	 */
	String getComment();

	/**
	 * Set the comment on the DbObject.
	 * @param cmt the comment
	 */
	void setComment(String cmt);

	String getDropStatement(WbConnection con, boolean cascade);

  boolean supportsGetSource();
}
