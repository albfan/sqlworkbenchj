/*
 * H2ConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.h2database;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.AbstractConstraintReader;

/**
 * Constraint reader for <a href="http://www.h2database.com">H2 Database</a>
 *
 * @author  Thomas Kellerer
 */
public class H2ConstraintReader
	extends AbstractConstraintReader
{

	private final String COLUMN_SQL =
		"select column_name constraint_name, " +
		"       case when \n" +
		"           check_constraint is not null and trim(check_constraint) <> '' then 'CHECK '||check_constraint \n" +
		"           else null \n" +
		"       end as check_constraint \n" +
		"from information_schema.columns \n" +
		"where table_name = ? \n" +
		"  and table_schema = ?";

	private final String TABLE_SQL =
		"select constraint_name, \n" +
		"       check_expression \n" +
		"from information_schema.constraints \n" +
		"where constraint_type = 'CHECK'  \n" +
		"and table_name = ? \n" +
		" and table_schema = ?";

	private Pattern systemNamePattern = Pattern.compile("^(CONSTRAINT_[0-9A-F][0-9A-F])");

	@Override
	public int getIndexForSchemaParameter()
	{
		return 2;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 1;
	}

	@Override
	public String getColumnConstraintSql()
	{
		return this.COLUMN_SQL;
	}

	@Override
	public String getTableConstraintSql()
	{
		return this.TABLE_SQL;
	}

	@Override
	public boolean isSystemConstraintName(String name)
	{
		Matcher m = systemNamePattern.matcher(name);
		return m.matches();
	}


}
