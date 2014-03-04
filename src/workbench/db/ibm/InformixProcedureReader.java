/*
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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.JdbcUtils;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * IBM's JDBC driver for Informix does not handle overloaded procedures correctly.
 *
 * @author Thomas Kellerer
 */
public class InformixProcedureReader
	extends JdbcProcedureReader
{
	private boolean fixRetrieval;
	private Map<String, Integer> typeMap;

	public InformixProcedureReader(WbConnection conn)
	{
		super(conn);
		fixRetrieval = JdbcUtils.hasMinimumServerVersion(conn, "11.0");
	}

	@Override
	public DataStore getProcedures(String catalog, String schemaPattern, String namePattern)
		throws SQLException
	{
		if (!fixRetrieval)
		{
			return super.getProcedures(catalog, schemaPattern, namePattern);
		}

		catalog = DbMetadata.cleanupWildcards(catalog);
		schemaPattern = DbMetadata.cleanupWildcards(schemaPattern);
		namePattern = DbMetadata.cleanupWildcards(namePattern);

		Statement stmt = null;
		ResultSet rs = null;
		String sql = getSQL(schemaPattern, namePattern);
		DataStore ds = buildProcedureListDataStore(this.connection.getMetadata(), false);
		try
		{
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("InformixProcedureReader.getProcedures()", "Query to retrieve procedurelist:\n" + sql);
			}
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String schema = rs.getString("PROCEDURE_SCHEM");
				String name = rs.getString("PROCEDURE_NAME");
				String specificName = rs.getString("SPECIFIC_NAME");
				String args = rs.getString("parameter_types");
				String procid = rs.getString("procid");

				int type = rs.getInt("PROCEDURE_TYPE");
				int row = ds.addRow();

				ProcedureDefinition def = new ProcedureDefinition(null, schema, name, type);

				List<String> argTypes = StringUtil.stringToList(args, ",", true, true);
				List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();

				for (int i=0; i < argTypes.size(); i++)
				{
					String argName = "input" + Integer.toString(i + 1);
					ColumnIdentifier col = new ColumnIdentifier(argName);
					col.setDbmsType(argTypes.get(i));
					col.setDataType(getJdbcType(argTypes.get(i)));
					cols.add(col);
				}
				def.setParameters(cols);
				def.setSpecificName(specificName);
				def.setInternalIdentifier(procid);
				def.setDisplayName(name + "(" + args + ")");
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, null);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, def.getDisplayName());
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, type);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, null);
				ds.getRow(row).setUserObject(def);
			}

			return ds;
		}
		catch (Exception e)
		{
			LogMgr.logError("InformixProcedureReader.getProcedures()", "Error retrieving procedures using query:\n" + sql, e);
			fixRetrieval = false;
			return super.getProcedures(catalog, schemaPattern, namePattern);
		}
		finally
		{
			// The resultSet is already closed by fillProcedureListDataStore
			SqlUtil.closeStatement(stmt);
		}
	}

	private String getSQL(String schemaPattern, String namePattern)
	{
		StringBuilder sql = new StringBuilder(100);

		sql.append(
			"SELECT '' as PROCEDURE_CAT,  \n" +
			"       owner as PROCEDURE_SCHEM, \n" +
			"       procname as PROCEDURE_NAME, \n" +
			"       '' as remarks, \n" +
			"       CASE  \n" +
			"         WHEN isproc = 'f' THEN " + DatabaseMetaData.procedureReturnsResult + "  \n" +
			"         ELSE " + DatabaseMetaData.procedureNoResult + "  \n" +
			"       END as PROCEDURE_TYPE, \n" +
			"       specificname as specific_name, \n" +
			"       procid::LVARCHAR as procid, \n" +
			"       paramtypes::LVARCHAR as parameter_types \n" +
			"FROM sysprocedures \n" +
			"WHERE internal = 'f' \n " +
			"  AND mode IN ('D', 'd', 'O', 'o') ");

		SqlUtil.appendAndCondition(sql, "owner", schemaPattern, this.connection);
		SqlUtil.appendAndCondition(sql, "procname", namePattern, this.connection);
		return sql.toString();
	}

	@Override
	public DataStore getProcedureColumns(ProcedureDefinition def)
		throws SQLException
	{
		if (def.getParameterNames().isEmpty())
		{
			return super.getProcedureColumns(def);
		}
		DataStore ds = createProcColsDataStore();
		List<ColumnIdentifier> parameters = def.getParameters(null);
		for (ColumnIdentifier col : parameters)
		{
			int row = ds.addRow();
			ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, col.getColumnName());

		}
		return ds;
	}

	private int getJdbcType(String typeName)
	{
		String baseType = SqlUtil.getBaseTypeName(typeName);
		Integer jdbc = getJavaTypeMapping().get(baseType.toLowerCase());
		if (jdbc == null) return Types.OTHER;
		return jdbc.intValue();
	}

	private Map<String, Integer> getJavaTypeMapping()
	{
		if (typeMap == null)
		{
			typeMap = new HashMap<String, Integer>();
			typeMap.put("smallint", Integer.valueOf(Types.SMALLINT));
			typeMap.put("byte", Integer.valueOf(Types.SMALLINT));
			typeMap.put("integer", Integer.valueOf(Types.INTEGER));
			typeMap.put("int8", Integer.valueOf(Types.BIGINT));
			typeMap.put("int", Integer.valueOf(Types.INTEGER));
			typeMap.put("serial", Integer.valueOf(Types.INTEGER));
			typeMap.put("serial8", Integer.valueOf(Types.BIGINT));
			typeMap.put("bigserial", Integer.valueOf(Types.BIGINT));
			typeMap.put("money", Integer.valueOf(Types.DOUBLE));
			typeMap.put("numeric", Integer.valueOf(Types.NUMERIC));
			typeMap.put("decimal", Integer.valueOf(Types.DECIMAL));
			typeMap.put("dec", Integer.valueOf(Types.DECIMAL));
			typeMap.put("smallfloat", Integer.valueOf(Types.FLOAT));
			typeMap.put("double precision", Integer.valueOf(Types.FLOAT));
			typeMap.put("float", Integer.valueOf(Types.DOUBLE));
			typeMap.put("smallfloat", Integer.valueOf(Types.FLOAT));
			typeMap.put("char", Integer.valueOf(Types.CHAR));
			typeMap.put("character", Integer.valueOf(Types.CHAR));
			typeMap.put("character varying", Integer.valueOf(Types.VARCHAR));
			typeMap.put("nchar", Integer.valueOf(Types.NCHAR));
			typeMap.put("varchar", Integer.valueOf(Types.VARCHAR));
			typeMap.put("lvarchar", Integer.valueOf(Types.VARCHAR));
			typeMap.put("nvarchar", Integer.valueOf(Types.NVARCHAR));
			typeMap.put("text", Integer.valueOf(Types.CLOB));
			typeMap.put("boolean", Integer.valueOf(Types.BOOLEAN));
			typeMap.put("bit", Integer.valueOf(Types.BIT));
			typeMap.put("blob", Integer.valueOf(Types.BLOB));
			typeMap.put("clob", Integer.valueOf(Types.CLOB));
			typeMap.put("date", Integer.valueOf(Types.DATE));
			typeMap.put("datetime", Integer.valueOf(Types.TIMESTAMP));
			typeMap.put("time", Integer.valueOf(Types.TIME));
			typeMap.put("timetz", Integer.valueOf(Types.TIME));
			typeMap.put("timestamp", Integer.valueOf(Types.TIMESTAMP));
			typeMap.put("timestamptz", Integer.valueOf(Types.TIMESTAMP));
    }
		return typeMap;
	}
}
