/*
 * PostgresDataTypeResolver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

	@Override
	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
	{
		if (sqlType == Types.VARCHAR && "text".equals(dbmsName)) return "text";
		if (sqlType == Types.SMALLINT && "int2".equals(dbmsName)) return "smallint";
		if (sqlType == Types.INTEGER && "int4".equals(dbmsName)) return "integer";
		if (sqlType == Types.BIGINT && "int8".equals(dbmsName)) return "bigint";
		if ((sqlType == Types.BIT || sqlType == Types.BOOLEAN) && "bool".equals(dbmsName)) return "boolean";

		if (sqlType == Types.CHAR && "bpchar".equals(dbmsName))
		{
			return "char(" + size + ")";
		}

		if (sqlType == Types.VARCHAR && size == Integer.MAX_VALUE)
		{
			// enums are returned as Types.VARCHAR and size == Integer.MAX_VALUE
			// in order to not change the underlying data type, we just use
			// the type name that the driver returned
			return dbmsName;
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
		if (sqlType == Types.ARRAY && dbmsName.charAt(0) == '_')
		{
			if ("_int2".equals(dbmsName)) return "smallint[]";
			if ("_int4".equals(dbmsName)) return "integer[]";
			if ("_int8".equals(dbmsName)) return "bigint[]";
		}
		if ("_varchar".equals(dbmsName)) return "varchar[]";

		return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
	}

	@Override
	public String getColumnClassName(int type, String dbmsType)
	{
		return null;
	}

	@Override
	public int fixColumnType(int type, String dbmsType)
	{
		if (type == Types.BIT && "bool".equals(dbmsType)) return Types.BOOLEAN;
		return type;
	}

}
