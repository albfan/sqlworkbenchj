/*
 * SqlServerDataTypeResolver.java
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
package workbench.db.mssql;

import java.sql.Types;

import workbench.db.DefaultDataTypeResolver;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerDataTypeResolver
	extends DefaultDataTypeResolver
{

	private static final int MAX_DEFAULT_LENGTH = 8000;
	private static final int MAX_NVARCHAR_LENGTH = 4000;

	@Override
	public String getColumnClassName(int type, String dbmsType)
	{
		if (Settings.getInstance().getFixSqlServerTimestampDisplay() && type == Types.BINARY && "timestamp".equals(dbmsType))
		{
			// RowData#readRow() will convert the byte[] into a hex String getFixSqlServerTimestampDisplay() is true
			// so we need to make sure, the class name is correct
			return "java.lang.String";
		}
		return null;
	}

	/**
	 * Returns the correct display for the given data type.
	 *
	 * @see workbench.util.SqlUtil#getSqlTypeDisplay(java.lang.String, int, int, int)
	 */
	@Override
	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
	{
		// the new hierarchyid type is reported as VARBINARY (or BLOB by jTDS),
		// so this needs to be checked before checking the "real" BLOB and VARBINARY columns
		if ("hierarchyid".equals(dbmsName))
		{
			return dbmsName;
		}

		// this works around the jTDS driver reporting nvarchar as CLOB and not as NCLOB
		if (sqlType == Types.CLOB && "nvarchar".equals(dbmsName) && size > MAX_NVARCHAR_LENGTH)
		{
			return "nvarchar(max)";
		}

		if (sqlType == Types.BLOB && "varbinary".equals(dbmsName))
		{
			return "varbinary(max)";
		}

		if ( (sqlType == Types.NVARCHAR && size > MAX_NVARCHAR_LENGTH) || sqlType == Types.NCLOB)
		{
			return "nvarchar(max)";
		}

		if ( (sqlType == Types.VARCHAR && size > MAX_DEFAULT_LENGTH) || sqlType == Types.CLOB)
		{
			return "varchar(max)";
		}

		if (sqlType == Types.BLOB)
		{
			return "varbinary(max)";
		}

		if (sqlType == Types.VARBINARY)
		{
			if (size > MAX_DEFAULT_LENGTH)
			{
				return "varbinary(max)";
			}
			return "varbinary(" + Integer.toString(size) + ")";
		}

		if (sqlType == Types.BINARY)
		{
			if (size > MAX_DEFAULT_LENGTH)
			{
				return "binary(max)";
			}
			return "binary(" + Integer.toString(size) + ")";
		}

		if (sqlType == Types.TIME && digits > 0)
		{
			return "time(" + digits + ")";
		}
		return super.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
	}

	/**
	 * Fix a bug in jTDS that reports a date or time column as Types.VARCHAR.
	 *
	 * @param type the java.sql.Types as returned from the driver
	 * @param dbmsType the DBMS data type as returned from the driver
	 *
	 * @return the correct Types.XXX value for the dbmsType
	 */
	@Override
	public int fixColumnType(int type, String dbmsType)
	{
		if (type == Types.VARCHAR)
		{
			// Fix the jTDS bug that reports a date or time column as VARCHAR
			// no check for the driver is done as these combinations will not show up with the Microsoft Driver
			if ("date".equalsIgnoreCase(dbmsType))
			{
				return Types.DATE;
			}
			if ("time".equalsIgnoreCase(dbmsType))
			{
				return Types.TIME;
			}
		}
		return type;
	}
}
