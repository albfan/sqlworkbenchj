/*
 * Db2SequenceReader.java
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
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
 * A class to read sequence definitions from a DB2 database.
 *
 * @author  Thomas Kellerer
 */
public class Db2SequenceReader
	implements SequenceReader
{
	private WbConnection connection;
	private final String dbid;
	private boolean quoteKeyword;
	private char catalogSeparator;

	public Db2SequenceReader(WbConnection conn, String useId)
	{
		this.connection = conn;
		dbid = useId;
		catalogSeparator = conn.getMetadata().getCatalogSeparator();
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

	private String getDSValueString(DataStore ds, int row, String ... colNames)
	{
		Object o = getDSValue(ds, row, colNames);
		if (o == null) return null;
		return o.toString();
	}

	private Object getDSValue(DataStore ds, int row, String ... colNames)
	{
		if (colNames == null) return null;
		for (String col : colNames)
		{
			if (ds.getColumnIndex(col) > -1)
			{
				return ds.getValue(row, col);
			}
		}
		return null;
	}

	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		String name = getDSValueString(ds, row, "SEQNAME", "NAME", "SEQUENCE_NAME");
		String schema = getDSValueString(ds, row, "SEQUENCE_SCHEMA", "SEQSCHEMA", "SCHEMA");

		SequenceDefinition result = new SequenceDefinition(schema != null ? schema.trim() : null, name.trim());

		result.setSequenceProperty(PROP_START_VALUE, ds.getValue(row, "START"));
		result.setSequenceProperty(PROP_MIN_VALUE, getDSValue(ds, row, "MINVALUE", "MINIMUM_VALUE"));
		result.setSequenceProperty(PROP_MAX_VALUE, getDSValue(ds, row, "MAXVALUE", "MAXIMUM_VALUE"));
		result.setSequenceProperty(PROP_INCREMENT, ds.getValue(row, "INCREMENT"));
		result.setSequenceProperty(PROP_CYCLE, Boolean.toString(StringUtil.stringToBool(ds.getValueAsString(row, "CYCLE"))));
		result.setSequenceProperty(PROP_ORDERED, Boolean.toString(StringUtil.stringToBool(ds.getValueAsString(row, "ORDER"))));
		result.setSequenceProperty(PROP_CACHE_SIZE, ds.getValue(row, "CACHE"));
		if (ds.getColumnIndex("DATATYPEID") > -1)
		{
			result.setSequenceProperty("DATATYPEID", ds.getValue(row, "DATATYPEID"));
		}

		if (ds.getColumnIndex(PROP_DATA_TYPE) > -1)
		{
			result.setSequenceProperty("DATA_TYPE", ds.getValue(row, "DATA_TYPE"));
		}

		result.setComment(getDSValueString(ds, row, "REMARKS", "LONG_COMMENT"));
		readSequenceSource(result);
		return result;
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String schema, String namePattern)
	{
		StringBuilder query = new StringBuilder(100);

		int schemaIndex = -1;
		int nameIndex = -1;

		String nameCol;
		String schemaCol;

		if (dbid.equals("db2i"))
		{
			// Host system on AS/400
			query.append(
			"SELECT SEQUENCE_NAME, \n" +
			"       SEQUENCE_SCHEMA \n, " +
			"       0 as START, \n" +
			"       minimum_value as MINVALUE, \n" +
			"       maximum_value as MAXVALUE, \n" +
			"       INCREMENT, \n" +
			"       case cycle when 'YES' then 'Y' else 'N' end as CYCLE, \n" +
			"       case ORDER when 'YES' then 'Y' else 'N' end as ORDER, \n" +
			"       CACHE, \n" +
			"       data_type, \n" +
			"       long_comment as remarks \n" +
			"FROM   qsys2" + catalogSeparator + "syssequences \n");

			nameCol = "sequence_name";
			schemaCol = "sequence_schema";
		}
		else if (dbid.equals("db2h"))
		{
			// Host system on z/OS
			query.append(
			"SELECT NAME AS SEQNAME, \n" +
			"       SCHEMA AS SEQUENCE_SCHEMA, \n" +
			"       START, \n" +
			"       MINVALUE, \n" +
			"       MAXVALUE, \n" +
			"       INCREMENT, \n" +
			"       CYCLE, \n" +
			"       ORDER, \n" +
			"       CACHE, \n" +
			"       DATATYPEID, \n" +
			"       REMARKS \n" +
			"FROM   SYSIBM.SYSSEQUENCES \n");

			nameCol = "name";
			schemaCol = "schema";
		}
		else
		{
			// LUW Version
			query.append(
			"SELECT SEQNAME AS SEQUENCE_NAME, \n" +
			"       SEQSCHEMA as SEQUENCE_SCHEMA, \n" +
			"       START, \n" +
			"       MINVALUE, \n" +
			"       MAXVALUE, \n" +
			"       INCREMENT, \n" +
			"       CYCLE, \n" +
			"       ORDER, \n" +
			"       CACHE, \n" +
			"       DATATYPEID, \n" +
		  "       REMARKS  \n" +
			"FROM   syscat.sequences \n");

			nameCol = "seqname";
			schemaCol = "seqschema";
		}

		boolean whereAdded = false;

		if (StringUtil.isNonBlank(schema))
		{
			query.append(" WHERE " + schemaCol + " = ?");
			schemaIndex = 1;
			whereAdded = true;
		}

		if (StringUtil.isNonBlank(namePattern))
		{
			if (whereAdded)
			{
				query.append(" AND ");
				nameIndex = 2;
			}
			else
			{
				query.append(" WHERE ");
				nameIndex = 1;
			}
			if (namePattern.indexOf('%') > 0)
			{
				query.append(nameCol).append(" LIKE ? ");
				SqlUtil.appendEscapeClause(query, connection, namePattern);
				namePattern = SqlUtil.escapeUnderscore(namePattern, connection);
			}
			else
			{
				query.append(nameCol).append(" = ? ");
			}
		}

		String sql = query.toString();
		// Needed for the unit test (because in H2 order is a reserved word)
		if (quoteKeyword)
		{
			sql = sql.replace(" ORDER,", " \"ORDER\",");
			sql = sql.replace(" ORDER ", " \"ORDER\" ");
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("Db2SequenceReader.getRawSequenceDefinition()", "Query to retrieve sequence:\n" +
				SqlUtil.replaceParameters(sql, schema, namePattern));
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.connection.getSqlConnection().prepareStatement(sql);
			if (schemaIndex > -1)	stmt.setString(schemaIndex, schema);
			if (nameIndex > -1) stmt.setString(nameIndex, namePattern);
			rs = stmt.executeQuery();
			result = new DataStore(rs, this.connection, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleMetaData.getSequenceDefinition()", "Error when retrieving sequence definition", e);
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
		result.append(def.getObjectExpression(connection));

		Number start = (Number) def.getSequenceProperty(PROP_START_VALUE);
		Number minvalue = (Number) def.getSequenceProperty(PROP_MIN_VALUE);
		Number maxvalue = (Number) def.getSequenceProperty(PROP_MAX_VALUE);
		Number increment = (Number) def.getSequenceProperty(PROP_INCREMENT);
		boolean cycle = Boolean.parseBoolean((String) def.getSequenceProperty(PROP_CYCLE));
		boolean order = Boolean.parseBoolean((String) def.getSequenceProperty(PROP_ORDERED));
		Number cache = (Number) def.getSequenceProperty(PROP_CACHE_SIZE);
		Number typeid = (Number) def.getSequenceProperty("TYPEID");

		if (typeid != null)
		{
      result.append(" AS " + typeIdToName(typeid.intValue()));
		}
		else
		{
			Object oname = def.getSequenceProperty("DATA_TYPE");
			String typeName = (oname != null ? oname.toString() : null);
			if (typeName != null )
			{
				result.append(" AS ");
				result.append(typeName);
			}
		}

		result.append(buildSequenceDetails(true, start, minvalue, maxvalue, increment, cycle, order, cache));

		result.append(';');
		result.append(nl);

		if (StringUtil.isNonBlank(def.getComment()))
		{
			result.append("COMMENT ON SEQUENCE " + def.getSequenceName() + " IS '" + def.getComment().replace("'", "''") + "';");
			result.append(nl);
		}

		def.setSource(result);
	}

	public static CharSequence buildSequenceDetails(boolean doFormat, Number start, Number minvalue, Number maxvalue, Number increment, boolean cycle, boolean order, Number cache)
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
		if (minvalue == null || minvalue.longValue() == 0)
		{
			if (doFormat) result.append(" NO MINVALUE");
		}
		else
		{
			result.append(" MINVALUE ");
			result.append(minvalue);
		}

		if (doFormat) result.append(nl + "      ");
		if (maxvalue != null && maxvalue.longValue() == -1)
		{
			if (maxvalue.longValue() != Long.MAX_VALUE)
			{
				result.append(" MAXVALUE ");
				result.append(maxvalue);
			}
		}
		else if (doFormat)
		{
			result.append(" NO MAXVALUE");
		}

		if (doFormat) result.append(nl + "      ");
		if (cache != null && cache.longValue() > 0)
		{
			if (cache.longValue() != 20 || doFormat)
			{
				result.append(" CACHE ");
				result.append(cache);
			}
		}
		else if (doFormat)
		{
			result.append(" NO CACHE");
		}

		if (doFormat) result.append(nl + "      ");
		if (cycle)
		{
			result.append(" CYCLE");
		}
		else if (doFormat)
		{
			result.append(" NO CYCLE");
		}

		if (doFormat) result.append(nl + "      ");
		if (order)
		{
			result.append(" ORDER");
		}
		else if (doFormat)
		{
			result.append(" NO ORDER");
		}
		return result;
	}

	private String typeIdToName(int id)
	{
		switch (id)
		{
			case 20:
				return "BIGINT";
			case 28:
				return "SMALLINT";
			case 16:
				return "DECIMAL";
		}
		return "INTEGER";
	}

	void setQuoteKeyword(boolean flag)
	{
		quoteKeyword = flag;
	}

	@Override
	public String getSequenceTypeName()
	{
		return SequenceReader.DEFAULT_TYPE_NAME;
	}

}
