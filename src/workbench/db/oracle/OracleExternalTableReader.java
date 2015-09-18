/*
 * OracleExternalTableReader.java
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
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read and re-create the definition of an external table for Oracle.
 *
 * @author Thomas Kellerer
 */
public class OracleExternalTableReader
{
	private String baseSql =
      "-- SQL Workbench \n" +
      "select et.owner,  \n" +
      "       et.table_name,  \n" +
      "       et.type_name,  \n" +
      "       et.default_directory_name,  \n" +
      "       et.reject_limit,  \n" +
      "       et.access_parameters,  \n" +
      "       el.location, \n" +
      "       el.directory_name \n" +
      "from all_external_tables et \n" +
      "  join all_external_locations el on el.owner = et.owner and el.table_name = et.table_name " +
      " where et.owner = ? \n" +
      "   and et.table_name = ?";

	public CharSequence getDefinition(TableIdentifier table, WbConnection conn)
	{
		StringBuilder options = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleExternalTableReader.getDefinition()", "Retrieving external table definition using:\n" + SqlUtil.replaceParameters(baseSql, table.getRawSchema(), table.getRawTableName()));
		}

		try
		{
			pstmt = conn.getSqlConnection().prepareStatement(baseSql);
			pstmt.setString(1, table.getRawSchema());
			pstmt.setString(2, table.getRawTableName());
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String tableType = rs.getString("type_name");
				String defDir = rs.getString("default_directory_name");
				String limit = rs.getString("reject_limit");
				String access = rs.getString("access_parameters");
				String location = rs.getString("location");
				String dir = rs.getString("directory_name");
				options = new StringBuilder(100);
				options.append("ORGANIZATION EXTERNAL\n(\n");
				options.append("  TYPE ");
				options.append(tableType);
				options.append('\n');
				options.append("  DEFAULT DIRECTORY ");
				options.append(defDir);
				options.append('\n');
				options.append("  ACCESS PARAMETERS\n  (\n");
				options.append(fixIndention(access.trim(), "    "));
				options.append("\n  )\n");
				options.append("  LOCATION ('");
				if (!StringUtil.equalStringIgnoreCase(defDir, dir))
				{
					options.append(dir);
					options.append(':');
				}
				options.append(location);
				options.append("')\n");
				options.append(")\n");
				options.append("REJECT LIMIT ");
				options.append(limit);
			}
		}
		catch (SQLException sql)
		{
			LogMgr.logError("OracleExternalTableReader.getDefinition()","Error retrieving external table options using:\n" + baseSql, sql);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return options;
	}

	/**
	 * This method tries to merge our own indention and the one originally used
	 * in the access parameters.
	 *
	 * @param parameter
	 * @param baseIndent
	 */
	private StringBuilder fixIndention(String parameter, String baseIndent)
	{
		List<String> lines = StringUtil.getLines(parameter);
		StringBuilder result = new StringBuilder(lines.size() * 50);

		String indent = null;
		for (int i=0; i < lines.size(); i++)
		{
			if (lines.get(i).trim().startsWith(")"))
			{
				indent = null;
			}
			else if (i > 1 && lines.get(i-1).trim().startsWith("("))
			{
				String prevLine = lines.get(i-1);
				int pos = prevLine.indexOf('(');
				int start = StringUtil.findFirstNonWhitespace(lines.get(i));
				if (start > pos)
				{
					int numIndent = start - pos;
					indent = StringUtil.padRight(" ", numIndent);
				}
				else
				{
					indent = null;
				}
			}

			result.append(baseIndent);
			if (indent != null)
			{
				result.append(indent);
			}
			result.append(lines.get(i).trim());
			if (i < lines.size() - 1) result.append('\n');

		}
		return result;
	}
}
