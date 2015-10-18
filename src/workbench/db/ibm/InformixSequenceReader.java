/*
 * InformixSequenceReader.java
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
package workbench.db.ibm;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * SequenceReader for Informix
 *
 * @author  Thomas Kellerer
 */
public class InformixSequenceReader
	implements SequenceReader
{
	private WbConnection dbConn;

	public InformixSequenceReader(WbConnection conn)
	{
		dbConn = conn;
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
		String name = ds.getValueAsString(row, "sequence_name");
		String schema = ds.getValueAsString(row, "sequence_schema");
		SequenceDefinition result = new SequenceDefinition(schema, name);
		result.setSequenceProperty(PROP_START_VALUE, ds.getValue(row, "start_val"));
		result.setSequenceProperty(PROP_MIN_VALUE, ds.getValue(row, "min_val"));
		result.setSequenceProperty(PROP_MAX_VALUE, ds.getValue(row, "max_val"));
		result.setSequenceProperty(PROP_INCREMENT, ds.getValue(row, "inc_val"));
		result.setSequenceProperty(PROP_CYCLE, Boolean.valueOf(StringUtil.stringToBool(ds.getValueAsString(row, "cycle"))));
		result.setSequenceProperty(PROP_ORDERED, Boolean.valueOf(StringUtil.stringToBool(ds.getValueAsString(row, "order"))));
		result.setSequenceProperty(PROP_CACHE_SIZE, ds.getValue(row, "cache"));
		readSequenceSource(result);
		return result;
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String schema, String namePattern)
	{
		String systemSchema = dbConn.getDbSettings().getProperty("systemschema", "informix");
		TableIdentifier sysTabs = new TableIdentifier(catalog, systemSchema, "systables");
		TableIdentifier seqTabs = new TableIdentifier(catalog, systemSchema, "syssequences");

		String systables = sysTabs.getFullyQualifiedName(this.dbConn);
		String syssequences = seqTabs.getFullyQualifiedName(this.dbConn);

		StringBuilder sql = new StringBuilder(100);
		sql.append(
			"select trim(t.tabname) as sequence_name, \n" +
			"       trim(t.owner) as sequence_schema, \n" +
			"       seq.start_val, \n" +
			"       seq.inc_val, \n" +
			"       seq.min_val, \n" +
			"       seq.max_val, \n" +
			"       seq.cycle, \n" +
			"       seq.cache, \n" +
			"       seq.order \n" +
			" from ");
		sql.append(syssequences);
		sql.append(" seq \n   join ");
		sql.append(systables);
		sql.append(" t on seq.tabid = t.tabid");

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
			LogMgr.logInfo("InformixSequenceReader.getRawSequenceDefinition()", "Query to retrieve sequence:\n" + sql);
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
			LogMgr.logError("InformixSequenceReader.getRawSequenceDefinition()", "Could not retrieve sequence definition using SQL:\n" + sql, e);
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
		boolean cycle = (Boolean) def.getSequenceProperty(PROP_CYCLE);
		boolean order = (Boolean) def.getSequenceProperty(PROP_ORDERED);
		Number cache = (Number) def.getSequenceProperty(PROP_CACHE_SIZE);

		result.append(buildSequenceDetails(start, minvalue, maxvalue, increment, cycle, order, cache));

		result.append(';');
		result.append(nl);
		def.setSource(result);
	}

	private CharSequence buildSequenceDetails(Number start, Number minvalue, Number maxvalue, Number increment, boolean cycle, boolean order, Number cache)
	{
		StringBuilder result = new StringBuilder(30);
		String nl = Settings.getInstance().getInternalEditorLineEnding();

		if (start != null && start.longValue() > 0)
		{
			result.append(nl + "       ");
			result.append("START WITH ");
			result.append(start);
		}

		result.append(nl + "      ");
		result.append(" INCREMENT BY ");
		result.append(increment);

		result.append(nl + "      ");

		boolean hasMinValue = minvalue != null && minvalue.longValue() != 1;

		if (hasMinValue)
		{
			result.append(" MINVALUE ");
			result.append(minvalue);
		}
		else
		{
			result.append(" NOMINVALUE");
		}

		result.append(nl + "      ");
		boolean hasMaxValue = maxvalue != null && maxvalue.longValue() != Long.MAX_VALUE;
		if (hasMaxValue)
		{
			result.append(" MAXVALUE ");
			result.append(maxvalue);
		}
		else
		{
			result.append(" NOMAXVALUE");
		}

		result.append(nl + "      ");
		if (cycle)
		{
			result.append(" CYCLE");
		}
		else
		{
			result.append(" NOCYCLE");
		}

		result.append(nl + "      ");
		if (order)
		{
			result.append(" ORDER");
		}
		else
		{
			result.append(" NOORDER");
		}

		result.append(nl + "      ");
		if (cache != null && cache.longValue() > 0)
		{
			result.append(" CACHE ");
			result.append(cache.toString());
		}
		else
		{
			result.append(" NOCACHE");
		}


		return result;
	}

	@Override
	public String getSequenceTypeName()
	{
		return SequenceReader.DEFAULT_TYPE_NAME;
	}
}
