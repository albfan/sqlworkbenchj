/*
 * Db2ConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.db.AbstractConstraintReader;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;

/**
 * Constraint reader for the Derby database
 * @author  Thomas Kellerer
 */
public class Db2ConstraintReader
	extends AbstractConstraintReader
{
	private final String HOST_TABLE_SQL =
		"select checkname, '('||checkcondition||')' \n" +
		"from  sysibm.syschecks \n" +
		"where tbname = ? " +
		"  and tbowner = ?";

	private final String AS400_TABLE_SQL =
		"select chk.constraint_name, '('||chk.check_clause||')' \n" +
		"from  qsys2.syschkcst chk \n" +
		"  JOIN qsys2.syscst cons ON cons.constraint_schema = chk.constraint_schema AND cons.constraint_name = chk.constraint_name " +
		"where cons.table_name = ? " +
		"  and cons.table_schema = ?";

	private final String LUW_TABLE_SQL =
		"select cons.constname, '('||cons.text||')' \n" +
		"from syscat.checks cons \n" +
		"where type <> 'S' " +
		"  AND tabname = ? " +
		"  and tabschema = ?";

	private final boolean isHostDB2;
	private final boolean isAS400; // aka iSeries

	private Pattern sysname = Pattern.compile("^SQL[0-9]+");
	private char catalogSeparator;

	public Db2ConstraintReader(WbConnection conn)
	{
		super(conn.getDbId());
		String dbid = conn.getDbId();
    isHostDB2 = dbid.equals(DbMetadata.DBID_DB2_ZOS);
		isAS400 = dbid.equals(DbMetadata.DBID_DB2_ISERIES);
		catalogSeparator = conn.getMetadata().getCatalogSeparator();
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
		if (isAS400) return AS400_TABLE_SQL.replace("qsys2.", "qsys2" + catalogSeparator);
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
