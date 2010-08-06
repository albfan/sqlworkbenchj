/*
 * PostgresProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.db.JdbcProcedureReader;
import workbench.db.JdbcUtils;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read procedure and function definitions from a Postgres database.
 *
 * @author  Thomas Kellerer
 */
public class PostgresProcedureReader
	extends JdbcProcedureReader
{
	// Maps PG type names to Java types.
	private Map<String, Integer> pgType2Java;
	private PGTypeLookup pgTypes;
	private PGType voidType;

	public PostgresProcedureReader(WbConnection conn)
	{
		super(conn);
		try
		{
			this.useSavepoint = conn.supportsSavepoints();
		}
		catch (Throwable th)
		{
			this.useSavepoint = false;
		}
	}

	private Map<String, Integer> getJavaTypeMapping()
	{
		if (pgType2Java == null)
		{
			// This mapping has been copied from the JDBC driver.
			// This map is a private attribute of the class org.postgresql.jdbc2.TypeInfoCache
			// so, even if I hardcoded references to the Postgres driver I wouldn't be able
			// to use the information.
			pgType2Java = new HashMap<String, Integer>();
			pgType2Java.put("int2", Integer.valueOf(Types.SMALLINT));
			pgType2Java.put("int4", Integer.valueOf(Types.INTEGER));
			pgType2Java.put("oid", Integer.valueOf(Types.INTEGER));
			pgType2Java.put("int8", Integer.valueOf(Types.BIGINT));
			pgType2Java.put("money", Integer.valueOf(Types.DOUBLE));
			pgType2Java.put("numeric", Integer.valueOf(Types.NUMERIC));
			pgType2Java.put("float4", Integer.valueOf(Types.REAL));
			pgType2Java.put("float8", Integer.valueOf(Types.DOUBLE));
			pgType2Java.put("char", Integer.valueOf(Types.CHAR));
			pgType2Java.put("bpchar", Integer.valueOf(Types.CHAR));
			pgType2Java.put("varchar", Integer.valueOf(Types.VARCHAR));
			pgType2Java.put("text", Integer.valueOf(Types.VARCHAR));
			pgType2Java.put("name", Integer.valueOf(Types.VARCHAR));
			pgType2Java.put("bytea", Integer.valueOf(Types.BINARY));
			pgType2Java.put("bool", Integer.valueOf(Types.BIT));
			pgType2Java.put("bit", Integer.valueOf(Types.BIT));
			pgType2Java.put("date", Integer.valueOf(Types.DATE));
			pgType2Java.put("time", Integer.valueOf(Types.TIME));
			pgType2Java.put("timetz", Integer.valueOf(Types.TIME));
			pgType2Java.put("timestamp", Integer.valueOf(Types.TIMESTAMP));
			pgType2Java.put("timestamptz", Integer.valueOf(Types.TIMESTAMP));
    }
		return pgType2Java;
	}

	private Integer getJavaType(String pgType)
	{
		Integer i = getJavaTypeMapping().get(pgType);
		if (i == null) return Integer.valueOf(Types.OTHER);
		return i;
	}

	protected PGTypeLookup getTypeLookup()
	{
		if (pgTypes == null)
		{
			Map<Integer, PGType> typeMap = new HashMap<Integer, PGType>(300);
			Statement stmt = null;
			ResultSet rs = null;
			Savepoint sp = null;
			try
			{
				sp = connection.setSavepoint();
				stmt = connection.createStatement();
				rs = stmt.executeQuery("select oid, typname, format_type(oid, null) from pg_type");
				while (rs.next())
				{
					Integer oid = Integer.valueOf(rs.getInt(1));
					PGType typ = new PGType(rs.getString(2), StringUtil.trimQuotes(rs.getString(3)), oid.intValue());
					typeMap.put(typ.oid, typ);
					if ("void".equals(typ.rawType))
					{
						voidType = typ;
					}
				}
				connection.releaseSavepoint(sp);
			}
			catch (SQLException e)
			{
				connection.rollback(sp);
				LogMgr.logError("PostgresProcedureReqder.getPGTypes()", "Could not read postgres data types", e);
				typeMap = Collections.emptyMap();
			}
			finally
			{
				SqlUtil.closeAll(rs, stmt);
			}
			pgTypes = new PGTypeLookup(typeMap);
		}
		return pgTypes;
	}

	private String getRawTypeNameFromOID(int oid)
	{
		PGType typ = getTypeLookup().getTypeFromOID(Integer.valueOf(oid));
		return typ.rawType;
	}

	private String getFormattedTypeFromOID(int oid)
	{
		PGType typ = getTypeLookup().getTypeFromOID(Integer.valueOf(oid));
		return typ.formattedType;
	}

	@Override
	public DataStore getProcedures(String catalog, String schemaPattern, String procName)
		throws SQLException
	{
		if ("*".equals(schemaPattern) || "%".equals(schemaPattern))
		{
			schemaPattern = null;
		}

		String namePattern = null;
		if ("*".equals(procName) || "%".equals(procName))
		{
			namePattern = null;
		}
		else if (StringUtil.isNonBlank(procName))
		{
			PGProcName pg = new PGProcName(procName, getTypeLookup());
			namePattern = pg.getName();
		}

		Statement stmt = null;
		Savepoint sp = null;
		ResultSet rs = null;
		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}

			String sql =
						"SELECT NULL AS PROCEDURE_CAT, \n" +
						"       n.nspname AS PROCEDURE_SCHEM, \n" +
						"       p.proname AS PROCEDURE_NAME, \n" +
						"	      d.description AS REMARKS, \n" +
						"       array_to_string(p.proargtypes, ';') as PG_ARGUMENTS \n" +
						" FROM pg_catalog.pg_namespace n, pg_catalog.pg_proc p \n" +
						"   LEFT JOIN pg_catalog.pg_description d ON (p.oid=d.objoid) \n" +
						"   LEFT JOIN pg_catalog.pg_class c ON (d.classoid=c.oid AND c.relname='pg_proc') \n" +
						"   LEFT JOIN pg_catalog.pg_namespace pn ON (c.relnamespace=pn.oid AND pn.nspname='pg_catalog') \n" +
						" WHERE p.pronamespace=n.oid ";

			if (StringUtil.isNonBlank(schemaPattern))
			{
					sql += " AND n.nspname LIKE '" + schemaPattern + "' ";
			}
			if (StringUtil.isNonBlank(namePattern))
			{
					sql += " AND p.proname LIKE '" + namePattern + "' ";
			}
			sql += " ORDER BY PROCEDURE_SCHEM, PROCEDURE_NAME ";

			stmt = connection.createStatementForQuery();

			rs = stmt.executeQuery(sql);
			DataStore ds = buildProcedureListDataStore(this.connection.getMetadata(), false);

			while (rs.next())
			{
				String cat = rs.getString("PROCEDURE_CAT");
				String schema = rs.getString("PROCEDURE_SCHEM");
				String name = rs.getString("PROCEDURE_NAME");
				String remark = rs.getString("REMARKS");
				String args = rs.getString("PG_ARGUMENTS");
				int row = ds.addRow();

				PGProcName pname = new PGProcName(name, args, getTypeLookup());

				ProcedureDefinition def = new ProcedureDefinition(cat, schema, name, java.sql.DatabaseMetaData.procedureReturnsResult);
				def.setDisplayName(pname.getFormattedName());
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, cat);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, pname.getFormattedName());
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, java.sql.DatabaseMetaData.procedureReturnsResult);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
				ds.getRow(row).setUserObject(def);
			}

			this.connection.releaseSavepoint(sp);
			ds.resetStatus();
			return ds;
		}
		catch (SQLException sql)
		{
			this.connection.rollback(sp);
			throw sql;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	public DataStore getProcedureColumns(ProcedureDefinition def)
		throws SQLException
	{
		PGProcName pgName = new PGProcName(def.getProcedureName(), getTypeLookup());
		if (Settings.getInstance().getBoolProperty("workbench.db.postgresql.fixproctypes", true)
			  && JdbcUtils.hasMinimumServerVersion(connection, "8.1"))
		{
			return getColumns(def.getCatalog(), def.getSchema(), pgName);
		}
		else
		{
			return super.getProcedureColumns(def.getCatalog(), def.getSchema(), def.getProcedureName());
		}
	}

	@Override
	public void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{
		boolean usePGFunction = Settings.getInstance().getBoolProperty("workbench.db.postgresql.procsource.useinternal", true);
		if (usePGFunction && JdbcUtils.hasMinimumServerVersion(connection, "8.4"))
		{
			readFunctionDef(def);
			return;
		}

		PGProcName name = new PGProcName(def.getProcedureName(), getTypeLookup());

		String sql = "SELECT p.prosrc, \n" +
								"        l.lanname as lang_name, \n";

		if (JdbcUtils.hasMinimumServerVersion(connection, "8.4"))
		{
			sql += "        pg_get_function_result(p.oid) as formatted_return_type, \n";
		}
		else
		{
			sql += "        null::text as formatted_return_type, \n";
		}
		sql +=	"        p.prorettype as return_type_oid, \n" +
						"        coalesce(array_to_string(p.proallargtypes, ';'), " +
						"                 array_to_string(p.proargtypes, ';')) as argtypes, \n" +
						"        array_to_string(p.proargnames, ';') as argnames, \n" +
						"        array_to_string(p.proargmodes, ';') as argmodes, \n " +
						"        p.prosecdef, " +
						"        p.proretset, " +
						"        p.provolatile, " +
						"        p.proisstrict, " +
						"        p.proisagg ";

		boolean hasCost = JdbcUtils.hasMinimumServerVersion(connection, "8.3");
		if (hasCost)
		{
			sql += ", p.procost, p.prorows ";
		}
		sql +=  "\nFROM pg_proc p, pg_language l, pg_namespace n \n" +
								" where p.prolang = l.oid \n" +
								" and p.pronamespace = n.oid ";

		sql += " and p.proname = '" + name.getName() + "' ";
		if (StringUtil.isNonBlank(def.getSchema()))
		{
			sql += " and n.nspname = '" + def.getSchema() + "' ";
		}

		String oids = name.getOIDs();
		if (StringUtil.isNonBlank(oids))
		{
			sql += " AND p.proargtypes = cast('" + oids + "' as oidvector)";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresProcedureReader.readProcedureSource()", "Using SQL=" + sql);
		}

		StringBuilder source = new StringBuilder(500);

		ResultSet rs = null;
		Savepoint sp = null;
		Statement stmt = null;

		boolean isAggregate = false;

		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);

			if (rs.next())
			{
				isAggregate = rs.getBoolean("proisagg");
			}

			if (!isAggregate)
			{
				source.append("CREATE OR REPLACE FUNCTION ");
				source.append(name.getName());

				String src = rs.getString(1);
				if (rs.wasNull() || src == null) src = "";

				String lang = rs.getString("lang_name");
				int retTypeOid = rs.getInt("return_type_oid");
				String readableReturnType = rs.getString("formatted_return_type");

				String types = rs.getString("argtypes");
				String names = rs.getString("argnames");
				String modes = rs.getString("argmodes");
				boolean returnSet = rs.getBoolean("proretset");


				boolean securityDefiner = rs.getBoolean("prosecdef");
				boolean strict = rs.getBoolean("proisstrict");
				String volat = rs.getString("provolatile");

				Double cost = null;
				Double rows = null;
				if (hasCost)
				{
					cost = rs.getDouble("procost");
					rows = rs.getDouble("prorows");
				}
				List<String> argNames = StringUtil.stringToList(names, ";", true, true);
				List<String> argTypes = StringUtil.stringToList(types, ";", true, true);
				List<String> argModes = StringUtil.stringToList(modes, ";", true, true);

				source.append('(');
				int paramCount = 0;

				for (int i=0; i < argTypes.size(); i++)
				{
					if (paramCount > 0) source.append(", ");

					if (i < argModes.size())
					{
						String mode = argModes.get(i);
						if ("o".equals(mode)) source.append("OUT ");
						if ("b".equals(mode)) source.append("INOUT");
					}

					if (i < argNames.size())
					{
						source.append(argNames.get(i));
						source.append(' ');
					}

					int typeOid = StringUtil.getIntValue(argTypes.get(i), voidType.oid);
					source.append(getRawTypeNameFromOID(typeOid));
					paramCount ++;
				}

				source.append(")\n  RETURNS ");
				if (readableReturnType == null)
				{
					if (returnSet)
					{
						source.append("SETOF ");
					}
					source.append(getRawTypeNameFromOID(retTypeOid));
				}
				else
				{
					source.append(readableReturnType);
				}
				source.append("\n  LANGUAGE ");
				source.append(lang);
				source.append("\nAS\n$body$\n");
				src = src.trim();
				source.append(StringUtil.makePlainLinefeed(src));
				if (!src.endsWith(";")) source.append(';');
				source.append("\n$body$\n");
				if (volat.equals("i"))
				{
					source.append(" IMMUTABLE");
				}
				else if (volat.equals("s"))
				{
					source.append(" STABLE");
				}
				else
				{
					source.append(" VOLATILE");
				}
				if (strict)
				{
					source.append(" STRICT");
				}
				if (securityDefiner)
				{
					source.append("\n SECURITY DEFINER");
				}

				if (cost != null)
				{
					source.append("\n COST ");
					source.append(cost.longValue());
				}

				if (rows != null && returnSet)
				{
					source.append("\n ROWS ");
					source.append(rows.longValue());
				}
				source.append('\n');
				source.append(Settings.getInstance().getAlternateDelimiter(connection).getDelimiter());
				source.append('\n');
				if (StringUtil.isNonBlank(def.getComment()))
				{
					source.append("\nCOMMENT ON FUNCTION " + name.getFormattedName() + " IS '" + SqlUtil.escapeQuotes(def.getComment()) + "'\n" );
					source.append(Settings.getInstance().getAlternateDelimiter(connection).getDelimiter());
					source.append('\n');
				}
			}
			connection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
			connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.readProcedureSource()", "Error retrieving source for " + name.getFormattedName(), e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		if (isAggregate)
		{
			source.append(getAggregateSource(name, def.getSchema()));
			if (StringUtil.isNonBlank(def.getComment()))
			{
				source.append("\n\nCOMMENT ON AGGREGATE IS '" + SqlUtil.escapeQuotes(def.getComment()) + "';\n\n");
			}
		}
		def.setSource(source);
	}

	/**
	 * Read the definition of a function using pg_get_functiondef()
	 *
	 * @param def
	 */
	protected void readFunctionDef(ProcedureDefinition def)
	{
		PGProcName name = new PGProcName(def.getProcedureName(), getTypeLookup());
		String funcname = def.getSchema() + "." + name.getFormattedName();
		String sql = "select pg_get_functiondef('" + funcname + "'::regprocedure)";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresProcedureReader.readFunctionDef()", "Using SQL=" + sql);
		}

		StringBuilder source = null;
		ResultSet rs = null;
		Savepoint sp = null;
		Statement stmt = null;
		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				String s = rs.getString(1);
				if (StringUtil.isNonBlank(s))
				{
					source = new StringBuilder(s.length() + 50);
					source.append(s);
					if (!s.endsWith("\n"))	source.append('\n');
					source.append(Settings.getInstance().getAlternateDelimiter(connection).getDelimiter());
					source.append('\n');
					if (StringUtil.isNonBlank(def.getComment()))
					{
						source.append("\nCOMMENT ON FUNCTION " + name.getFormattedName() + " IS '" + SqlUtil.escapeQuotes(def.getComment()) + "'\n" );
						source.append(Settings.getInstance().getAlternateDelimiter(connection).getDelimiter());
						source.append('\n');
					}
				}
			}
		}
		catch (SQLException e)
		{
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
			connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.readProcedureSource()", "Error retrieving source for " + name.getFormattedName(), e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		def.setSource(source);
	}

	protected StringBuilder getAggregateSource(PGProcName name, String schema)
	{
		String baseSelect = "SELECT a.aggtransfn, a.aggfinalfn, format_type(a.aggtranstype, null) as stype, a.agginitval, op.oprname ";
	  String from =
			 " FROM pg_proc p \n" +
       "  JOIN pg_namespace n ON p.pronamespace = n.oid \n" +
       "  JOIN pg_aggregate a ON a.aggfnoid = p.oid \n" +
       "  LEFT JOIN pg_operator op ON op.oid = a.aggsortop ";

		boolean hasSort = JdbcUtils.hasMinimumServerVersion(connection, "8.1");
		if (hasSort)
		{
			baseSelect += ", a.aggsortop ";
		}

		String sql = baseSelect + "\n" + from;
		sql += " WHERE p.proname = '" + name.getName() + "' ";
		if (StringUtil.isNonBlank(schema))
		{
			sql += " and n.nspname = '" + schema + "' ";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresProcedureReader.getAggregateSource()", "Using SQL=" + sql);
		}
		StringBuilder source = new StringBuilder();
		ResultSet rs = null;
		Statement stmt = null;
		Savepoint sp = null;

		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{

				source.append("CREATE AGGREGATE ");
				source.append(name.getFormattedName());
				source.append("\n(\n");
				String sfunc = rs.getString("aggtransfn");
				source.append("  sfunc = " + sfunc);

				String stype = rs.getString("stype");
				source.append(",\n  stype = " + stype);

				String sortop = rs.getString("oprname");
				if (StringUtil.isNonBlank(sortop))
				{
					source.append(",\n  sortop = " + SqlUtil.quoteObjectname(sortop));
				}

				String finalfunc = rs.getString("aggfinalfn");
				if (StringUtil.isNonBlank(finalfunc) && !finalfunc.equals("-"))
				{
					source.append(",\n  finalfunc = " + finalfunc);
				}

				String initcond = rs.getString("agginitval");
				if (StringUtil.isNonBlank(initcond))
				{
					source.append(",\n  initcond = '" + initcond + "'");
				}
				source.append("\n);");
			}
			connection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			source = null;
			connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.readProcedureSource()", "Error retrieving aggregate source for " + name, e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;

	}


	/**
	 * A workaround for pre 8.3 drivers so that argument names are retrieved properly
	 * from the database. This was mainly inspired by the source code of pgAdmin III
	 * and the 8.3 driver sources
	 *
	 * @param catalog
	 * @param schema
	 * @param procname
	 * @return a DataStore with the argumens of the procedure
	 * @throws java.sql.SQLException
	 */
	private DataStore getColumns(String catalog, String schema, PGProcName procname)
		throws SQLException
	{
		String sql = "SELECT format_type(p.prorettype, NULL) as formatted_type, \n" +
			       "       t.typname as pg_type, \n" +
						 "       coalesce(array_to_string(proallargtypes, ';'), array_to_string(proargtypes, ';')) as argtypes, \n" +
             "       array_to_string(p.proargnames, ';') as argnames, \n" +
						 "       array_to_string(p.proargmodes, ';') as modes, \n" +
						 "       t.typtype " +
             "FROM pg_catalog.pg_proc p, \n" +
             "     pg_catalog.pg_namespace n, \n " +
						 "     pg_catalog.pg_type t " +
             "WHERE p.pronamespace = n.oid \n" +
             "AND   n.nspname = ? \n " +
						 "AND   p.prorettype = t.oid \n" +
             "AND   p.proname = ? ";

		DataStore result = createProcColsDataStore();

		Savepoint sp = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			sp = connection.setSavepoint();

			String oids = procname.getOIDs();
			if (StringUtil.isNonBlank(oids))
			{
				sql += " AND p.proargtypes = cast('" + oids + "' as oidvector)";
			}

			stmt = this.connection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, schema);
			stmt.setString(2, procname.getName());

			rs = stmt.executeQuery();
			if (rs.next())
			{
				String typeName = rs.getString("formatted_type");
				String pgType = rs.getString("pg_type");
				String types = rs.getString("argtypes");
				String names = rs.getString("argnames");
				String modes = rs.getString("modes");
				String returnTypeType = rs.getString("typtype");

				// pgAdmin II distinguishes functions from procedures using only the "modes" information
				// the driver uses the returnTypeType as well
				boolean isFunction = (returnTypeType.equals("b") || returnTypeType.equals("d") || (returnTypeType.equals("p") && modes == null));

				if (isFunction)
				{
					int row = result.addRow();
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, "returnValue");
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, "RETURN");
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, getJavaType(pgType));
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, StringUtil.trimQuotes(typeName));
				}

				List<String> argNames = StringUtil.stringToList(names, ";", true, true);
				List<String> argTypes = StringUtil.stringToList(types, ";", true, true);
				List<String> argModes = StringUtil.stringToList(modes, ";", true, true);

				for (int i=0; i < argTypes.size(); i++)
				{
					int row = result.addRow();
					int typeOid = StringUtil.getIntValue(argTypes.get(i), -1);
					String pgt = getRawTypeNameFromOID(typeOid);

					String nm = "$" + (i + 1);
					if (argNames != null && i < argNames.size())
					{
						nm = argNames.get(i);
					}
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, nm);

					String md = "IN";
					if (argModes != null && i < argModes.size())
					{
						String m = argModes.get(i);
						if ("o".equals(m))
						{
							md = "OUT";
						}
						else if ("b".equals(m))
						{
							md = "INOUT";
						}
					}
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, md);
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, getJavaType(pgt));
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, getFormattedTypeFromOID(typeOid));
				}

			}
			else
			{
				LogMgr.logWarning("PostgreProcedureReader.getProcedureHeader()", "Could not retrieve columns", null);
				return super.getProcedureColumns(catalog, schema, procname.getName());
			}

			connection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.getProcedureHeader()", "Error retrieving header", e);
			return super.getProcedureColumns(catalog, schema, procname.getName());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}
}
