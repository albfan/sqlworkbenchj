/*
 * DDLFilter.java
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
 * A Filter that can be applied before running DDL statements.
 *
 * Currently only used for PostgreSQL
 *
 * @author support@sql-workbench.net
 * @see workbench.db.postgres.PostgresDDLFilter
 * @see workbench.sql.commands.DdlCommand#execute(java.lang.String)
 * @see workbench.db.ProcedureCreator#recreate()
 */
public interface DDLFilter
{
	String adjustDDL(String sql);
}
