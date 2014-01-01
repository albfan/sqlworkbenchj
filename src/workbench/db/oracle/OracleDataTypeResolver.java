/*
 * OracleDataTypeResolver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DataTypeResolver;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDataTypeResolver
	implements DataTypeResolver
{
	static final int BYTE_SEMANTICS = 1;
	static final int CHAR_SEMANTICS = 2;

	private final WbConnection connection;
	private int defaultCharSemantics = -1;
	private boolean alwaysShowCharSemantics = false;

	/**
	 * Only for testing purposes
	 */
	OracleDataTypeResolver(int defaultSemantics, boolean alwaysShowSemantics)
	{
		connection = null;
		defaultCharSemantics = defaultSemantics;
		alwaysShowCharSemantics = alwaysShowSemantics;
	}

	public OracleDataTypeResolver(WbConnection conn)
	{
		this.connection = conn;
		alwaysShowCharSemantics = Settings.getInstance().getBoolProperty("workbench.db.oracle.charsemantics.displayalways", true);

		if (!alwaysShowCharSemantics)
		{
			Statement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = this.connection.createStatement();
				String sql = "SELECT /* SQLWorkbench */ value FROM v$nls_parameters where parameter = 'NLS_LENGTH_SEMANTICS'";
				rs = stmt.executeQuery(sql);
				if (rs.next())
				{
					String v = rs.getString(1);
					if ("BYTE".equalsIgnoreCase(v))
					{
						defaultCharSemantics = BYTE_SEMANTICS;
					}
					else if ("CHAR".equalsIgnoreCase(v))
					{
						defaultCharSemantics = CHAR_SEMANTICS;
					}
				}
			}
			catch (Exception e)
			{
				defaultCharSemantics = BYTE_SEMANTICS;
				LogMgr.logWarning("OracleDataTypeResolver.<init>", "Could not retrieve NLS_LENGTH_SEMANTICS from v$nls_parameters. Assuming byte semantics", e);
			}
			finally
			{
				SqlUtil.closeAll(rs, stmt);
			}
		}
	}


	@Override
	public int fixColumnType(int type, String dbmsType)
	{
		if (type == Types.DATE && OracleUtils.getMapDateToTimestamp(connection)) return Types.TIMESTAMP;

		// Oracle reports TIMESTAMP WITH TIMEZONE with the numeric
		// value -101 (which is not an official java.sql.Types value
		// TIMESTAMP WITH LOCAL TIMEZONE is reported as -102
		if (type == -101 || type == -102) return Types.TIMESTAMP;

		// The Oracle driver stupidly reports TIMESTAMP(n) columns as Types.OTHER
		if (type == Types.OTHER && dbmsType != null && dbmsType.startsWith("TIMESTAMP("))
		{
			return Types.TIMESTAMP;
		}

		return type;
	}

	@Override
	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
	{
		return getSqlTypeDisplay(dbmsName, sqlType, size, digits, -1);
	}

	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits, int byteOrChar)
	{
		String display;

		if (sqlType == Types.VARCHAR)
		{
			// Hack to get Oracle's VARCHAR2(xx Byte) or VARCHAR2(xxx Char) display correct
			// My own statement to retrieve column information in OracleMetaData
			// will return the byte/char semantics in the field WB_SQL_DATA_TYPE
			// Oracle's JDBC driver does not supply this information (because
			// the JDBC standard does not define a column for this)
			display = getVarcharType(dbmsName, size, byteOrChar);
		}
		else if ("NUMBER".equalsIgnoreCase(dbmsName))
		{
			if (digits < 0)
			{
				return "NUMBER";
			}
			else if (digits == 0)
			{
				return "NUMBER(" + size + ")";
			}
			else
			{
				return "NUMBER(" + size + "," + digits + ")";
			}
		}
		else if (sqlType == Types.VARBINARY && "RAW".equals(dbmsName))
		{
			return "RAW(" + size  + ")";
		}
		else
		{
			display = SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
		}
		return display;
	}

	private String getVarcharType(String type, int size, int semantics)
	{
		StringBuilder result = new StringBuilder(25);
		result.append(type);
		result.append('(');
		result.append(size);

		// Only apply this logic on VARCHAR columns
		// NVARCHAR (which might have been reported as type == VARCHAR) does not allow Byte/Char semantics
		if (type.startsWith("VARCHAR"))
		{
			if (alwaysShowCharSemantics || semantics != defaultCharSemantics)
			{
				if (semantics < 0) semantics = defaultCharSemantics;
				if (semantics == BYTE_SEMANTICS)
				{
					result.append(" Byte");
				}
				else if (semantics == CHAR_SEMANTICS)
				{
					result.append(" Char");
				}
			}
		}
		result.append(')');
		return result.toString();
	}

	public int getDefaultCharSemantics()
	{
		return defaultCharSemantics;
	}

	@Override
	public String getColumnClassName(int type, String dbmsType)
	{
		// use default
		return null;
	}

}
