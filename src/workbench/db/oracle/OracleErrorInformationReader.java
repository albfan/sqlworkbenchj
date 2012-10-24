/*
 * OracleErrorInformationReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.db.DbMetadata;
import workbench.db.ErrorInformationReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
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
		String query =
			"SELECT /* SQLWorkbench */ line, position, text, name, type \n" +
			" FROM all_errors \n" +
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
			if (objectName != null && objectName.indexOf('.') > -1)
			{
				schema = objectName.substring(0, objectName.indexOf('.'));
			}
			else if (schema == null)
			{
				schema = connection.getMetadata().getCurrentSchema();
			}
			DbMetadata meta = this.connection.getMetadata();

			stmt = this.connection.getSqlConnection().prepareStatement(query);

			schema = meta.adjustSchemaNameCase(StringUtil.trimQuotes(schema));
			String otype = objectType.toUpperCase().trim();
			String oname = meta.adjustObjectnameCase(StringUtil.trimQuotes(objectName));

			stmt.setString(1, schema);
			
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
			int indentLength = 14;

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
					lineInfo = StringUtil.padRight(lineInfo, indentLength + 2);
					result.append(lineInfo);
					// indent all lines of the message
					msg = msg.trim().replaceAll(StringUtil.REGEX_CRLF, "\n" + indent);
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
