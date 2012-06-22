/*
 * Db2SearchPath.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.DbSearchPath;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2SearchPath
	implements DbSearchPath
{
	/**
	 * Returns the current search path defined in the session (or the user).
	 * <br/>
	 * @param con the connection for which the search path should be retrieved
	 * @return the list of schemas (libraries) in the search path.
	 */
	@Override
	public List<String> getSearchPath(WbConnection con, String defaultSchema)
	{
		if (con == null) return Collections.emptyList();

		if (defaultSchema != null)
		{
			return Collections.singletonList(con.getMetadata().adjustSchemaNameCase(defaultSchema));
		}

		List<String> result = new ArrayList<String>();

		ResultSet rs = null;
		Statement stmt = null;
		String sql = getSQL(con.getDbId());
		LogMgr.logDebug("Db2SearchPath.getSearchPath()", "Using statement: " + sql);

		try
		{
			stmt = con.createStatementForQuery();

			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String row = rs.getString(1);
				if (StringUtil.isNonBlank(row))
				{
					result.add(row.trim());
				}
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("Db2SearchPath.getSearchPath()", "Could not read search path", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		List<String> searchPath = parseResult(result);

		LogMgr.logDebug("Db2SearchPath.getSearchPath()", "Using path: " + searchPath.toString());
		return searchPath;
	}

	private String getSQL(String dbid)
	{
		return Settings.getInstance().getProperty("workbench.db." + dbid + ".retrieve.searchpath", "values(current_path)");
	}
	
	List<String> parseResult(List<String> entries)
	{
		List<String> searchPath = new ArrayList<String>(entries.size());
		for (String line : entries)
		{
			if (line.charAt(0) != '*')
			{
				searchPath.addAll(StringUtil.stringToList(line, ",", true, true, false, false));
			}
		}
		return searchPath;
	}

}
