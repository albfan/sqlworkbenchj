/*
 * OracleErrorInformationReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ErrorInformationReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An ErrorInformationReader for Oracle that retrieves the errors for the last DDL command.
 *
 * This is basically what SQL*Plus' "show errors" does.
 *
 * @author Thomas Kellerer
 */
public class OracleErrorInformationReader
	implements ErrorInformationReader
{
	private WbConnection connection;

	public OracleErrorInformationReader(WbConnection conn)
	{
		this.connection = conn;
	}

	/**
	 *	Return the errors reported in the all_errors table for Oracle.
	 *	This method can be used to obtain error information after a CREATE PROCEDURE
	 *	or CREATE TRIGGER statement has been executed.
	 *
	 *	@return extended error information if available
	 */
	@Override
	public String getErrorInfo(String schema, String objectName, String objectType, boolean formatMessages)
	{
		if (StringUtil.isEmptyString(objectName))
		{
			return null;
		}
		
		String query =
			"SELECT /* SQLWorkbench */ line, position, text, name, type \n" +
			"FROM all_errors \n" +
			"WHERE owner = ? \n";

		int typeIndex = -1;
		int nameIndex = -1;

		if (objectType != null)
		{
			query += " and type = ? \n";
			typeIndex = 2;
		}

		if (objectName != null)
		{
			query += "  and name = ? \n";
			nameIndex = typeIndex == -1 ? 2 : 3;
		}

		query += " ORDER BY type, name, line, position";

		PreparedStatement stmt = null;
		ResultSet rs = null;

		StringBuilder result = new StringBuilder(250);
		try
		{
			TableIdentifier tbl = new TableIdentifier(objectName);

			tbl.adjustCase(connection);
			String oschema = schema == null ? tbl.getRawSchema() : schema;
			String oname = tbl.getRawTableName();

			if (oschema == null)
			{
				oschema = connection.getMetadata().getCurrentSchema();
			}
			stmt = this.connection.getSqlConnection().prepareStatement(query);

			String otype = objectType == null ? null : objectType.toUpperCase().trim();

			stmt.setString(1, oschema);

			if (typeIndex > -1)
			{
				stmt.setString(typeIndex, otype);
			}
			if (nameIndex > -1)
			{
				stmt.setString(nameIndex, oname);
			}

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleErrorInformationReader.getErrorInfo()", "Using SQL: " + SqlUtil.replaceParameters(query, schema, otype, oname));
			}

			rs = stmt.executeQuery();
			int count = 0;
			String currentName = null;

			boolean firstHeading = true;
			int indentLength = 15;

			String indent = StringUtil.padRight("", indentLength);
			while (rs.next())
			{
				if (count > 0)
				{
					result.append("\n");
				}
				int line = rs.getInt(1);
				int pos = rs.getInt(2);
				String msg = rs.getString(3);
				String name = rs.getString(4);
				String type = rs.getString(5);

				if (formatMessages && (currentName == null || !currentName.equals(name)))
				{
					if (firstHeading)
					{
						firstHeading = false;
					}
					else
					{
						result.append('\n');
					}
					String heading = ResourceMgr.getFormattedString("ErrForObject", type, name);
					String divide = StringUtil.padRight("", heading.length(), '-');
					result.append(heading);
					result.append('\n');
					result.append(divide);
					result.append("\n");
					currentName = name;
				}

				if (formatMessages)
				{
					String lineInfo = ResourceMgr.getFormattedString("ErrAtLinePos", Integer.valueOf(line), Integer.valueOf(pos));
					lineInfo = StringUtil.padRight(lineInfo, indentLength);
					result.append(lineInfo);
					msg = msg.trim().replace("\n\n", "\n"); // remove duplicate newlines
					// indent all lines of the message
					msg = msg.replaceAll(StringUtil.REGEX_CRLF, "\n" + indent);
				}
				result.append(msg);
				count++;
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleErrorInformationReader.getErrorInfo()", "Error retrieving error information", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result.toString();
	}

}
