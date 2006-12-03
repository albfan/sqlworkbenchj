/*
 * OracleIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;

import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class OracleIndexReader
	extends JdbcIndexReader
{
	private PreparedStatement indexStatement;
	
	/** Creates a new instance of OracleMetaData */
	public OracleIndexReader(DbMetadata meta)
	{
		super(meta);
	}
	
	public void indexInfoProcessed()
	{
		try 
		{ 
			this.indexStatement.close(); 
			this.indexStatement = null;
		} 
		catch (Throwable th) 
		{
		}
	}
	
	/**
	 * 	Replacement for the DatabaseMetaData.getIndexInfo() method.
	 * 	Oracle's JDBC driver does an ANALYZE INDEX each time an indexInfo is
	 * 	requested which slows down the retrieval of index information.
	 *  (and is not necessary at all for the Workbench, as we don't use the
	 *  cardinality field anyway)
	 */
	public ResultSet getIndexInfo(TableIdentifier table, boolean unique)
		throws SQLException
	{
		if (this.indexStatement != null)
		{
			LogMgr.logWarning("OracleIndexReader.getIndexInfo()", "getIndexInfo() called with pending results!");
			indexInfoProcessed();
		}
		
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.metaData.getWbConnection());
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT null as table_cat, " +
			"       i.owner as table_schem, " +
			"       i.table_name, "+
			"       decode (i.uniqueness, 'UNIQUE', 0, 1) as non_unique, " +
			"       null as index_qualifier, " +
			"       i.index_name, "+
			"       1 as type, " +
			"       c.column_position as ordinal_position, " +
			"       c.column_name, " +
			"       null as asc_or_desc, " +
			"       i.distinct_keys as cardinality, " +
			"       i.leaf_blocks as pages, " +
			"       null as filter_condition, " +
			"       i.index_type " +
			"FROM all_indexes i, all_ind_columns c " +
			"WHERE i.table_name = ? \n");
		
		if (tbl.getSchema() != null)
		{
			sql.append("  AND i.owner = ? \n");
		}
		if (unique)
		{
			sql.append("  and i.uniqueness = 'UNIQUE'\n");
		}
		sql.append("  and i.index_name = c.index_name " +
			"  and i.table_owner = c.table_owner " +
			"  and i.table_name = c.table_name " +
			"  and i.owner = c.index_owner ");
		sql.append("ORDER BY non_unique, type, index_name, ordinal_position ");
		
		this.indexStatement = this.metaData.getWbConnection().getSqlConnection().prepareStatement(sql.toString());
		this.indexStatement.setString(1,table.getTableName());
		if (table.getSchema() != null) this.indexStatement.setString(2, table.getSchema());
		ResultSet rs = this.indexStatement.executeQuery();
		return rs;
	}
	
	/**
	 * 	Read the definition for function based indexes into the Map provided.
	 * 	The map should contain the names of the indexes as keys, and an List
	 * 	as elements. Each Element of the list is one part (=function call to a column)
	 * 	of the index definition.
	 */
	public void processIndexList(TableIdentifier tbl, Collection indexDefs)
	{
		if (indexDefs.size() == 0) return;
		
		Map result = new HashMap();
		
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
		Iterator keys = indexDefs.iterator();
		boolean found = false;
		
		sql.append(" AND i.index_name IN (");
		while (keys.hasNext())
		{
			IndexDefinition def = (IndexDefinition)keys.next();
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
		sql.append(") ");
		
		if (!found) return;
		
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
				result.put(name, exp);
			}
			
			keys = indexDefs.iterator();
			while (keys.hasNext())
			{
				IndexDefinition def = (IndexDefinition)keys.next();
				String exp = (String)result.get(def.getName());
				if (exp != null)
				{
					def.setExpression(exp);
				}
				String type = def.getIndexType();
				if (type.startsWith("FUNCTION-BASED"))
				{
					def.setIndexType(type.replace("FUNCTION-BASED ", ""));
				}
				else if (type.indexOf(' ') > -1 || type.indexOf('-') > -1)
				{
					def.setIndexType(DbMetadata.IDX_TYPE_NORMAL);
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
		return;
	}

}
