/*
 * OracleSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.db.JdbcUtils;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class OracleSequenceReader
  implements SequenceReader
{
  private WbConnection connection;
	private boolean is12c;

  public OracleSequenceReader(WbConnection conn)
  {
    this.connection = conn;
		this.is12c = JdbcUtils.hasMinimumServerVersion(connection, "12.1");
  }

	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null || ds.getRowCount() == 0) return Collections.emptyList();
		ArrayList<SequenceDefinition> result = new ArrayList<>();
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			result.add(createDefinition(ds, row));
		}
		return result;
	}

	@Override
	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
		if (ds == null || ds.getRowCount() == 0) return null;
		SequenceDefinition def = createDefinition(ds, 0);
		return def;
	}

	@Override
  public DataStore getRawSequenceDefinition(String catalog, String owner, String sequence)
  {
		StringBuilder sql = new StringBuilder(100);
		sql.append(
			"SELECT SEQUENCE_OWNER, \n" +
			"       SEQUENCE_NAME, \n" +
			"       MIN_VALUE, \n" +
			"       MAX_VALUE, \n" +
			"       INCREMENT_BY, \n" +
			"       CASE WHEN CYCLE_FLAG = 'Y' then 'CYCLE' ELSE 'NOCYCLE' END AS CYCLE_FLAG, \n" +
			"       CASE WHEN ORDER_FLAG = 'Y' then 'ORDER' ELSE 'NOORDER' END AS ORDER_FLAG, \n" +
			"       CACHE_SIZE, \n" +
			"       LAST_NUMBER \n" +
			"FROM ALL_SEQUENCES \n" +
			"WHERE sequence_owner = '");

		sql.append(StringUtil.trimQuotes(owner));
		sql.append("'\n ");

		if (is12c && connection.getDbSettings().hideOracleIdentitySequences())
		{
			sql.append("  AND sequence_name NOT LIKE 'ISEQ$$%'"); // remove Oracle 12c sequences used for identity columns
		}

		if (StringUtil.isNonEmpty(sequence))
		{
			SqlUtil.appendAndCondition(sql, "sequence_name", sequence, connection);
		}
		sql.append("\nORDER BY 1,2");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("OracleSequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
		}

    Statement stmt = null;
    ResultSet rs = null;
    DataStore result = null;
    try
    {
      stmt = this.connection.createStatement();
      rs = stmt.executeQuery(sql.toString());
      result = new DataStore(rs, this.connection, true);
    }
    catch (Exception e)
    {
      LogMgr.logError("OracleMetaData.getSequenceDefinition()", "Error when retrieving sequence definition", e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    return result;
  }

	@Override
  public CharSequence getSequenceSource(String catalog, String owner, String sequence)
  {
		SequenceDefinition def = getSequenceDefinition(catalog, owner, sequence);
		if (def == null) return null;
		return def.getSource();
	}

	private SequenceDefinition createDefinition(DataStore ds, int row)
	{
		if (ds == null || row >= ds.getRowCount()) return null;
		String name = ds.getValueAsString(row, "SEQUENCE_NAME");
		String owner = ds.getValueAsString(row, "SEQUENCE_OWNER");
		SequenceDefinition result = new SequenceDefinition(owner, name);
		result.setSequenceProperty(PROP_MIN_VALUE, ds.getValue(row, "MIN_VALUE"));
		result.setSequenceProperty(PROP_MAX_VALUE, ds.getValue(row, "MAX_VALUE"));
		result.setSequenceProperty(PROP_INCREMENT, ds.getValue(row, "INCREMENT_BY"));
		result.setSequenceProperty(PROP_CYCLE, Boolean.toString("CYCLE".equalsIgnoreCase(ds.getValueAsString(row, "CYCLE_FLAG"))));
		result.setSequenceProperty(PROP_CACHE, ds.getValue(row, "CACHE_SIZE"));
		result.setSequenceProperty(PROP_ORDERED, Boolean.toString("ORDER".equalsIgnoreCase(ds.getValueAsString(row, "ORDER_FLAG"))));
		readSequenceSource(result);
		return result;
	}


	@Override
	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		if (def.getSource() != null) return;

		StringBuilder result = new StringBuilder(100);
		String nl = Settings.getInstance().getInternalEditorLineEnding();

		result.append("CREATE SEQUENCE ");
		result.append(def.getSequenceName());

		Number minValue = (Number) def.getSequenceProperty(PROP_MIN_VALUE);
		Number maxValue = (Number) def.getSequenceProperty(PROP_MAX_VALUE);

		Number increment = (Number) def.getSequenceProperty(PROP_INCREMENT);

		String cycle = (String) def.getSequenceProperty(PROP_CYCLE);
		String order = (String) def.getSequenceProperty(PROP_ORDERED);
		Number cache = (Number) def.getSequenceProperty(PROP_CACHE);

		result.append(nl).append("       INCREMENT BY ");
		result.append(increment);

		if (minValue != null && minValue.intValue() != 1)
		{
			result.append(nl).append("       MINVALUE ");
			result.append(minValue);
		}
		else
		{
			result.append(nl).append("       NOMINVALUE");
		}

		if (maxValue != null && !maxValue.toString().startsWith("999999999999999999999999999"))
		{
			result.append(nl).append("       MAXVALUE ");
			result.append(maxValue);
		}
		else
		{
			result.append(nl).append("       NOMAXVALUE");
		}

		if (cache != null && cache.longValue() > 0)
		{
			result.append(nl).append("       CACHE ");
			result.append(cache);
		}
		else
		{
			result.append(nl).append("       NOCACHE");
		}
		result.append(nl).append("       ");
		result.append(Boolean.getBoolean(cycle) ? "CYCLE" : "NOCYCLE");

		result.append(nl).append("       ");
		result.append(Boolean.getBoolean(order) ? "ORDER" : "NOORDER");

		result.append(';');
		result.append(nl);

		def.setSource(result);
	}

	@Override
	public String getSequenceTypeName()
	{
		return SequenceReader.DEFAULT_TYPE_NAME;
	}
}
