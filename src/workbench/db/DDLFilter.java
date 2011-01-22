/*
 * DDLFilter.java
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
 * A Filter that can be applied before running DDL statements.
 *
 * Currently only used for PostgreSQL
 *
 * @author Thomas Kellerer
 * @see workbench.db.postgres.PostgresDDLFilter
 * @see workbench.sql.commands.DdlCommand#execute(java.lang.String)
 */
public interface DDLFilter
{
	String adjustDDL(String sql);
}
