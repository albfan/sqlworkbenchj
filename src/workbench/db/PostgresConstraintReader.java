/*
 * PostgresConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db;


/**
 *
 * @author  info@sql-workbench.net
 */
public class PostgresConstraintReader extends AbstractConstraintReader
{
	private static final String TABLE_SQL = 
					 "select rel.rcsrc \n" + 
           "from pg_class t, pg_relcheck rel \n" + 
           "where t.relname = ? \n" + 
           "and   t.oid = rel.rcrelid \n";
	
	public PostgresConstraintReader()
	{
	}
	
	public String getPrefixTableConstraintKeyword() { return "check"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }


}
