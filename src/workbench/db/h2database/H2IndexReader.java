/*
 * H2IndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * An index reader for H2
 *
 * Because of that, the name of the primary key might not match the index supporting that primary key.
 * <br/>
 * Therefor a separate class to read the name of the index supporting the PK is necessary.
 * <br/>
 * For versions before 1.106 this works as well. In this case the name of the primary
 * key is always identical to the index name.
 *
 * @author Thomas Kellerer
 */
public class H2IndexReader
	extends JdbcIndexReader
{
	private Statement primaryKeysStatement;
	private boolean useJDBCRetrieval;

	public H2IndexReader(DbMetadata meta)
	{
		super(meta);
		this.useJDBCRetrieval = Settings.getInstance().getBoolProperty("workbench.db.h2.getprimarykeyindex.usejdbc", false);
		if (!this.useJDBCRetrieval)
		{
			this.pkIndexNameColumn = "PK_INDEX_NAME";
		}
	}

	@Override
	protected ResultSet getPrimaryKeyInfo(String catalog, String schema, String table)
		throws SQLException
	{
		if (useJDBCRetrieval)
		{
			return super.getPrimaryKeyInfo(catalog, schema, table);
		}

		if (primaryKeysStatement != null)
		{
			LogMgr.logWarning("H2IndexReader.getPrimeryKeys()", "getPrimeryKeys() called with pending statement!");
			primaryKeysResultDone();
		}

		String sql = "" +
			"SELECT constraint_name as pk_name, \n" +
			"       index_name as pk_index_name, \n" +
			"       column_name, \n " +
			"       ordinal_position as key_seq\n " +
      "FROM information_schema.indexes \n" +
      "WHERE primary_key = true \n";

		primaryKeysStatement = this.metaData.getSqlConnection().createStatement();
		if (StringUtil.isNonBlank(schema))
		{
			sql += " AND table_schema = '" + StringUtil.trimQuotes(schema) + "' ";
		}
		sql += " AND table_name = '" + StringUtil.trimQuotes(table) + "'";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("H2IndexReader.getPrimaryKeys()", "Using query=\n" + sql);
		}
		return primaryKeysStatement.executeQuery(sql);
	}

	@Override
	protected void primaryKeysResultDone()
	{
		SqlUtil.closeStatement(primaryKeysStatement);
		primaryKeysStatement = null;
	}

}
