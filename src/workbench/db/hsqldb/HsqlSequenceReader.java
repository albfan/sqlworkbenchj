/*
 * HsqlSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.db.JdbcUtils;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * SequenceReader for <a href="http://www.hsqldb.org">HSQLDB</a>
 *
 * @author  Thomas Kellerer
 */
public class HsqlSequenceReader
	implements SequenceReader
{
	private Connection dbConn;
	private String baseQuery;
	private boolean supportsColumnSequence;

	public HsqlSequenceReader(Connection conn)
	{
		this.dbConn = conn;
		String query = "SELECT sequence_schema, " +
						"sequence_name, " +
						"dtd_identifier as data_type, " +
						"maximum_value, " +
						"minimum_value, " +
						"increment, " +
						"cycle_option, " +
						"start_with " +
						"FROM ";

		if (JdbcUtils.hasMinimumServerVersion(conn, "2.0"))
		{
			query += "information_schema.sequences";
			query = query.replace("dtd_identifier as data_type", "data_type");
		}
		else
		{
			query += "information_schema.system_sequences";
		}
		baseQuery = query;
		supportsColumnSequence = JdbcUtils.hasMinimumServerVersion(conn, "2.1.1");
	}

	@Override
	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		CharSequence s = getSequenceSource(def.getCatalog(), def.getSequenceOwner(), def.getSequenceName());
		def.setSource(s);
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String schema, String namePattern)
	{
		StringBuilder query = new StringBuilder(baseQuery.length() + 20);
		query.append(baseQuery);
		boolean whereAdded = false;

		if (StringUtil.isNonBlank(namePattern))
		{
			whereAdded = true;
			query.append(" WHERE sequence_name LIKE '");
			query.append(StringUtil.trimQuotes(namePattern));
			query.append("' ");
		}

		if (StringUtil.isNonBlank(schema))
		{
			if (!whereAdded)
			{
				query.append(" WHERE ");
			}
			else
			{
				query.append(" AND ");
			}
			SqlUtil.appendExpression(query, "sequence_schema", StringUtil.trimQuotes(schema));
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("HsqlSequenceReader.getRawSequenceDefinition()", "Using query=" + query);
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.dbConn.prepareStatement(query.toString());
			rs = stmt.executeQuery();
			result = new DataStore(rs, true);
		}
		catch (Throwable e)
		{
			LogMgr.logError("HsqlSequenceReader.getSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}


	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null) return Collections.emptyList();

		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();

		for (int row = 0; row < ds.getRowCount(); row++)
		{
			result.add(createSequenceDefinition(ds, row));
		}
		return result;
	}

	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		SequenceDefinition result = null;

    if (ds == null || ds.getRowCount() == 0) return null;

		String name = ds.getValueAsString(row, "SEQUENCE_NAME");
		String schema = ds.getValueAsString(row, "SEQUENCE_SCHEMA");
		result = new SequenceDefinition(schema, name);

		result.setSequenceProperty("START_WITH", ds.getValue(row, "START_WITH"));
		result.setSequenceProperty("MAXIMUM_VALUE", ds.getValue(row, "MAXIMUM_VALUE"));
		result.setSequenceProperty("MINIMUM_VALUE", ds.getValue(row, "MINIMUM_VALUE"));
		result.setSequenceProperty("INCREMENT", ds.getValue(row, "INCREMENT"));
		result.setSequenceProperty("CYCLE_OPTION", ds.getValue(row, "CYCLE_OPTION"));
		result.setSequenceProperty("DATA_TYPE", ds.getValue(row, "DATA_TYPE"));
		readRelatedTable(result); // must be called before buildSource is called!

		result.setSource(buildSource(result));
		return result;
	}

	@Override
	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
		if (ds == null) return null;
		return createSequenceDefinition(ds, 0);
	}

	@Override
	public CharSequence getSequenceSource(String catalog, String owner, String sequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, owner, sequence);
		return buildSource(def);
	}

	protected CharSequence buildSource(SequenceDefinition def)
	{
		if (def == null) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(100);
		result.append("CREATE SEQUENCE ");
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append(def.getSequenceName());
		String type = (String)def.getSequenceProperty("DATA_TYPE");

		if (!"INTEGER".equals(type))
		{
			result.append(" AS ");
			result.append(type);
		}

		// For some reason HSQLDB returns all properties as String objects, even the numeric ones!
		String start = (String)def.getSequenceProperty("START_WITH");
		result.append(nl);
		result.append("       START WITH ");
		result.append(start);

		String inc = (String)def.getSequenceProperty("INCREMENT");
		result.append(nl);
		result.append("       INCREMENT BY ");
		result.append(inc);
		result.append(';');
		result.append(nl);
		if (def.getRelatedTable() != null)
		{
			result.append(nl);
			result.append("-- Sequence for: ");
			result.append(def.getRelatedTable().getTableExpression());
			result.append('.');
			result.append(def.getRelatedColumn());
			result.append(nl);
		}

		return result;
	}

	private void readRelatedTable(SequenceDefinition def)
	{
		if (def == null) return;
		if (!supportsColumnSequence) return;

		String sql =
				"SELECT table_catalog, table_schema, table_name, column_name \n" +
				"FROM information_schema.system_column_sequence_usage \n" +
				"WHERE sequence_schema = ? \n " +
				"  AND sequence_name = ? ";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try
		{
			pstmt = dbConn.prepareStatement(sql);
			pstmt.setString(1, def.getSchema());
			pstmt.setString(2, def.getSequenceName());

			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String cat = rs.getString(1);
				String schema = rs.getString(2);
				String name = rs.getString(3);
				String col = rs.getString(4);
				if (StringUtil.isNonEmpty(name) && StringUtil.isNonEmpty(col))
				{
					def.setRelatedTable(new TableIdentifier(cat, schema, name), col);
				}
			}
		}
		catch (SQLException e)
		{
			supportsColumnSequence = false;
			LogMgr.logError("HsqlSequenceReader.getOwnedByClause()", "Error retrieving sequence column", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
	}
}
