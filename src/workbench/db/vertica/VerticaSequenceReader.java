/*
 * VerticaSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.vertica;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author  Thomas Kellerer
 */
public class VerticaSequenceReader
	implements SequenceReader
{
	private WbConnection dbConnection;
	private final String baseSql =
              "select current_database as sequence_catalog, \n" +
						  "       sequence_schema,  \n" +
              "       sequence_name, \n" +
              "       minimum, \n" +
              "       maximum, \n" +
              "       increment_by, \n" +
              "       session_cache_count, \n" +
              "       allow_cycle \n" +
              "from v_catalog.sequences";

	public VerticaSequenceReader(WbConnection conn)
	{
		this.dbConnection = conn;
	}

	@Override
	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;

		StringBuilder buf = new StringBuilder(250);

		try
		{
			String name = def.getSequenceName();
			Long maxO = (Long) def.getSequenceProperty("MAXVALUE");
			Long minO = (Long) def.getSequenceProperty("MINVALUE");
			Long incO = (Long) def.getSequenceProperty("INCREMENT");
			Long cacheO = (Long) def.getSequenceProperty("CACHE");
			Boolean cycle = (Boolean) def.getSequenceProperty("CYCLE");
			if (cycle == null) cycle = Boolean.FALSE;

			long max = (maxO == null ? Long.MAX_VALUE : maxO.longValue());
			long min = (minO == null ? 1 : minO.longValue());
			long inc = (incO == null ? 1 : incO.longValue());
			long cache = (cacheO == null ? 250000 : cacheO.longValue());

			buf.append("CREATE SEQUENCE ");
			buf.append(name);
			buf.append("\n       INCREMENT BY ");
			buf.append(inc);

			buf.append("\n       MINVALUE ");
			buf.append(min);

			if (max != Long.MAX_VALUE)
			{
				buf.append("\n       MAXVALUE ");
				buf.append(Long.toString(max));
			}

			if (cache != 250000)
			{
				buf.append("\n       CACHE ");
				buf.append(cache);
			}
			buf.append("\n       ");
			if (!cycle.booleanValue())
			{
				buf.append("NO");
			}
			buf.append(" CYCLE");
			buf.append(";\n");

			if (StringUtil.isNonBlank(def.getComment()))
			{
				buf.append('\n');
				buf.append("COMMENT ON SEQUENCE ").append(def.getSequenceName()).append(" IS '").append(def.getComment().replace("'", "''")).append("';");
			}

		}
		catch (Exception e)
		{
			LogMgr.logError("VerticaSequenceReader.getSequenceSource()", "Error reading sequence definition", e);
		}

		def.setSource(buf);
	}

	/**
	 *	Return the source SQL for a PostgreSQL sequence definition.
	 *
	 *	@return The SQL to recreate the given sequence
	 */
	@Override
	public CharSequence getSequenceSource(String catalog, String schema, String aSequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, schema, aSequence);
		return def.getSource();
	}

	/**
	 * Retrieve the list of full SequenceDefinitions from the database.
	 */
	@Override
	public List<SequenceDefinition> getSequences(String catalog, String schema, String namePattern)
	{
		List<SequenceDefinition> result = new ArrayList<>();

		ResultSet rs = null;
		Statement stmt = null;
		Savepoint sp = null;
		if (namePattern == null) namePattern = "%";

		String sql = buildSql(schema, null);
		try
		{
			stmt = dbConnection.createStatementForQuery();

			rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, dbConnection, true);
			for (int row=0; row < ds.getRowCount(); row++)
			{
				result.add(createDefinition(ds, row));
			}
			this.dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			this.dbConnection.rollback(sp);
			LogMgr.logError("VerticaSequenceReader.getSequences()", "Error retrieving sequences using sql:\n" + sql, e);
			return result;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private SequenceDefinition createDefinition(DataStore ds, int row)
	{
		String catalog = ds.getValueAsString(row, "sequence_catalog");
		String name = ds.getValueAsString(row, "sequence_name");
		String schema = ds.getValueAsString(row, "sequence_schema");
		SequenceDefinition def = new SequenceDefinition(catalog, schema, name);
		def.setSequenceProperty("INCREMENT", ds.getValue(0, "increment_by"));
		def.setSequenceProperty("MAXVALUE", ds.getValue(0, "maximum"));
		def.setSequenceProperty("MINVALUE", ds.getValue(0, "minimum"));
		def.setSequenceProperty("CACHE", ds.getValue(0, "session_cache_count"));
		def.setSequenceProperty("CYCLE", ds.getValue(0, "allow_cycle"));
		return def;
	}

	private SequenceDefinition retrieveSequenceDetails(String catalog, String schema, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(catalog, schema, sequence);
		if (ds.getRowCount() == 0)
		{
			return null;
		}
		SequenceDefinition result = createDefinition(ds, 0);
		readSequenceSource(result);
		return result;
	}

	@Override
	public SequenceDefinition getSequenceDefinition(String catalog, String schema, String sequence)
	{
		return retrieveSequenceDetails(catalog, schema, sequence);
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String schema, String sequence)
	{
		if (sequence == null) return null;

		DataStore result = null;
		try
		{
			String sql = buildSql(schema, sequence);
			result = SqlUtil.getResultData(dbConnection, sql, true);
		}
		catch (SQLException e)
		{
			LogMgr.logDebug("VerticaSequenceReader.getSequenceDefinition()", "Error reading sequence definition", e);
		}
		return result;
	}

	private String buildSql(String schema, String sequence)
	{
		StringBuilder sql = new StringBuilder(100);
		sql.append(baseSql);

		if (schema != null || sequence != null)
		{
			sql.append(" WHERE ");
			boolean needAnd = false;

			if (sequence != null)
			{
				SqlUtil.appendExpression(sql, "sequence_name", sequence, dbConnection);
				needAnd = true;
			}

			if (needAnd)
			{
				SqlUtil.appendAndCondition(sql, "sequence_schema", schema, dbConnection);
			}
			else
			{
				SqlUtil.appendExpression(sql, "sequence_schema", schema, dbConnection);
				needAnd = true;
			}
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("VerticaSequenceReader.buildSql()", "Using SQL=\n" + sql);
			}
		}
		return sql.toString();
	}

	@Override
	public String getSequenceTypeName()
	{
		return SequenceReader.DEFAULT_TYPE_NAME;
	}
}
