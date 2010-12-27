/*
 * PostgresDataTypeResolver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.sqlite;

import java.sql.Types;
import java.util.Set;
import workbench.db.DataTypeResolver;
import workbench.util.CollectionUtil;

/**
 * @author Thomas Kellerer
 */
public class SQLiteDataTypeResolver
	implements DataTypeResolver
{

	private Set<String> INTEGER_TYPES = CollectionUtil.caseInsensitiveSet(
		"INT", "INTEGER", "TINYINT", "SMALLINT", "MEDIUMINT",
		"BIGINT", "UNSIGNED BIG INT",
		"INT2", "INT8");

	private Set<String> BIG_INTEGER_TYPES = CollectionUtil.caseInsensitiveSet("BIGINT", "UNSIGNED BIG INT", "INT8");

	private Set<String> CHARACTER_TYPES = CollectionUtil.caseInsensitiveSet("VARCHAR", "CHARACTER", "VARYING CHARACTER", "NCHAR", "NATIVE CHARACTER", "NVARCHAR");

	private Set<String> CLOB_TYPES = CollectionUtil.caseInsensitiveSet("TEXT", "CLOB");

	private Set<String> DECIMAL_TYPES = CollectionUtil.caseInsensitiveSet("NUMERIC", "DECIMAL");

	private Set<String> DOUBLE_TYPES = CollectionUtil.caseInsensitiveSet("REAL", "DOUBLE", "DOUBLE PRECISION", "FLOAT");

	@Override
	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
	{
		// SQLite already returns a fully "qualified" data type, including any length definition if necessary
		return dbmsName;
	}

	@Override
	public int fixColumnType(int type, String dbmsType)
	{
		String plainType = getPlainTypeName(dbmsType);

		if (INTEGER_TYPES.contains(plainType))
		{
			return Types.INTEGER;
		}
		if (BIG_INTEGER_TYPES.contains(plainType))
		{
			return Types.BIGINT;
		}
		if (CHARACTER_TYPES.contains(plainType))
		{
			return Types.VARCHAR;
		}
		if (DECIMAL_TYPES.contains(plainType))
		{
			return Types.DECIMAL;
		}
		if (DOUBLE_TYPES.contains(plainType))
		{
			return Types.DOUBLE;
		}
		if (CLOB_TYPES.contains(plainType))
		{
			return Types.CLOB;
		}
		if (dbmsType.equalsIgnoreCase("BLOB"))
		{
			return Types.BLOB;
		}
		return type;
	}

	@Override
	public String getColumnClassName(int type, String dbmsType)
	{
		switch (type)
		{
			case Types.DOUBLE:
			case Types.DECIMAL:
				return "java.lang.Double";
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
				return "java.lang.Integer";
			case Types.BIGINT:
				return "java.math.BigInt";
			default:
				return "java.lang.String";
		}
	}

	private String getPlainTypeName(String input)
	{
		int pos = input.indexOf('(');
		if (pos < 0) return input;
		return input.substring(0, pos);
	}
}
