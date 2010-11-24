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
package workbench.db.postgres;

import java.sql.Types;
import workbench.db.DataTypeResolver;
import workbench.util.SqlUtil;

/**
 * @author Thomas Kellerer
 */
public class PostgresDataTypeResolver
	implements DataTypeResolver
{

	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
	{
		if (sqlType == Types.VARCHAR && "text".equals(dbmsName)) return "text";
		if (sqlType == Types.SMALLINT && "int2".equals(dbmsName)) return "smallint";
		if (sqlType == Types.INTEGER && "int4".equals(dbmsName)) return "integer";
		if (sqlType == Types.BIGINT && "int8".equals(dbmsName)) return "bigint";
		if (sqlType == Types.BIT && "bool".equals(dbmsName)) return "boolean";
		
		if (sqlType == Types.CHAR && "bpchar".equals(dbmsName))
		{
			return "char(" + size + ")";
		}

		if (sqlType == Types.VARCHAR && size == Integer.MAX_VALUE)
		{
			return "varchar";
		}

		if (sqlType == Types.NUMERIC || sqlType == Types.DECIMAL)
		{
			if (size == 65535 || size == 131089) size = 0;
			if (digits == 65531) digits = 0;
		}
		
		if (sqlType == Types.OTHER && "varbit".equals(dbmsName))
		{
			return "bit varying(" + size + ")";
		}

		if (sqlType == Types.BIT && "bit".equals(dbmsName))
		{
			return "bit(" + size + ")";
		}
		return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
	}

	@Override
	public int fixColumnType(int type, String dbmsType)
	{
		if (type == Types.BIT && "bool".equals(dbmsType)) return Types.BOOLEAN;
		return type;
	}

}
