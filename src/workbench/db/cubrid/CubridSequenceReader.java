/*
 * CubridSequenceReader.java
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
package workbench.db.cubrid;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
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

/**
 * @author  Thomas Kellerer
 */
public class CubridSequenceReader
	implements SequenceReader
{
	private WbConnection dbConnection;
	private final String baseSql =
			"SELECT name,  \n" +
			"       current_val,  \n" +
			"       increment_val as increment,  \n" +
			"       case when max_val = 10000000000000000000000000000000000000 then null else max_val end as maxvalue,  \n" +
			"       min_val as minvalue,  \n" +
			"       case when cyclic = 0 then 'NOCYCLE' else 'CYCLE' end as cycle_flag, \n" +
			"       cached_num as cache \n" +
			"FROM db_serial \n";

	public CubridSequenceReader(WbConnection conn)
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
			BigDecimal max = (BigDecimal) def.getSequenceProperty("MAXVALUE");
			BigDecimal min = (BigDecimal) def.getSequenceProperty("MINVALUE");
			BigDecimal inc = (BigDecimal) def.getSequenceProperty("INCREMENT");
			Integer cache = (Integer) def.getSequenceProperty("CACHE");
			String cycle = (String)def.getSequenceProperty("CYCLE");

			buf.append("CREATE SERIAL ");
			buf.append(name);
			buf.append("\n       INCREMENT BY ");
			buf.append(inc);
			buf.append("\n       MINVALUE ");
			buf.append(min);
			if (max != null)
			{
				buf.append("\n       MAXVALUE ");
				buf.append(max.toString());
			}
			buf.append("\n       CACHE ");
			buf.append(cache);
			if (cycle != null)
			{
				buf.append("\n       ");
				buf.append(cycle);
			}
			buf.append(";\n");

		}
		catch (Exception e)
		{
			LogMgr.logError("CubridSequenceReader.getSequenceSource()", "Error reading sequence definition", e);
		}

		def.setSource(buf);
	}

	/**
	 *	Return the source SQL for a Cubrid SERIAL (sequence) definition.
	 *
	 *	@return The SQL to recreate the given sequence
	 */
	@Override
	public CharSequence getSequenceSource(String catalog, String owner, String aSequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, owner, aSequence);
		readSequenceSource(def);
		return def.getSource();
	}

	/**
	 * Retrieve the list of full SequenceDefinitions from the database.
	 */
	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		List<SequenceDefinition> result = new ArrayList<>();

		StringBuilder sql = new StringBuilder(baseSql.length() + 20);
		sql.append(baseSql);
		if (namePattern != null)
		{
			sql.append(" WHERE ");
		}
		SqlUtil.appendExpression(sql, "name", namePattern, dbConnection);

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("CubridSequenceReader.getSequences()", "Query to retrieve sequences:\n" + sql.toString());
		}

		try
		{
			DataStore ds = SqlUtil.getResultData(dbConnection, sql.toString(), false);
			for (int row=0; row < ds.getRowCount(); row ++)
			{
				result.add(createDefinition(ds, row));
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("CubridSequenceReader.getSequences()", "Error retrieving sequences", e);
			return null;
		}
		return result;
	}

	private SequenceDefinition createDefinition(DataStore ds, int row)
		throws SQLException
	{
		String name = ds.getValueAsString(row, "name");
		SequenceDefinition def = new SequenceDefinition(null, name);
		def.setObjectTypeName(getSequenceTypeName());
		def.setSequenceProperty("INCREMENT", ds.getValue(row, "increment"));
		def.setSequenceProperty("MAXVALUE", ds.getValue(row, "maxvalue"));
		def.setSequenceProperty("MINVALUE", ds.getValue(row, "minvalue"));
		def.setSequenceProperty("CACHE", ds.getValue(row, "cache"));
		def.setSequenceProperty("CYCLE", ds.getValue(row, "cycle_flag"));
		return def;
	}

	@Override
	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
		List<SequenceDefinition> seq = getSequences(catalog, owner, sequence);
		if (seq.size() > 0)
		{
			return seq.get(0);
		}
		return null;
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String owner, String sequence)
	{
		if (sequence == null) return null;


		StringBuilder sql = new StringBuilder(baseSql.length() + 20);
		sql.append(baseSql);
		sql.append(" WHERE name = '" + sequence + "'");

		ResultSet rs = null;
		Statement stmt = null;
		DataStore result = null;
		try
		{
			stmt = dbConnection.createStatementForQuery();
			rs = stmt.executeQuery(sql.toString());
			result = new DataStore(rs, true);
		}
		catch (SQLException e)
		{
			LogMgr.logError("CubridSequenceReader.getRawSequenceDefinition()", "Error retrieving sequences using SQL:\n" + sql, e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

	@Override
	public String getSequenceTypeName()
	{
		return "SERIAL";
	}

}
