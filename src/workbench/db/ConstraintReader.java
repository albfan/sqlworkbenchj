/*
 * ConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author support@sql-workbench.net
 */
public interface ConstraintReader
{
	/**
	 *	Returns the column constraints for the given table. The key to the Map is
	 *	the column name, the value is the full expression which can be appended
	 *	to the column definition inside a CREATE TABLE statement.
	 */
	Map getColumnConstraints(Connection dbConnection, TableIdentifier aTable);
	
	
	/**
	 * Returns the SQL Statement that should be appended to a CREATE table
	 * in order to create the constraints defined on the table
	 */
	String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent)
		throws SQLException;
	
}
