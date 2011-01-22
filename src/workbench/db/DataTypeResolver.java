/*
 * DataTypeResolver.java
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

/**
 * An interface to return the SQL code for a given JDBC data type.
 * 
 * @author Thomas Kellerer
 */
public interface DataTypeResolver 
{
	/**
	 * Return a SQL for the indicated data type
	 * @param dbmsName the name of the type 
	 * @param sqlType the numeric value from java.sql.Types
	 * @param size the size of the column
	 * @param digits the digits, &lt; 0 if not applicable
	 * @return the SQL "display" for the given datatype
	 */
	String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits);

	/**
	 * Fixes data types returned by the JDBC driver to the correct one
	 * @param type
	 * @return the JDBC data type to be used instead
	 */
	int fixColumnType(int type, String dbmsType);

	/**
	 * Return the Java class to be used for the passed datatype.
	 * If null is returned, the information from the driver is used (ResultSetMetaData.getColumnClassName())
	 *
	 * @param type the JDBC data type
	 * @param dbmsType the DBMS data type name
	 * @return null if the driver default should be used, a fully qualified classname otherwise
	 */
	String getColumnClassName(int type, String dbmsType);
}
