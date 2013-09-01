/*
 * SqlServerSequenceReader.java
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
package workbench.db.mssql;


import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A sequence reader for SQL Server 2012
 *
 * @author  Thomas Kellerer
 */
public class SqlServerSequenceReader
  implements SequenceReader
{
  private WbConnection connection;
	private Set<String> noPrecisionTypes = CollectionUtil.caseInsensitiveSet("tinyint", "smallint", "int", "bigint");

  public SqlServerSequenceReader(WbConnection conn)
  {
    this.connection = conn;
  }

	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null || ds.getRowCount() == 0) return Collections.emptyList();
		ArrayList<SequenceDefinition> result = new ArrayList<SequenceDefinition>();
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
  public DataStore getRawSequenceDefinition(String catalog, String schema, String sequence)
  {
		StringBuilder sql = new StringBuilder(100);
		sql.append(
			"SELECT db_name() as sequence_catalog,   \n" +
			"       sc.name as sequence_schema,  \n" +
			"       sq.name as sequence_name,         \n" +
			"       cast(sq.minimum_value as bigint) as minimum_value,         \n" +
			"       cast(sq.maximum_value as bigint) as maximum_value,         \n" +
			"       cast(sq.start_value as bigint) as start_value,         \n" +
			"       cast(sq.increment as bigint) as increment,         \n" +
			"       case when is_cycling = 1 then 'CYCLE' else 'NO CYCLE' end as cycle_flag,  \n" +
			"       sq.is_cached,  \n" +
			"       sq.cache_size,  \n" +
			"       type_name(sq.system_type_id) as data_type,  \n" +
			"       type_name(sq.user_type_id) as user_type,  \n" +
			"       sq.precision,  \n" +
			"       cast(sq.current_value  as bigint) as current_value \n" +
			"FROM sys.sequences sq with (nolock)  \n" +
			"   join sys.schemas sc with (nolock) on sq.schema_id = sc.schema_id " +
			"WHERE sc.name = '");
		sql.append(SqlUtil.removeObjectQuotes(schema));
		sql.append("' ");

		if (StringUtil.isNonEmpty(sequence))
		{
			SqlUtil.appendAndCondition(sql, "sq.name", sequence, connection);
		}
		sql.append("\n ORDER BY 1,2");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("SqlServerSequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
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
		if (def.getSource() == null)
		{
			readSequenceSource(def);
		}
		return def.getSource();
	}

	private SequenceDefinition createDefinition(DataStore ds, int row)
	{
		if (ds == null || row >= ds.getRowCount()) return null;
		String name = ds.getValueAsString(row, "SEQUENCE_NAME");
		String schema = ds.getValueAsString(row, "SEQUENCE_SCHEMA");
		String db = ds.getValueAsString(row, "SEQUENCE_CATALOG");
		SequenceDefinition result = new SequenceDefinition(schema, name);
		result.setCatalog(db);
		result.setSequenceProperty("minimum_value", ds.getValue(row, "minimum_value"));
		result.setSequenceProperty("maximum_value", ds.getValue(row, "maximum_value"));
		result.setSequenceProperty("start_value", ds.getValue(row, "start_value"));
		result.setSequenceProperty("increment", ds.getValue(row, "increment"));
		result.setSequenceProperty("cycle_flag", ds.getValue(row, "cycle_flag"));
		result.setSequenceProperty("is_cached", ds.getValue(row, "is_cached"));
		result.setSequenceProperty("cache_size", ds.getValue(row, "cache_size"));
		result.setSequenceProperty("current_value", ds.getValue(row, "current_value"));
		result.setSequenceProperty("data_type", ds.getValue(row, "data_type"));
		result.setSequenceProperty("user_type", ds.getValue(row, "user_type"));
		result.setSequenceProperty("precision", ds.getValue(row, "precision"));
		return result;
	}


	private BigDecimal getNumberValue(SequenceDefinition def, String property)
	{
		Object v = def.getSequenceProperty(property);
		if (v == null) return null;
		try
		{
			BigDecimal d = new BigDecimal(v.toString());
			return d;
		}
		catch (NumberFormatException nfe)
		{
			LogMgr.logWarning("SqlServerSequenceReader.getNumberValue()", "Could not convert " + v + " to a number", nfe);
		}
		return null;
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

		Number minValue = getNumberValue(def, "minimum_value");
		Number maxValue = getNumberValue(def, "maximum_value");
		Number startValue = getNumberValue(def, "start_value");

		Number increment = getNumberValue(def, "increment");

		String cycle = (String) def.getSequenceProperty("cycle_flag");
		Boolean isCached = (Boolean)def.getSequenceProperty("is_cached");
		Number cache = (Number) def.getSequenceProperty("cache_size");
		Number precision = (Number)def.getSequenceProperty("precision");

		String type = (String)def.getSequenceProperty("data_type");
		String userType = (String)def.getSequenceProperty("data_type");
		String typeToUse = type;
		if (!type.equals(userType))
		{
			typeToUse = userType;
		}

		result.append(nl).append("       AS ");
		result.append(typeToUse);
		if (needsPrecision(typeToUse) && precision != null)
		{
			result.append('(');
			result.append(precision.toString());
			result.append(')');
		}

		result.append(nl).append("       INCREMENT BY ");
		result.append(increment);

		if (minValue != null && isMinValue(typeToUse, minValue))
		{
			result.append(nl).append("       MINVALUE ");
			result.append(minValue);
		}
		else
		{
			result.append(nl).append("       NO MINVALUE");
		}

		if (maxValue != null && isMaxValue(typeToUse, maxValue))
		{
			result.append(nl).append("       MAXVALUE ");
			result.append(maxValue);
		}
		else
		{
			result.append(nl).append("       NO MAXVALUE");
		}

		if (startValue != null)
		{
			if (minValue != null && !startValue.equals(minValue) || minValue == null)
			{
				result.append(nl).append("       START WITH ");
				result.append(startValue);
			}
		}

		if (Boolean.TRUE.equals(isCached))
		{
			result.append(nl).append("       CACHE ");
			if (cache != null && cache.longValue() > 0) result.append(cache);
		}
		else
		{
			result.append(nl).append("       NOCACHE");
		}

		result.append(nl).append("       ");
		result.append(cycle);

		result.append(';');
		result.append(nl);

		def.setSource(result);
	}

	private boolean isMaxValue(String type, Number maxValue)
	{
		if (maxValue == null) return true;
		if ("bigint".equals(type))
		{
			return maxValue.toString().equals("9223372036854776000");
		}
		if ("tinyint".equals(type))
		{
			return maxValue.intValue() == 255;
		}
		if ("smallint".equals(type))
		{
			return maxValue.longValue() == 32767;
		}
		if ("int".equals(type))
		{
			return maxValue.longValue() == 2147483647;
		}
		return false;
	}

	private boolean isMinValue(String type, Number minValue)
	{
		if (minValue == null) return true;
		if ("bigint".equals(type))
		{
			return minValue.toString().equals("-9223372036854776000");
		}
		if ("tinyint".equals(type))
		{
			return minValue.intValue() == 0;
		}
		if ("smallint".equals(type))
		{
			return minValue.longValue() == -32768;
		}
		if ("int".equals(type))
		{
			return minValue.longValue() == -2147483648;
		}
		return false;
	}

	private boolean needsPrecision(String type)
	{
		if (noPrecisionTypes.contains(type)) return false;
		return true;
	}

	@Override
	public String getSequenceTypeName()
	{
		return SequenceReader.DEFAULT_TYPE_NAME;
	}
}
