/*
 * PostgresMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.DbMetadata;
import workbench.db.IndexReader;
import workbench.db.JdbcIndexReader;
import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;

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
	
	public StrBuffer getIndexSource(TableIdentifier table, DataStore indexDefinition, String tableNameToUse)
	{
		Connection con = this.metaData.getSqlConnection();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String sql = "SELECT indexdef FROM pg_indexes WHERE indexname = ? ";
		int count = indexDefinition.getRowCount();
		if (count == 0) return StrBuffer.EMPTY_BUFFER;
		StrBuffer source = new StrBuffer(count * 50);
		boolean includeSchema = false;
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
					source.append(";\n");
				}
			}
			source.append('\n');
		}
		catch (Exception e)
		{
			e.printStackTrace();
			source = new StrBuffer(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}

}
