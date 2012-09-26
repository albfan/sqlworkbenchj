/*
 * SqlServerDataTypeResolver
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
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

	private static final int MAX_INDICATOR = 2147483647;

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
		// this works around the jTDS driver reporting nvarchar as CLOB and not as NCLOB
		if (sqlType == Types.CLOB && "nvarchar".equals(dbmsName) && size > 8000)
		{
			return "nvarchar(max)";
		}

		if (sqlType == Types.BLOB && "varbinary".equals(dbmsName))
		{
			return "varbinary(max)";
		}

		if ( (sqlType == Types.NVARCHAR && size >= MAX_INDICATOR) || sqlType == Types.NCLOB)
		{
			return "nvarchar(max)";
		}
		if ( (sqlType == Types.VARCHAR && size >= MAX_INDICATOR) || sqlType == Types.CLOB)
		{
			return "varchar(max)";
		}
		if (sqlType == Types.BLOB)
		{
			return "varbinary(max)";
		}
		if (sqlType == Types.VARBINARY)
		{
			if (size >= MAX_INDICATOR)
			{
				return "varbinary(max)";
			}
			return "varbinary(" + Integer.toString(size) + ")";
		}

		return super.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
	}

}
