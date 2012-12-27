/*
 * InformixSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.ibm;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.WbConnection;


/**
 * SequenceReader for Apache Derby 10.6
 *
 * @author  Thomas Kellerer
 */
public class InformixSequenceReader
	implements SequenceReader
{
	private WbConnection dbConn;
	private final String baseQuery;

	public InformixSequenceReader(WbConnection conn)
	{
		dbConn = conn;
		baseQuery =
			"select trim(t.tabname) as sequence_name, \n" +
			"       trim(t.owner) as sequence_schema, \n" +
			"       seq.start_val, \n" +
			"       seq.inc_val, \n" +
			"       seq.min_val, \n" +
			"       seq.max_val, \n" +
			"       seq.cycle, \n" +
			"       seq.cache, \n" +
			"       seq.order \n" +
			" from syssequences seq \n" +
			"   join systables t on seq.tabid = t.tabid";
	}

	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null) return Collections.emptyList();
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>(ds.getRowCount());
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			result.add(createSequenceDefinition(ds, row));
		}
		return result;
	}

	@Override
	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
		if (ds == null || ds.getRowCount() != 1) return null;
		return createSequenceDefinition(ds, 0);
	}

	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		String name = ds.getValueAsString(row, "sequence_name");
		String schema = ds.getValueAsString(row, "sequence_schema");
		SequenceDefinition result = new SequenceDefinition(schema, name);
		result.setSequenceProperty("START", ds.getValue(row, "start_val"));
		result.setSequenceProperty("MINVALUE", ds.getValue(row, "min_val"));
		result.setSequenceProperty("MAXVALUE", ds.getValue(row, "max_val"));
		result.setSequenceProperty("INCREMENT", ds.getValue(row, "inc_val"));
		result.setSequenceProperty("CYCLE", ds.getValue(row, "cycle"));
		result.setSequenceProperty("ORDER", ds.getValue(row, "order"));
		readSequenceSource(result);
		return result;
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String schema, String namePattern)
	{
		StringBuilder sql = new StringBuilder(baseQuery.length() + 20);
		sql.append(baseQuery);

		boolean whereAdded = false;
		if (StringUtil.isNonBlank(schema))
		{
			sql.append(" WHERE t.owner = '" + schema + "'");
			whereAdded = true;
		}

		if (StringUtil.isNonBlank(namePattern))
		{
			if (whereAdded)
			{
				sql.append(" AND ");
			}
			else
			{
				sql.append(" WHERE ");
			}
			SqlUtil.appendExpression(sql, "t.tabname", namePattern, dbConn);
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("InformixSequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
		}

		Statement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = dbConn.createStatementForQuery();
			rs = stmt.executeQuery(sql.toString());
			result = new DataStore(rs, dbConn, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("InformixSequenceReader.getRawSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs,stmt);
		}

		return result;
	}

	@Override
	public CharSequence getSequenceSource(String catalog, String schema, String sequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, schema, sequence);
		if (def == null) return null;
		return def.getSource();
	}

	@Override
	public void readSequenceSource(SequenceDefinition def)
	{
		StringBuilder result = new StringBuilder(100);

		String nl = Settings.getInstance().getInternalEditorLineEnding();

		result.append("CREATE SEQUENCE ");
		result.append(def.getSequenceName());

		Number start = (Number) def.getSequenceProperty("START");
		Number minvalue = (Number) def.getSequenceProperty("MINVALUE");
		Number maxvalue = (Number) def.getSequenceProperty("MAXVALUE");
		Number increment = (Number) def.getSequenceProperty("INCREMENT");
		String cycle = (String) def.getSequenceProperty("CYCLE");
		String order = (String) def.getSequenceProperty("ORDER");
		Number cache = (Number) def.getSequenceProperty("CACHE");

		result.append(buildSequenceDetails(true, start, minvalue, maxvalue, increment, cycle, order, cache));

		result.append(';');
		result.append(nl);
		def.setSource(result);
	}

	public static CharSequence buildSequenceDetails(boolean doFormat, Number start, Number minvalue, Number maxvalue, Number increment, String cycle, String order, Number cache)
	{
		StringBuilder result = new StringBuilder(30);
		String nl = Settings.getInstance().getInternalEditorLineEnding();

		if (start != null && start.longValue() > 0)
		{
			if (doFormat) result.append(nl + "       ");
			result.append("START WITH ");
			result.append(start);
		}

		if (doFormat) result.append(nl + "      ");
		result.append(" INCREMENT BY ");
		result.append(increment);

		if (doFormat) result.append(nl + "      ");

		boolean hasMinValue = minvalue != null && minvalue.longValue() != 1;

		if (hasMinValue)
		{
			result.append(" MINVALUE ");
			result.append(minvalue);
		}
		else
		{
			if (doFormat) result.append(" NOMINVALUE");
		}

		if (doFormat) result.append(nl + "      ");
		boolean hasMaxValue = maxvalue != null && maxvalue.longValue() != Long.MAX_VALUE;
		if (hasMaxValue)
		{
			result.append(" MAXVALUE ");
			result.append(maxvalue);
		}
		else if (doFormat)
		{
			result.append(" NO MAXVALUE");
		}

		if (doFormat) result.append(nl + "      ");
		if (cycle != null && cycle.equals("1"))
		{
			result.append(" CYCLE");
		}
		else if (doFormat)
		{
			result.append(" NO CYCLE");
		}

		return result;
	}

	@Override
	public String getSequenceTypeName()
	{
		return SequenceReader.DEFAULT_TYPE_NAME;
	}
}
