/*
 * OracleIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.JdbcUtils;
import workbench.db.ObjectSourceOptions;
import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An implementation of the IndexReader interface for Oracle.
 *
 * This class uses its own SQL Statement to retrieve the index list from the database
 * as Oracle's JDBC driver runs an ANALYZE before actually returning the index information (which is awfully slow).
 *
 * The use of the JDBC (i.e. driver's) function can be enabled by setting the workbench property:
 * <tt>workbench.db.oracle.indexlist.usejdbc=true</tt>
 *
 * Indexes that are returned with the type "DOMAIN" are always retrieve using DBMS_METADATA.GET_DDL
 *
 * @author Thomas Kellerer
 */
public class OracleIndexReader
	extends JdbcIndexReader
{
	private static final String PROP_USE_JDBC_FOR_PK_INFO = "workbench.db.oracle.getprimarykeyindex.usejdbc";
	private static final String PROP_USE_JDBC_FOR_INDEXLIST = "workbench.db.oracle.indexlist.usejdbc";

	private static final String IDX_PROP_COMPRESS = "compression_level";
	private PreparedStatement indexStatement;
	private PreparedStatement pkStament;
	private String defaultTablespace;
	private final boolean hasCompression;
	private String currentUser;

	public OracleIndexReader(DbMetadata meta)
	{
		super(meta);
		boolean useJdbcForPk = Settings.getInstance().getBoolProperty(PROP_USE_JDBC_FOR_PK_INFO, false);
		if (!useJdbcForPk)
		{
			pkIndexNameColumn = "PK_INDEX_NAME";
			pkStatusColumn = "PK_STATUS";
			partitionedFlagColumn = "PARTITIONED";
		}
		if (OracleUtils.checkDefaultTablespace())
		{
			defaultTablespace = OracleUtils.getDefaultTablespace(meta.getWbConnection());
		}
		hasCompression = JdbcUtils.hasMinimumServerVersion(meta.getWbConnection(), "9.0");
		currentUser = meta.getWbConnection().getCurrentUser();
	}

	@Override
	public void indexInfoProcessed()
	{
		SqlUtil.closeStatement(this.indexStatement);
		this.indexStatement = null;
	}

	/**
	 * Replacement for the DatabaseMetaData.getIndexInfo() method.
	 * <br/>
	 * Oracle's JDBC driver does an <tt>ANALYZE INDEX</tt> each time an getIndexInfo() is called which slows down the
	 * retrieval of index information (and is not necessary at all for SQL Workbench, because the cardinality field isn't
	 * displayed anyway)
	 * <br/>
	 * Additionally, function based indexes are not returned correctly by the Oracle driver which is also fixed with this method.
	 * <br/>
	 * When the workbench property <tt>workbench.db.oracle.indexlist.usejdbc</tt> is set to <tt>true</tt>
	 * the original driver API will be used.
	 */
	@Override
	public ResultSet getIndexInfo(TableIdentifier table, boolean unique)
		throws SQLException
	{
		if (Settings.getInstance().getBoolProperty(PROP_USE_JDBC_FOR_INDEXLIST, false))
		{
			return super.getIndexInfo(table, unique);
		}
		return getIndexInfo(table, null, null, unique);
	}

	@Override
	public boolean supportsIndexStatus()
	{
		return true;
	}

	@Override
	public boolean supportsTableSpaces()
	{
		// We can only return tablespace information when using our own SQL statement for the index retrieval.
		return !Settings.getInstance().getBoolProperty(PROP_USE_JDBC_FOR_INDEXLIST, false);
	}

	private ResultSet getIndexInfo(TableIdentifier table, String indexName, String indexSchema, boolean unique)
		throws SQLException
	{
		if (this.indexStatement != null)
		{
			LogMgr.logWarning("OracleIndexReader.getIndexInfo()", "getIndexInfo() called with pending results!");
			indexInfoProcessed();
		}

		// Views can't have indexes (only MATERIALIZED VIEWs)
		if ("VIEW".equals(table.getType())) return null;

		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.metaData.getWbConnection());

		boolean useUserTables = false;
		if (OracleUtils.optimizeCatalogQueries())
		{
			String schema = tbl.getRawSchema();
			if (StringUtil.isEmptyString(schema) || schema.equalsIgnoreCase(currentUser))
			{
				useUserTables = true;
			}
		}

		String sql =
      "-- SQL Workbench \n" +
			"SELECT " + OracleUtils.getCacheHint() + " null as table_cat, \n" +
			"       " + (useUserTables ? "user" : "i.owner") + " as table_schem, \n" +
			"       i.table_name, \n" +
			"       decode (i.uniqueness, 'UNIQUE', 0, 1) as non_unique, \n" +
			"       null as index_qualifier, \n" +
			"       i.index_name, \n"+
			"       i.index_type as type, \n" +
			"       c.column_position as ordinal_position, \n" +
			"       c.column_name, \n" +
			"       decode(c.descend, 'ASC', 'A', 'DESC', 'D', null) as asc_or_desc, \n" +
			"       i.distinct_keys as cardinality, \n" +
			"       i.leaf_blocks as pages, \n" +
			"       null as filter_condition, \n" +
			"       i.tablespace_name, \n" +
			"       i.status as index_status, \n" +
			"       i.partitioned, \n" +
			(hasCompression ?
			"       i.compression, \n" +
			"       i.prefix_length \n" :
			// no compression
			"       null as compression, \n" +
			"       -1 as prefix_length \n");

		if (useUserTables)
		{
			sql +=
			"FROM user_indexes i \n" +
			"  JOIN user_ind_columns c \n" +
			"    ON i.index_name = c.index_name \n" +
			"   AND i.table_name = c.table_name \n";
		}
		else
		{
			sql +=
			"FROM all_indexes i \n" +
			"  JOIN all_ind_columns c \n" +
			"    ON i.index_name = c.index_name \n" +
			"   AND i.table_owner = c.table_owner \n" +
			"   AND i.table_name = c.table_name \n" +
			"   AND i.owner = c.index_owner \n";
		}

		sql += "WHERE i.table_name = ? \n";

		if (tbl.getRawSchema() != null && !useUserTables)
		{
			sql += "  AND i.owner = ? \n";
		}

		if (unique)
		{
			sql += "  AND i.uniqueness = 'UNIQUE'\n";
		}

		if (StringUtil.isNonBlank(indexName))
		{
			sql += "  AND i.index_name = '" + indexName + "' \n";
		}

		if (StringUtil.isNonEmpty(indexSchema) && !useUserTables)
		{
			sql += "  AND i.owner = '" +  indexSchema + "' \n";
		}

		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.indexlist.filtersnapindex", true))
		{
			sql += "  AND i.index_name NOT LIKE 'I_SNAP$%' \n";
		}

		sql += "ORDER BY non_unique, type, index_name, ordinal_position ";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			String s = SqlUtil.replaceParameters(sql, tbl.getTableName(), tbl.getSchema());
			LogMgr.logDebug("OracleIndexReader.getIndexInfo()", "Retrieving index information using query:\n " + s);
		}

		this.indexStatement = this.metaData.getWbConnection().getSqlConnection().prepareStatement(sql);

		this.indexStatement.setString(1,table.getRawTableName());
		if (table.getSchema() != null && !useUserTables) this.indexStatement.setString(2, table.getRawSchema());
		ResultSet rs = this.indexStatement.executeQuery();
		return rs;
	}

	@Override
	protected void processIndexResultRow(ResultSet rs, IndexDefinition index, TableIdentifier tbl)
		throws SQLException
	{
		if (Settings.getInstance().getBoolProperty(PROP_USE_JDBC_FOR_INDEXLIST, false)) return;
		String tblSpace = rs.getString("TABLESPACE_NAME");
		if (hasCompression)
		{
			String compressed = rs.getString("COMPRESSION");
			int compressLevel = rs.getInt("PREFIX_LENGTH");
			if ("ENABLED".equals(compressed) && compressLevel > 0)
			{
				ObjectSourceOptions options = index.getSourceOptions();
				options.addConfigSetting(IDX_PROP_COMPRESS, Integer.toString(compressLevel));
			}
		}
		index.setTablespace(tblSpace);
		String status = rs.getString("INDEX_STATUS");
		index.setStatus(status);

    String isPartitioned = rs.getString("partitioned");
    index.setPartitioned(StringUtil.equalString("YES", isPartitioned));
	}

	public IndexDefinition getIndexDefinition(TableIdentifier table, String indexName, String indexSchema)
		throws SQLException
	{
		ResultSet rs = null;
		IndexDefinition index = null;
		try
		{
			rs = getIndexInfo(table, indexName, indexSchema, false);

			PkDefinition pkIndex = table.getPrimaryKey();
			if (pkIndex == null)
			{
				pkIndex = getPrimaryKey(table);
			}
			List<IndexDefinition> result = processIndexResult(rs, pkIndex, table);
			if (result.isEmpty())
			{
				return null;
			}
			if (result.size() > 1)
			{
				LogMgr.logError("OracleIndexReader.getIndexDefinition()", "Got more than one index for indexName= " + indexName + " and table=" + table.toString(), null);
			}
			index = result.get(0);
		}
		finally
		{
			SqlUtil.closeResult(rs);
			indexInfoProcessed(); // close the statement
		}
		return index;
	}

	@Override
	protected StringBuilder processCreateIndexTemplate(TableIdentifier table, IndexDefinition indexDefinition)
	{
		StringBuilder baseSource = super.processCreateIndexTemplate(table, indexDefinition);
		CharSequence partitionSource = getPartitionDefinition(indexDefinition, "   ");
		if (partitionSource == null) return baseSource;
		StringBuilder sql = new StringBuilder(baseSource.length() + partitionSource.length() + 5);
		sql.append(SqlUtil.trimSemicolon(baseSource.toString()));
		sql.append('\n');
		sql.append(partitionSource);
		sql.append(";\n");
		return sql;
	}

  public static Set<String> getUseDbmsMetaForTypes()
  {
    List<String> types = Settings.getInstance().getListProperty("workbench.db.oracle.index.types.usedbmsmetadata", false, "DOMAIN");
    Set<String> result = CollectionUtil.caseInsensitiveSet();
    result.addAll(types);
    return result;
  }

	@Override
	public CharSequence getIndexSource(TableIdentifier table, IndexDefinition definition)
	{
		if (definition == null) return null;

		boolean useDbmsMeta = OracleUtils.getUseOracleDBMSMeta(OracleUtils.DbmsMetadataTypes.index);

    Set<String> typesForMeta = getUseDbmsMetaForTypes();

		if (useDbmsMeta || typesForMeta.contains(definition.getIndexType()))
		{
			try
			{
				return getSourceFromDBMSMeta(table, definition);
			}
			catch (SQLException e)
			{
				// already logged
			}
		}

		return super.getIndexSource(table, definition);
	}

	private String getSourceFromDBMSMeta(TableIdentifier table, IndexDefinition definition)
		throws SQLException
	{
    String indexDDL = DbmsMetadata.getDDL(metaData.getWbConnection(), "INDEX", definition.getObjectName(), definition.getSchema());
    boolean inlinePK = metaData.getDbSettings().createInlinePKConstraints();
    if (!inlinePK && definition.isUniqueConstraint())
    {
      String constraint = DbmsMetadata.getDependentDDL(metaData.getWbConnection(), "CONSTRAINT", table.getTableName(), table.getSchema());
      if (StringUtil.isNonBlank(constraint))
      {
        indexDDL += "\n\n" + constraint;
      }
    }
    return indexDDL;
	}

	@Override
	public String getIndexOptions(TableIdentifier table, IndexDefinition index)
	{
		String option = "";
		if (OracleUtils.shouldAppendTablespace(index.getTablespace(), defaultTablespace, index.getSchema(), metaData.getWbConnection().getCurrentUser()))
		{
			option = "\n   TABLESPACE " + index.getTablespace();
		}

		if ("NORMAL/REV".equals(index.getIndexType()))
		{
			option += "\n    REVERSE";
		}

		String level = index.getSourceOptions().getConfigSettings().get(IDX_PROP_COMPRESS);
		if (level != null)
		{
			option += "\n   COMPRESS " + level;
		}
		return option;
	}

	/**
	 * 	Read the definition for function based indexes into the Map provided.
	 *
	 * 	The map should contain the names of the indexes as keys, and an List
	 * 	as elements. Each Element of the list is one part (=function call to a column)
	 * 	of the index definition.
	 */
	@Override
	public void processIndexList(Collection<IndexDefinition> indexDefs)
	{
		if (CollectionUtil.isEmpty(indexDefs)) return;

		String base=
      "-- SQL Workbench \n" +
			"SELECT " + OracleUtils.getCacheHint() + "i.index_name, e.column_expression, e.column_position \n" +
			"FROM all_indexes i \n" +
			"  JOIN all_ind_expressions e \n" +
			"    ON i.index_name = e.index_name \n" +
			"   AND i.owner = e.index_owner \n" +
			"   AND i.table_name = e.table_name \n" +
			"   AND e.index_owner = i.owner \n " +
			"WHERE i.index_type like 'FUNCTION-BASED%' ";

		StringBuilder sql = new StringBuilder(300);
		sql.append(base);

		boolean found = false;

		sql.append(" AND (i.owner, i.index_name) IN (");
		for (IndexDefinition def : indexDefs)
		{
			String type = def.getIndexType();
			if (type == null) continue;
			if (type.startsWith("FUNCTION-BASED"))
			{
				if (found) sql.append(',');
				found = true;
				sql.append(" (");
				String schema = StringUtil.coalesce(def.getSchema(), def.getBaseTable().getSchema());
				if (schema == null)
				{
					schema = "USER";
				}
				else
				{
					schema = "'" + metaData.removeQuotes(schema) + "'";
				}
				sql.append(schema);
				sql.append(",'");
				sql.append(metaData.removeQuotes(def.getName()));
				sql.append("')");
			}
		}
		sql.append(" ) \n");
		sql.append(" ORDER BY 1,3");

		if (!found) return;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleIndexReader.processIndexList()", "Using SQL to enhance index info:\n" + sql.toString());
		}

		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			stmt = this.metaData.getWbConnection().createStatementForQuery();
			rs = stmt.executeQuery(sql.toString());
			while (rs.next())
			{
				String name = rs.getString(1);
				String exp = rs.getString(2);
				int position = rs.getInt(3);

				IndexDefinition def = findIndex(indexDefs, name);
				if (def == null) continue;

				List<IndexColumn> indexCols = def.getColumns();
				if (position >= 0 && position <= indexCols.size())
				{
					// List is zero-based, the column positions are 1-based
					IndexColumn col = indexCols.get(position - 1);
					col.setColumn(SqlUtil.removeObjectQuotes(exp));
          col.setIsExpression(true);
				}

				String type = def.getIndexType();
				if (type.startsWith("FUNCTION-BASED"))
				{
					def.setIndexType(type.replace("FUNCTION-BASED ", ""));
				}
				else if (type.indexOf(' ') > -1 || type.indexOf('-') > -1)
				{
					def.setIndexType(DbSettings.IDX_TYPE_NORMAL);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("OracleMetaData.processIndexList()", "Error reading function-based index definition", e);
			LogMgr.logDebug("OracleMetaData.processIndexList()", "Using sql: "  + sql.toString());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private IndexDefinition findIndex(Collection<IndexDefinition> indexes, String indexName)
	{
		for (IndexDefinition def : indexes)
		{
			if (def.getName().equals(indexName)) return def;
		}
		return null;
	}

	public CharSequence getPartitionDefinition(IndexDefinition def, String indent)
	{
		if (!def.isPartitioned()) return null;

		WbConnection conn = this.metaData.getWbConnection();
		try
		{
			OracleIndexPartition partIndex = new OracleIndexPartition(conn);
			partIndex.retrieve(def, conn);
			if (partIndex.isPartitioned())
			{
				return partIndex.getSourceForIndexDefinition(indent);
			}
		}
		catch (SQLException sql)
		{
			LogMgr.logError("OracleIndexReader.getPartitionedIndexSource()", "Error reading partition definition", sql);
		}
		return null;
	}

	/**
	 * This method retrieves the name of the PK index used for that table.
	 *
	 * As there is no such function in JDBC api JdbcIndexReader uses getPrimaryKeys() to retrieve the name
	 * of the index as in most cases the PK name is the same as the supporting index.
	 *
	 * But in Oracle its possible to create a PK that is supported by an existing index and thus those two names
	 * do not need to be identical.
	 *
	 * Therefor the usage of getPrimaryKey() is disabled by default and replaced with our own statement.
	 *
	 * @param catalog  the table's catalog (ignored)
	 * @param schema   the table's schema
	 * @param table    the tablename
	 *
	 * @return the name of the index supporting the primary key
	 */
	@Override
	protected ResultSet getPrimaryKeyInfo(String catalog, String schema, String table)
		throws SQLException
	{
		boolean useJdbcForPk = Settings.getInstance().getBoolProperty(PROP_USE_JDBC_FOR_PK_INFO, false);

		if (useJdbcForPk)
		{
			return super.getPrimaryKeyInfo(catalog, schema, table);
		}

		String sql =
      "-- SQL Workbench \n" +
			"select " + OracleUtils.getCacheHint() + "null as table_cat,  \n" +
			"       cols.owner as table_schem,  \n" +
			"       cols.table_name,  \n" +
			"       cols.column_name,  \n" +
			"       cols.position as key_seq,  \n" +
			"       cons.constraint_name as pk_name,  \n" +
			"       cons.index_name as pk_index_name,  \n" +
			"       cons.status as pk_status \n" +
			"from all_cons_columns cols  \n" +
			"  join all_constraints cons on cols.constraint_name = cons.constraint_name and cols.owner = cons.owner  \n" +
			"where cons.constraint_type = 'P' \n" +
			" and cons.owner = ? \n" +
			" and cons.table_name = ? ";

		if (OracleUtils.optimizeCatalogQueries())
		{
			if (StringUtil.isEmptyString(schema) || schema.equalsIgnoreCase(currentUser))
			{
				sql = sql.replace(" all_co", " user_co");
			}
		}

		if (pkStament != null)
		{
			LogMgr.logDebug("OracleIndexReader.getPrimaryKeyInfo()", "getPrimeryKeys() called with pending statement!");
			primaryKeysResultDone();
		}

		if (schema == null)
		{
			schema = this.metaData.getWbConnection().getCurrentSchema();
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleIndexReader.getPrimaryKeyInfo()", "Retrieving PK information using:\n" + SqlUtil.replaceParameters(sql, schema, table));
		}

		pkStament = metaData.getSqlConnection().prepareStatement(sql);
		pkStament.setString(1, schema);
		pkStament.setString(2, table);
		return pkStament.executeQuery();
	}

	@Override
	protected void primaryKeysResultDone()
	{
		SqlUtil.closeStatement(pkStament);
		pkStament = null;
	}

	@Override
	protected Boolean isStatusEnabled(String status)
	{
		if (status == null) return null;
		return ("ENABLED".equals(status));
	}

}
