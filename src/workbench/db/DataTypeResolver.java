/*
 * DataTypeResolver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

/**
 * An interface to return the SQL code for a given JDBC data type.
 * 
 * @author support@sql-workbench.net
 */
public interface DataTypeResolver 
{
	/**
	 * Return a SQL for the indicated data type
	 * @param dbmsName the name of the type 
	 * @param sqlType the numeric value from java.sql.Types
	 * @param size the size of the column
	 * @param digits the digits, &lt; 0 if not applicable
	 * @param additonalWbType for mapping Oracle's char/byte semantics. Use -1 if not needed
	 * @return the SQL "display" for the given datatype
	 */
	String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits, int additonalWbType);
}
