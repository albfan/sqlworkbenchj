/*
 * DerbySequenceReader.java
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
package workbench.db.derby;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
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
 * SequenceReader for Apache Derby 10.6
 *
 * @author  Thomas Kellerer
 */
public class DerbySequenceReader
	implements SequenceReader
{
	private WbConnection dbConn;
	private String baseQuery;

	public DerbySequenceReader(WbConnection conn)
	{
		dbConn = conn;
		baseQuery = "SELECT sch.schemaname,  \n" +
             "       seq.sequencename, \n" +
             "       seq.sequencedatatype, \n" +
             "       seq.currentvalue, \n" +
             "       seq.startvalue, \n" +
             "       seq.minimumvalue, \n" +
             "       seq.maximumvalue, \n" +
             "       seq.increment, \n" +
             "       seq.cycleoption \n" +
             "FROM sys.syssequences seq \n" +
             "  JOIN sys.sysschemas sch ON sch.schemaid = seq.schemaid";
	}

	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null) return Collections.emptyList();
		List<SequenceDefinition> result = new ArrayList<>(ds.getRowCount());
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
		String name = ds.getValueAsString(row, "sequencename");
		String schema = ds.getValueAsString(row, "schemaname");
		SequenceDefinition result = new SequenceDefinition(schema, name);
		result.setSequenceProperty(PROP_START_VALUE, ds.getValue(row, "startvalue"));
		result.setSequenceProperty(PROP_MIN_VALUE, ds.getValue(row, "minimumvalue"));
		result.setSequenceProperty(PROP_MAX_VALUE, ds.getValue(row, "maximumvalue"));
		result.setSequenceProperty(PROP_INCREMENT, ds.getValue(row, "increment"));
		result.setSequenceProperty(PROP_CYCLE, Boolean.valueOf(StringUtil.stringToBool(ds.getValueAsString(row, "cycleoption"))));
		result.setSequenceProperty(PROP_DATA_TYPE, ds.getValueAsString(row, "sequencedatatype"));
		readSequenceSource(result);
		return result;
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String schema, String namePattern)
	{
		StringBuilder sql = new StringBuilder(baseQuery.length() + 50);
		sql.append(baseQuery);

		boolean whereAdded = false;
		if (StringUtil.isNonBlank(schema))
		{
			sql.append(" WHERE sch.schemaname = '").append(schema).append('\'');
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
			SqlUtil.appendExpression(sql, "seq.sequencename", namePattern, null);
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DerbySequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
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
			LogMgr.logError("DerbySequenceReader.getRawSequenceDefinition()", "Error when retrieving sequence definition", e);
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

		Number start = (Number) def.getSequenceProperty(PROP_START_VALUE);
		Number minvalue = (Number) def.getSequenceProperty(PROP_MIN_VALUE);
		Number maxvalue = (Number) def.getSequenceProperty(PROP_MAX_VALUE);
		Number increment = (Number) def.getSequenceProperty(PROP_INCREMENT);
		Boolean cycle = (Boolean) def.getSequenceProperty(PROP_CYCLE);
		String type = (String) def.getSequenceProperty(PROP_DATA_TYPE);

    result.append(" AS ");
		result.append(type);
		result.append(buildSequenceDetails(type, start, minvalue, maxvalue, increment, cycle));

		result.append(';');
		result.append(nl);
		def.setSource(result);
	}

	private CharSequence buildSequenceDetails(String type, Number start, Number minvalue, Number maxvalue, Number increment, boolean cycle)
	{
		StringBuilder result = new StringBuilder(30);
		String nl = Settings.getInstance().getInternalEditorLineEnding();

		String indent = nl + "       ";
		if (start.longValue() > 0)
		{
			result.append(indent);
			result.append("START WITH ");
			result.append(start);
		}

		result.append(indent);
		result.append(" INCREMENT BY ");
		result.append(increment);

		result.append(indent);

		boolean hasMinValue = false;
		if ("integer".equalsIgnoreCase(type))
		{
			hasMinValue = minvalue != null && minvalue.intValue() != Integer.MIN_VALUE;
		}
		else if ("bigint".equalsIgnoreCase(type))
		{
			hasMinValue = minvalue != null && minvalue.longValue() != Long.MIN_VALUE;
		}

		if (hasMinValue)
		{
			result.append(" MINVALUE ");
			result.append(minvalue);
		}
		else
		{
			result.append(" NO MINVALUE");
		}

		result.append(indent);
		if (maxvalue != null && maxvalue.longValue() == -1)
		{
			if (maxvalue.longValue() != Long.MAX_VALUE)
			{
				result.append(" MAXVALUE ");
				result.append(maxvalue);
			}
		}
		else
		{
			result.append(" NO MAXVALUE");
		}

		result.append(indent);
		if (cycle)
		{
			result.append(" CYCLE");
		}
		else
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
