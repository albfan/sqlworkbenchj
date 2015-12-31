/*
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
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * IBM's JDBC driver for Informix does not handle overloaded procedures correctly.
 *
 * The SQL statements are only tested against an Informix 11.5
 *
 * @author Thomas Kellerer
 */
public class InformixProcedureReader
	extends JdbcProcedureReader
{
	private boolean fixProcRetrieval;
	private boolean fixParamsRetrieval;
	private Map<String, Integer> typeMap;

	public InformixProcedureReader(WbConnection conn)
	{
		super(conn);
		boolean useDriver = connection.getDbSettings().getBoolProperty("procedurelist.usedriver", false);
		if (useDriver)
		{
			fixProcRetrieval = false;
			fixParamsRetrieval = false;
		}
		else
		{
			// TODO: verify if our statement for sysprocedures really works with Informix 10.x
			fixProcRetrieval = JdbcUtils.hasMinimumServerVersion(conn, "10.0");
			fixParamsRetrieval = JdbcUtils.hasMinimumServerVersion(conn, "11.0");
		}
	}

	@Override
	public DataStore getProcedures(String catalog, String schemaPattern, String namePattern)
		throws SQLException
	{
		if (!fixProcRetrieval)
		{
			LogMgr.logDebug("InformixProcedureReader.getProcedures()", "Using JDBC driver to retrieve procedures.");
			return super.getProcedures(catalog, schemaPattern, namePattern);
		}

		catalog = DbMetadata.cleanupWildcards(catalog);
		schemaPattern = DbMetadata.cleanupWildcards(schemaPattern);
		namePattern = DbMetadata.cleanupWildcards(namePattern);

		Statement stmt = null;
		ResultSet rs = null;
		String sql = getSQL(catalog, schemaPattern, namePattern);
		DataStore ds = buildProcedureListDataStore(this.connection.getMetadata(), false);

		boolean showParametersInName = connection.getDbSettings().getBoolProperty("procedurelist.showparameters", true);

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
				long procid = rs.getLong("procid");

				int type = rs.getInt("PROCEDURE_TYPE");
				int row = ds.addRow();

				ProcedureDefinition def = new ProcedureDefinition(null, schema, name, type);

				List<ParamDef> argTypes = convertTypeList(args);
				List<ColumnIdentifier> cols = new ArrayList<>();

				String typeList = "";

				for (int i=0; i < argTypes.size(); i++)
				{
					if (i > 0) typeList += ",";
					String argName = "parameter_" + Integer.toString(i + 1);
					ColumnIdentifier col = new ColumnIdentifier(argName);
					String ifxType = argTypes.get(i).ifxTypeName;
					typeList += ifxType;
					col.setDbmsType(ifxType);
					col.setDataType(getJdbcType(ifxType));
					col.setArgumentMode(argTypes.get(i).mode);
					cols.add(col);
				}
				def.setParameters(cols);
				def.setSpecificName(specificName);
				def.setInternalIdentifier(Long.valueOf(procid));
				def.setDisplayName(name + "(" + typeList + ")");
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, null);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, showParametersInName ? def.getDisplayName() : name);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, type);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, null);
				ds.getRow(row).setUserObject(def);
			}

			return ds;
		}
		catch (Exception e)
		{
			LogMgr.logError("InformixProcedureReader.getProcedures()", "Error retrieving procedures using query:\n" + sql, e);
			fixProcRetrieval = false;
			return super.getProcedures(catalog, schemaPattern, namePattern);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private List<ParamDef> convertTypeList(String ifxTypeList)
	{
		List<String> plain = StringUtil.stringToList(ifxTypeList, ",", true, true);
		List<ParamDef> result = new ArrayList<>(plain.size());
		for (String ifx : plain)
		{
			String[] elements = ifx.split(" ");
			ParamDef def = new ParamDef();
			if (elements.length == 1)
			{
				def.ifxTypeName = elements[0];
				def.mode = "INPUT";
			}
			else if (elements.length == 2)
			{
				def.mode = elements[0].toUpperCase();
				def.ifxTypeName = elements[1];
			}
			result.add(def);
		}
		return result;
	}

	private String getSQL(String catalog, String schemaPattern, String namePattern)
	{
		StringBuilder sql = new StringBuilder(100);

		String sysProcs = getSysProceduresTable(catalog);

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
			"       procid, \n" +
			"       paramtypes::LVARCHAR as parameter_types \n" +
			"FROM " + sysProcs + " \n" +
			"WHERE internal = 'f' \n" +
			"  AND mode IN ('D', 'd', 'O', 'o', 'R', 'r') \n");

		SqlUtil.appendAndCondition(sql, "owner", schemaPattern, this.connection);
		SqlUtil.appendAndCondition(sql, "procname", namePattern, this.connection);
		return sql.toString();
	}

	@Override
	public DataStore getProcedureColumns(ProcedureDefinition def)
		throws SQLException
	{
		DataStore ds = null;

		if (fixParamsRetrieval)
		{
			// most of the column information is already present (because it's retrieved together with the procedure definition)
			// but that is lacking the parameter names. Therefor I'm doing another query to the database.
			ds = retrieveColumns(def);
		}

		if (ds == null && def != null && !def.getParameterTypes().isEmpty())
		{
			// the exception is already logged
			ds = createProcColsDataStore();
			List<ColumnIdentifier> parameters = def.getParameters(null);
			int pos = 1;
			for (ColumnIdentifier col : parameters)
			{
				int row = ds.addRow();
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, col.getColumnName());
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, col.getDbmsType());
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NR, Integer.valueOf(pos));
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, col.getArgumentMode());
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, Integer.valueOf(getJdbcType(col.getDbmsType())));
				pos ++;
			}
		}
		else if (ds == null)
		{
			// fall back to JDBC driver, better than nothing
			ds = super.getProcedureColumns(def);
		}
		return ds;
	}

	private DataStore retrieveColumns(ProcedureDefinition def)
	{
		String sysProcs = getSysProceduresTable(def.getCatalog());
		String sysCols = getSysProcColumnsTable(def.getCatalog());

		String sql =
			"select col.paramid,  \n" +
			"       col.paramname,  \n" +
			"       ifx_param_types(col.procid) as param_types, \n" +
			"       ifx_ret_types(col.procid) as ret_types, \n" +
			"       case  \n" +
			"         when col.paramattr = 1 then 'INPUT' \n" +
			"         when col.paramattr = 2 then 'INOUT' \n" +
			"         when col.paramattr in (3,5) then 'RETURN' \n" +
			"         when col.paramattr = 4 then 'OUT' \n" +
			"         else '' \n" +
			"       end as param_mode \n" +
			"from " + sysCols + " col \n" +
			"  join " + sysProcs + " p on p.procid = col.procid \n";

		if (def.getInternalIdentifier() != null)
		{
			sql += "where p.procid = " + def.getInternalIdentifier() + " \n";
		}
		else if (CollectionUtil.isNonEmpty(def.getParameterTypes()))
		{
			String types = StringUtil.listToString(def.getParameterTypes(), ",", false);
			sql +=
				"where p.procname = '" + def.getProcedureName() + "' \n " +
				"  and ifx_param_types(p.procid) = '" + types + "' \n";
		}
		sql += "order by col.paramid";
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("InformixProcedureReader.getProcedures()", "Query to retrieve procedure parameters:\n" + sql);
		}

		DataStore ds = createProcColsDataStore();

		Statement stmt = null;
		ResultSet rs = null;
		try
		{

			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			boolean isFirst = true;
			List<ParamDef> types = null;
			while (rs.next())
			{
				int paramid = rs.getInt("paramid");
				String name = rs.getString("paramname");
				String argTypes = rs.getString("param_types");
				String mode = rs.getString("param_mode");
				String returnType = rs.getString("ret_types");

				if (isFirst)
				{
					types = convertTypeList(argTypes);
					isFirst = false;
				}
				String dataType = null;
				if (paramid == 0)
				{
					dataType = returnType;
				}
				else if (paramid - 1 < types.size())
				{
					dataType = types.get(paramid - 1).ifxTypeName;
				}
				int row = ds.addRow();
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, name);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, dataType);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NR, Integer.valueOf(paramid));
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, mode);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, Integer.valueOf(getJdbcType(dataType)));
			}
			return ds;
		}
		catch (Exception e)
		{
			LogMgr.logError("InformixProcedureReader.getProcedures()", "Error retrieving procedure columns using query:\n" + sql, e);
			fixParamsRetrieval = false; // don't try again
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private int getJdbcType(String typeName)
	{
		if (typeName == null) return Types.OTHER;
		String baseType = SqlUtil.getBaseTypeName(typeName);
		Integer jdbc = getJavaTypeMapping().get(baseType.toLowerCase());
		if (jdbc == null) return Types.OTHER;
		return jdbc.intValue();
	}

	private Map<String, Integer> getJavaTypeMapping()
	{
		if (typeMap == null)
		{
			typeMap = new HashMap<>();
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

	private String getSysProceduresTable(String catalog)
	{
		String systemSchema = getSystemSchema();
		TableIdentifier procs = new TableIdentifier(catalog, systemSchema, "sysprocedures");
		return procs.getFullyQualifiedName(connection);
	}

	private String getSysProcColumnsTable(String catalog)
	{
		String systemSchema = getSystemSchema();
		TableIdentifier cols = new TableIdentifier(catalog, systemSchema, "sysproccolumns");
		return cols.getFullyQualifiedName(connection);
	}

	private String getSystemSchema()
	{
		return connection.getDbSettings().getProperty("systemschema", "informix");
	}

	private static class ParamDef
	{
		String ifxTypeName = "";
		String mode = "INPUT";
	}
}
