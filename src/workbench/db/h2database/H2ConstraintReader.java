/*
 * H2ConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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

	private final String TABLE_SQL =
		"select constraint_name, check_expression \n" +
		"from information_schema.constraints \n" +
		"where constraint_type = 'CHECK'  \n" +
		"and table_name = ? \n" +
		"and table_schema = ?";

	private Pattern systemNamePattern = Pattern.compile("^(CONSTRAINT_[0-9A-F][0-9A-F])");
	
	public int getIndexForSchemaParameter()
	{
		return 2;
	}

	public int getIndexForTableNameParameter()
	{
		return 1;
	}

	public String getColumnConstraintSql()
	{
		return null;
	}

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
