/*
 * Db2ConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.AbstractConstraintReader;

/**
 * Constraint reader for the Derby database
 * @author  support@sql-workbench.net
 */
public class Db2ConstraintReader 
	extends AbstractConstraintReader
{
	private static final String HOST_TABLE_SQL = "select checkname, '('||checkcondition||')' \n" +
					 "from  sysibm.syschecks \n" +
					 "where tbname = ? " +
					 "and tbowner = ?";
	
	private static final String LUW_TABLE_SQL = "select cons.constname, '('||cons.text||')' \n" +
					 "from syscat.checks cons \n" +
					 "where tabname = ? " +
					 "and tabschema = ?";

	private final boolean isHostDB2;

	private Pattern sysname = Pattern.compile("^SQL[0-9]+");
	
	public Db2ConstraintReader(String dbid)
	{
		isHostDB2 = dbid.equals("db2h");
	}

	@Override
	public boolean isSystemConstraintName(String name)
	{
		if (name == null) return false;
		Matcher m = sysname.matcher(name);
		return m.matches();
	}

	public String getColumnConstraintSql()
	{
		return null;
	}
	
	public String getTableConstraintSql() 
	{
		if (isHostDB2) return HOST_TABLE_SQL;
		return LUW_TABLE_SQL;
	}
	
	public int getIndexForTableNameParameter()
	{
		return 1;
	}

	public int getIndexForSchemaParameter()
	{
		return 2;
	}
}
