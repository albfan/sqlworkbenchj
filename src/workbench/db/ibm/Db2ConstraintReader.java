/*
 * Db2ConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
 * @author  Thomas Kellerer
 */
public class Db2ConstraintReader
	extends AbstractConstraintReader
{
	private static final String HOST_TABLE_SQL = "select checkname, '('||checkcondition||')' \n" +
					 "from  sysibm.syschecks \n" +
					 "where tbname = ? " +
					 "and tbowner = ?";

	private static final String AS400_TABLE_SQL = "select chk.constraint_name, '('||chk.check_clause||')' \n" +
					 "from  qsys2.syschkcst chk \n" +
					 "  JOIN qsys2.syscst cons ON cons.constraint_schema = chk.constraint_schema AND cons.constraint_name = chk.constraint_name " +
					 " where cons.table_name = ? " +
					 " and cons.table_schema = ?";

	private static final String LUW_TABLE_SQL = "select cons.constname, '('||cons.text||')' \n" +
					 "from syscat.checks cons \n" +
					 "where type <> 'S' " +
					 "AND tabname = ? " +
					 "and tabschema = ?";

	private final boolean isHostDB2;
	private final boolean isAS400; // aka iSeries

	private Pattern sysname = Pattern.compile("^SQL[0-9]+");

	public Db2ConstraintReader(String dbid)
	{
		isHostDB2 = dbid.equals("db2h");
		isAS400 = dbid.equals("db2i");
	}

	@Override
	public boolean isSystemConstraintName(String name)
	{
		if (name == null) return false;
		Matcher m = sysname.matcher(name);
		return m.matches();
	}

	@Override
	public String getColumnConstraintSql()
	{
		return null;
	}

	@Override
	public String getTableConstraintSql()
	{
		if (isHostDB2) return HOST_TABLE_SQL;
		if (isAS400) return AS400_TABLE_SQL;
		return LUW_TABLE_SQL;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 1;
	}

	@Override
	public int getIndexForSchemaParameter()
	{
		return 2;
	}
}
