/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.mssql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;
import workbench.db.sqltemplates.TemplateHandler;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerIndexReader
	extends JdbcIndexReader
{
	public static final String CLUSTERED_PLACEHOLDER = "%clustered_attribute%";
	private boolean checkIncludedColumns;

	public SqlServerIndexReader(DbMetadata meta)
	{
		super(meta);
		checkIncludedColumns = SqlServerUtil.isSqlServer2005(meta.getWbConnection());
	}

	@Override
	protected String getUniqueConstraint(TableIdentifier table, IndexDefinition indexDefinition)
	{
		String sql = super.getUniqueConstraint(table, indexDefinition);
		return replaceClustered(sql, indexDefinition);
	}

	@Override
	public String getSQLKeywordForType(String type)
	{
		if (type == null) return "";
		if (type.equals("NORMAL")) return "NONCLUSTERED";
		return type;
	}

	@Override
	public String getIndexOptions(TableIdentifier table, IndexDefinition index)
	{
		if (!checkIncludedColumns) return null;
		List<String> cols = getIncludedColumns(table, index);
		if (cols.isEmpty()) return null;

		StringBuilder sql = new StringBuilder(cols.size() * 20);
		sql.append("\n   INCLUDE (");
		for (int i=0; i < cols.size(); i++)
		{
			if (i > 0) sql.append(", ");
			sql.append(cols.get(i));
		}
		sql.append(')');
		return sql.toString();

	}
	private List<String> getIncludedColumns(TableIdentifier table, IndexDefinition index)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		String sql =
			"select col.name \n" +
			"from sys.index_columns ic with (nolock) \n" +
			"  join sys.columns col with (nolock) on col.object_id = ic.object_id and col.column_id = ic.column_id \n" +
			"  join sys.indexes ix with (nolock) on ix.index_id = ic.index_id and ix.object_id = col.object_id \n" +
			"  join sys.all_objects ao with (nolock) on ao.object_id = ix.object_id \n" +
			"  join sys.schemas sh with (nolock) on sh.schema_id = ao.schema_id \n" +
			"where ix.name = ? \n" +
			"  and ao.name = ? \n" +
			"  and sh.name = ? \n" +
			"  and ic.is_included_column = 1 \n " +
			"order by ic.index_column_id";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("SqlServerIndexReader.getIncludedColumns()", "Using SQL=\n" +
				SqlUtil.replaceParameters(sql, index.getName(), table.getRawTableName(), table.getRawSchema()));
		}

		List<String> result = new ArrayList<String>();
		try
		{
			pstmt = this.metaData.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, index.getName());
			pstmt.setString(2, table.getRawTableName());
			pstmt.setString(3, table.getRawSchema());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logWarning("SqlServerIndexReader.getIncludedColumns()", "Could not read included columns", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result;
	}

	private String replaceClustered(String sql, IndexDefinition indexDefinition)
	{
		if (StringUtil.isEmptyString(sql)) return sql;
		if (indexDefinition == null) return sql;

		String type = indexDefinition.getIndexType();
		String clustered = "CLUSTERED";
		if ("NORMAL".equals(type))
		{
			clustered = "NONCLUSTERED";
		}

		if (StringUtil.isBlank(type))
		{
			sql = TemplateHandler.removePlaceholder(sql, CLUSTERED_PLACEHOLDER, true);
		}
		else
		{
			sql = TemplateHandler.replacePlaceholder(sql, CLUSTERED_PLACEHOLDER, clustered);
		}
		return sql;
	}

	@Override
	public boolean supportsIndexList()
	{
		return SqlServerUtil.isSqlServer2005(metaData.getWbConnection());
	}

	@Override
	public List<IndexDefinition> getIndexes(String catalog, String schema)
	{
		if (SqlServerUtil.isSqlServer2005(metaData.getWbConnection()))
		{
			return super.getIndexes(catalog, schema);
		}
		return Collections.emptyList();
	}
	
}
