/*
 * DbObject.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;
import java.sql.SQLException;
/**
 *
 * @author support@sql-workbench.net
 */
public interface DbObject 
{
	String getCatalog();
	
	String getSchema();
	
	/**
	 * Returns the name of the object (e.g. TABLE, PROCEDURE, ...)
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
	 * Return the name of the version to be used in SQL Statements.
	 * This will consider quoting of special characters if necessary.
	 * 
	 * @param conn The connection for which the correct name should be returned
	 * @return the name of the object, quoted with respect to the passed connection
	 */
	String getObjectName(WbConnection conn);
	
	/**
	 * Get a fully qualified name of the object. 
	 * 
	 * @param conn The connection for which the qualified name should be createdd
	 * @return the qualified name including catalog and schema if applicable
	 */
	String getObjectExpression(WbConnection conn);
	
	/**
	 * Return the SQL source for this object. This is not necessariyl
	 * a valid SQL Statement that can be run (e.g. for a column definition)
	 * @param con the connection for which to create the source
	 * @return the course to re-create this object
	 * @throws java.sql.SQLException
	 */
	CharSequence getSource(WbConnection con)
		throws SQLException;
	
}
