/*
 * PostgresIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import workbench.db.DbMetadata;
import workbench.db.IndexReader;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A extension to the JdbcIndexReader to construct the Postgres specific syntax
 * for indexes.
 *
 * This class does not actually construct the CREATE INDEX based on the information
 * available from the JDBC API, but retrieves the CREATE INDEX directly from the database
 * as Postgres stores the full comannd in the table <tt>pg_indexes</tt>.
 * 
 * @author  support@sql-workbench.net
 */
public class PostgresIndexReader
	extends JdbcIndexReader
{
	public PostgresIndexReader(DbMetadata meta)
	{
		super(meta);
	}
	
	public StringBuilder getIndexSource(TableIdentifier table, DataStore indexDefinition, String tableNameToUse)
	{
		if (indexDefinition == null) return null;
		if (indexDefinition.getRowCount() == 0) return null;
		
		WbConnection con = this.metaData.getWbConnection();
		Statement stmt = null;
		ResultSet rs = null;
		
		// The full CREATE INDEX Statement is stored in pg_indexes for each 
		// index. So all we need to do, is retrieve the indexdef value
		// from that table for all indexes defined for this table.
		
		StringBuilder sql = new StringBuilder(50 + indexDefinition.getRowCount() * 20); 
		sql.append("SELECT indexdef FROM pg_indexes WHERE indexname in (");
			
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		int count = indexDefinition.getRowCount();
		if (count == 0) return StringUtil.emptyBuffer();
		
		StringBuilder source = new StringBuilder(count * 50);
		Savepoint sp = null;
		int indexCount = 0;
		try
		{
			
			for (int i = 0; i < count; i++)
			{
				String idxName = indexDefinition.getValueAsString(i, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME);
				String pk = indexDefinition.getValueAsString(i, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG);
				if ("YES".equalsIgnoreCase(pk)) continue;
				if (i > 0) sql.append(',');
				sql.append('\'');
				sql.append(idxName);
				sql.append('\'');
				indexCount ++;
			}
			sql.append(')');
			if (indexCount > 0)
			{
				sp = con.setSavepoint();
				stmt = con.createStatement();

				rs = stmt.executeQuery(sql.toString());
				while (rs.next())
				{
					source.append(rs.getString(1));
					source.append(';');
					source.append(nl);
				}
				con.releaseSavepoint(sp);
			}
		}
		catch (Exception e)
		{
			con.rollback(sp);
			LogMgr.logError("PostgresIndexReader.getIndexSource()", "Error retrieving source", e);
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		
		if (source.length() > 0) source.append(nl);
		
		return source;
	}

}
