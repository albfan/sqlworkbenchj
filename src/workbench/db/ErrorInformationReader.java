/*
 * ErrorInformationReader.java
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
 * An interface for reading extended error information from the database.
 *
 * This should be implemented for any database that can return additional
 * error information after running DDL statements.
 *
 * Currently only implemented for Oracle to retrieve detailed error messages
 * after a CREATE PROCEDURE or similar statement.
 * 
 * @author support@sql-workbench.net
 */
public interface ErrorInformationReader
{
	String getErrorInfo(String schema, String object, String type);
}
