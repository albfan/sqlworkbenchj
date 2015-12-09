/*
 * H2IndexReader.java
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
package workbench.db.h2database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;

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
			"SELECT table_catalog as table_cat, \n" +
			"       table_schema as table_schem, \n" +
			"       table_name, \n" +
			"       column_name, \n " +
			"       ordinal_position as key_seq, \n " +
			"       constraint_name as pk_name, \n" +
			"       index_name as pk_index_name \n" +
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

  @Override
  public CharSequence getIndexSource(TableIdentifier table, IndexDefinition indexDefinition)
  {
    if (indexDefinition == null) return null;
    
    if (indexDefinition.isUniqueConstraint())
    {
      return getUniqueConstraint(table, indexDefinition);
    }
    return super.getIndexSource(table, indexDefinition);
  }


}
