/*
 * H2IndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.h2database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.DbMetadata;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * The newer versions of H2 correctly return the defined name for a Primary key
 * in the getPrimaryKeys() call. Because of that, the name of the primary
 * key might not match the index supporting that primary key.
 * <br/>
 * Therefor a separate class to read the name of the index supporting the PK
 * is necessary.
 * <br/>
 * For versions before 1.106 this works as well. In this case the name of the primary
 * key is always identical to the index name.
 *
 * @author Thomas Kellerer
 */
public class H2IndexReader
	extends JdbcIndexReader
{
	public H2IndexReader(DbMetadata meta)
	{
		super(meta);
	}

	@Override
	public String getPrimaryKeyIndex(TableIdentifier tbl)
	{
		String pkName = null;

		String sql = "" +
			"SELECT index_name  \n" +
      "FROM information_schema.indexes \n" +
      "WHERE index_type_name = 'PRIMARY KEY' \n";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("H2IndexReader.getPrimaryKeyIndex()", "Using query=\n" + sql);
		}
		
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			stmt = this.metaData.getSqlConnection().createStatement();
			if (!metaData.ignoreSchema(tbl.getSchema()))
			{
				String schema = StringUtil.trimQuotes(tbl.getSchema());
				sql += " AND table_schema = '" + schema + "' ";
			}
			sql += " AND table_name = '" + StringUtil.trimQuotes(tbl.getTableName()) + "'";

			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				pkName = rs.getString(1);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("H2IndexReader.getPrimaryKeyIndex()", "Error reading index name", e);
			pkName = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return pkName;
	}
	
}
