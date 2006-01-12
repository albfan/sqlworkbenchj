/*
 * DB2InfoReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibmdb2;

import java.sql.ResultSet;
import java.sql.Statement;
import workbench.db.SchemaInformationReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class DB2InfoReader
	implements SchemaInformationReader
{

	/** Creates a new instance of DB2InfoReader */
	public DB2InfoReader()
	{
	}

	public String getCurrentSchema(WbConnection conn)
	{
		String schemaQuery = "values(current_schema)";
		Statement stmt = null;
		ResultSet rs = null;
		String currentSchema = null;
		try
		{
			stmt = conn.createStatementForQuery();
			rs = stmt.executeQuery(schemaQuery);
			if (rs.next())
			{
				currentSchema = rs.getString(1);
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DB2InfoReader.getCurrentSchema()", "Error reading current schema", e);
			currentSchema = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return currentSchema;
	}
	
}
