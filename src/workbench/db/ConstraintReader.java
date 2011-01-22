/*
 * ConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * An interface to read column and table constraints from the database.
 * 
 * @author Thomas Kellerer
 */
public interface ConstraintReader
{
	/**
	 *	Returns the column constraints for the given table. The key to the Map is
	 *	the column name, the value is the full expression which can be appended
	 *	to the column definition inside a CREATE TABLE statement.
	 */
	Map<String, String> getColumnConstraints(Connection dbConnection, TableIdentifier aTable);
	
	
	/**
	 * Returns the (check) constraint definitions for the given table
	 */
	List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableIdentifier aTable);

	/**
	 * Rebuild the source of the given constraints
	 * 
	 * @param constraints
	 * @param indent
	 */
	String getConstraintSource(List<TableConstraint> constraints, String indent);
}
