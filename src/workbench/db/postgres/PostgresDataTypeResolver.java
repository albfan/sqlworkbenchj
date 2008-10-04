/*
 * PgDataTypeResolver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import workbench.db.DataTypeResolver;
import workbench.util.SqlUtil;

/**
 * @author support@sql-workbench.net
 */
public class PostgresDataTypeResolver
	implements DataTypeResolver
{

	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits, int wbTypeInfo)
	{
		if (sqlType == java.sql.Types.NUMERIC || sqlType == java.sql.Types.DECIMAL)
		{
			if (size == 65535 || size == 131089) size = 0;
			if (digits == 65531) digits = 0;
		}
		return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
	}

}
