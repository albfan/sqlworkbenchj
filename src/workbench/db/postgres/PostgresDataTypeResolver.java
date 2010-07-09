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
		if ("text".equalsIgnoreCase(dbmsName)) return "text";
		if (sqlType == Types.CHAR && "bpchar".equalsIgnoreCase(dbmsName))
		{
			return "char(" + size + ")";
		}
		if (sqlType == Types.VARCHAR && size == Integer.MAX_VALUE)
		{
			return "VARCHAR";
		}
		if (sqlType == Types.NUMERIC || sqlType == Types.DECIMAL)
		{
			if (size == 65535 || size == 131089) size = 0;
			if (digits == 65531) digits = 0;
		}
		return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
	}

	@Override
	public int fixColumnType(int type)
	{
		return type;
	}

}
