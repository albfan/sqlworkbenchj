/*
 * ErrorInformationReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

/**
 * @author support@sql-workbench.net
 */
public interface ErrorInformationReader
{
	String getErrorInfo(String schema, String object, String type);
}
