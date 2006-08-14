/*
 * PostgresIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import workbench.db.DbMetadata;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author  support@sql-workbench.net
 */
public class PostgresIndexReader
	extends JdbcIndexReader
{
	public PostgresIndexReader(DbMetadata meta)
	{
		super(meta);
	}
	
	public StringBuffer getIndexSource(TableIdentifier table, DataStore indexDefinition, String tableNameToUse)
	{
		Connection con = this.metaData.getSqlConnection();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String sql = "SELECT indexdef FROM pg_indexes WHERE indexname = ? ";
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		int count = indexDefinition.getRowCount();
		if (count == 0) return StringUtil.emptyBuffer();
		StringBuffer source = new StringBuffer(count * 50);
		try
		{
			stmt = con.prepareStatement(sql);
			for (int i = 0; i < count; i++)
			{
				String idxName = indexDefinition.getValueAsString(i, 0);
				stmt.setString(1, idxName);
				rs = stmt.executeQuery();
				if (rs.next())
				{
					source.append(rs.getString(1));
					source.append(";");
					source.append(nl);
				}
			}
					source.append(nl);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			source = new StringBuffer(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}

}
