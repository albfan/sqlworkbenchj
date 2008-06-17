/*
 * DerbyConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import workbench.db.AbstractConstraintReader;

/**
 * Constraint reader for the Derby database
 * @author  support@sql-workbench.net
 */
public class Db2ConstraintReader 
	extends AbstractConstraintReader
{
	private static final String TABLE_SQL = "select 'check ('||text||') ' \n" +
					 "from syscat.checks cons \n" +
					 "where tabname = ? " +
					 "and tabschema = ?";

						 
	public Db2ConstraintReader()
	{
	}
	
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }
	
	public int getIndexForTableNameParameter() { return 1; }
	public int getIndexForSchemaParameter() { return 2; }
}
