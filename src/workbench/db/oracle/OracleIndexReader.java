/*
 * OracleIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An implementation of the IndexReader interface for Oracle.
 *
 * This class uses its own SQL Statement to retrieve the index list from the database
 * as Oracle's JDBC driver runs an ANALYZE before actually returning the index information
 *
 * @author Thomas Kellerer
 */
public class OracleIndexReader
	extends JdbcIndexReader
{
	private PreparedStatement indexStatement;

	public OracleIndexReader(DbMetadata meta)
	{
		super(meta);
	}

	public void indexInfoProcessed()
	{
		SqlUtil.closeStatement(this.indexStatement);
		this.indexStatement = null;
	}

	/**
	 * Replacement for the DatabaseMetaData.getIndexInfo() method.
	 * Oracle's JDBC driver does an ANALYZE INDEX each time an indexInfo is
	 * requested which slows down the retrieval of index information.
	 * (and is not necessary at all for the Workbench, as we don't use the
	 * cardinality field anyway)
	 * <br/>
	 * Additionally function based indexes are not returned correctly by the
	 * Oracle driver.
	 */
	public ResultSet getIndexInfo(TableIdentifier table, boolean unique)
		throws SQLException
	{
		if (this.indexStatement != null)
		{
			LogMgr.logWarning("OracleIndexReader.getIndexInfo()", "getIndexInfo() called with pending results!");
			indexInfoProcessed();
		}

		if ("VIEW".equals(table.getType())) return null;

		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.metaData.getWbConnection());

		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT null as table_cat, \n" +
			"       i.owner as table_schem, \n" +
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
			"       i.index_type \n" +
			"FROM all_indexes i, all_ind_columns c \n" +
			"WHERE i.table_name = ? \n");

		if (tbl.getSchema() != null)
		{
			sql.append("  AND i.owner = ? \n");
		}
		if (unique)
		{
			sql.append("  and i.uniqueness = 'UNIQUE'\n");
		}
		sql.append("  and i.index_name = c.index_name \n" +
			"  and i.table_owner = c.table_owner \n" +
			"  and i.table_name = c.table_name \n" +
			"  and i.owner = c.index_owner \n");
		sql.append("ORDER BY non_unique, type, index_name, ordinal_position ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleIndexReader.getIndexInfo()", "Using SQL to retrieve index info for " + table.getTableExpression() + ":\n" + sql.toString());
		}
		this.indexStatement = this.metaData.getWbConnection().getSqlConnection().prepareStatement(sql.toString());

		this.indexStatement.setString(1,table.getTableName());
		if (table.getSchema() != null) this.indexStatement.setString(2, table.getSchema());
		ResultSet rs = this.indexStatement.executeQuery();
		return rs;
	}

	@Override
	public String getIndexSourceForType(TableIdentifier table, IndexDefinition definition)
	{
		if (definition == null) return null;

		boolean alwaysUseDbmsMeta = this.metaData.getDbSettings().getUseOracleDBMSMeta("index");

		if (!alwaysUseDbmsMeta && !"DOMAIN".equals(definition.getIndexType())) return null;

		PreparedStatement stmt = null;
		ResultSet rs = null;
		String source = null;

		String sql = "select dbms_metadata.get_ddl('INDEX', ?, ?) from dual";

		try
		{
			stmt = this.metaData.getSqlConnection().prepareStatement(sql);

			stmt.setString(1, definition.getObjectName());
			stmt.setString(2, definition.getSchema());
			
			rs = stmt.executeQuery();
			if (rs.next())
			{
				source = rs.getString(1);
				if (source != null) source = source.trim();
				source += ";\n";
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleIndexReader", "Error retrieving index via DBMS_DDL", e);
			source = ExceptionUtil.getDisplay(e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}

	@Override
	public String getIndexOptions(IndexDefinition index)
	{
		if ("NORMAL/REV".equals(index.getIndexType())) return "\n    REVERSE";
		return null;
	}

	/**
	 * 	Read the definition for function based indexes into the Map provided.
	 * 	The map should contain the names of the indexes as keys, and an List
	 * 	as elements. Each Element of the list is one part (=function call to a column)
	 * 	of the index definition.
	 */
	public void processIndexList(TableIdentifier tbl, Collection<IndexDefinition> indexDefs)
	{
		if (CollectionUtil.isEmpty(indexDefs)) return;

		String base="SELECT i.index_name, e.column_expression, e.column_position \n" +
			"FROM all_indexes i, all_ind_expressions e  \n" +
			" WHERE i.index_name = e.index_name   \n" +
			"    and i.owner = e.index_owner   \n" +
			"    and i.table_name = e.table_name   \n" +
			"    and e.index_owner = i.owner \n " +
			"    and i.index_type like 'FUNCTION-BASED%' ";
		StringBuilder sql = new StringBuilder(300);
		sql.append(base);
		String schema = tbl.getSchema();

		if (schema != null && schema.length() > 0)
		{
			sql.append(" AND i.owner = '" + schema + "' ");
		}
		boolean found = false;

		sql.append(" AND i.index_name IN (");
		for (IndexDefinition def : indexDefs)
		{
			String type = def.getIndexType();
			if (type == null) continue;
			if (type.startsWith("FUNCTION-BASED"))
			{
				if (found) sql.append(',');
				found = true;
				sql.append('\'');
				sql.append(def.getName());
				sql.append('\'');
			}
		}
		sql.append(") \n");
		sql.append(" ORDER BY 1,3");

		if (!found) return;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleIndexReader.processIndexList()", "Using SQL to enhance index info for " + tbl.getTableExpression() + ":\n" + sql.toString());
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
					col.setColumn(StringUtil.trimQuotes(exp));
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
}
