/*
 * PostgresProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author  support@sql-workbench.net
 */
public class PostgresProcedureReader
	extends JdbcProcedureReader
{
	// Maps PG type names to Java types.
	private Map<String, Integer> pgTypeMap;
	private Map<Integer, PGType> pgTypes;
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
		if (pgTypeMap == null)
		{
			// This mapping has been copied from the JDBC driver.
			// This map is deeply hidden in the driver and even 
			// if I hard-coded references to the driver into the 
			// class I wouldn't know how to retrieve them.
			pgTypeMap = new HashMap<String, Integer>();
			pgTypeMap.put("int2", new Integer(Types.SMALLINT));
			pgTypeMap.put("int4", new Integer(Types.INTEGER));
			pgTypeMap.put("oid", new Integer(Types.INTEGER));
			pgTypeMap.put("int8", new Integer(Types.BIGINT));
			pgTypeMap.put("money", new Integer(Types.DOUBLE));
			pgTypeMap.put("numeric", new Integer(Types.NUMERIC));
			pgTypeMap.put("float4", new Integer(Types.REAL));
			pgTypeMap.put("float8", new Integer(Types.DOUBLE));
			pgTypeMap.put("bpchar", new Integer(Types.CHAR));
			pgTypeMap.put("varchar", new Integer(Types.VARCHAR));
			pgTypeMap.put("text", new Integer(Types.VARCHAR));
			pgTypeMap.put("name", new Integer(Types.VARCHAR));
			pgTypeMap.put("bytea", new Integer(Types.BINARY));
			pgTypeMap.put("bool", new Integer(Types.BIT));
			pgTypeMap.put("bit", new Integer(Types.BIT));
			pgTypeMap.put("date", new Integer(Types.DATE));
			pgTypeMap.put("time", new Integer(Types.TIME));
			pgTypeMap.put("timetz", new Integer(Types.TIME));
			pgTypeMap.put("timestamp", new Integer(Types.TIMESTAMP));
			pgTypeMap.put("timestamptz", new Integer(Types.TIMESTAMP));
    }
		return pgTypeMap;
	}
	
	private Integer getJavaType(String pgType)
	{
		Integer i = getJavaTypeMapping().get(pgType);
		if (i == null) return new Integer(Types.OTHER);
		return i;
	}
	
	private Map<Integer, PGType> getPGTypes()
	{
		if (pgTypes == null)
		{
			pgTypes = new HashMap<Integer, PGType>(300);
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
					Integer oid = new Integer(rs.getInt(1));
					PGType typ = new PGType();
					typ.rawType = rs.getString(2);
					typ.formattedType = StringUtil.trimQuotes(rs.getString(3));
					typ.oid = oid.intValue();
					pgTypes.put(oid, typ);
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
				pgTypes = Collections.emptyMap();
			}
			finally
			{
				SqlUtil.closeAll(rs, stmt);
			}
		}
		return pgTypes;
	}
	
	private String getRawTypeNameFromOID(int oid)
	{
		PGType typ = getPGTypes().get(new Integer(oid));
		return typ.rawType;
	}

	private String getFormattedTypeFromOID(int oid)
	{
		PGType typ = getPGTypes().get(new Integer(oid));
		return typ.formattedType;
	}
	
	public StringBuilder getProcedureHeader(String aCatalog, String aSchema, String aProcname, int procType)
	{
		StringBuilder source = new StringBuilder();
		
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		Savepoint sp = null;
		try
		{
			sp = this.connection.setSavepoint();
			DataStore ds = this.getProcedureColumns(aCatalog, aSchema, aProcname);
			source.append("CREATE OR REPLACE FUNCTION ");
			
			source.append(aProcname);
			source.append(" (");
			String retType = null;
			int count = ds.getRowCount();
			int added = 0;
			for (int i=0; i < count; i++)
			{
				String varname = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
				String mode = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				String vartype = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String ret = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if ("RETURN".equals(ret))
				{
					retType = vartype;
				}
				else
				{
					if (added > 0) source.append(',');
					source.append(mode);
					source.append(' ');
					if (varname != null) 
					{
						source.append(varname);
						source.append(' ');
					}
					source.append(vartype);
					added ++;
				}
			}
			source.append(')');
			if (retType != null)
			{
				source.append(nl + "RETURNS ");
				source.append(retType);
			}
			source.append(nl + "AS" + nl);
			this.connection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			this.connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.getProcedureHeader()", "Error retrieving header", e);
			source = StringUtil.emptyBuffer();
		}
		return source;
	}

	public DataStore getProcedureColumns(String catalog, String schema, String procname)
		throws SQLException
	{
//		String version = connection.getMetadata().getDbVersion();
//		int majorVersion = 0;
//		int minorVersion = 0;
//		
//		try
//		{
//			String[] parts = version.split("\\.");
//			majorVersion = (parts.length > 0 ? Integer.valueOf(parts[0]) : 0);
//			minorVersion = (parts.length > 1 ? Integer.valueOf(parts[1]) : 0);
//		}
//		catch (Throwable th)
//		{
//			majorVersion = 0;
//			minorVersion = 0;
//		}
//		
		if (Settings.getInstance().getBoolProperty("workbench.db.postgresql.fixproctypes", true) 
			  && JdbcUtils.hasMinimumServerVersion(connection, "8.1"))
		{
			return getColumns(catalog, schema, procname);
		}
		else
		{
			return super.getProcedureColumns(catalog, schema, procname);
		}
	}
	
	/** 
	 * A workaround for pre 8.3 drivers so that argument names are retrieved properly
	 * from the database. This was mainly inspired by the source code of pgAdmin III
	 * and the current 8.3 driver sources
	 * 
	 * @param catalog
	 * @param schema
	 * @param procname
	 * @return a DataStore with the argumens of the procedure
	 * @throws java.sql.SQLException
	 */
	private DataStore getColumns(String catalog, String schema, String procname)
		throws SQLException
	{
		String sql = "SELECT format_type(p.prorettype, NULL) as formatted_type, \n" + 
			       "       t.typname as pg_type, \n" + 
						 "       array_to_string(p.proargtypes, ';') as argtypes, \n" + 
             "       array_to_string(p.proallargtypes, ';') as allargtypes, \n" + 
             "       array_to_string(p.proargnames, ';') as argnames, \n" + 		
						 "       array_to_string(p.proargmodes, ';') as modes, \n" +
						 "       t.typtype " + 
             "FROM pg_catalog.pg_proc p, \n" + 
             "     pg_catalog.pg_namespace n, \n " +
						 "     pg_catalog.pg_type t " + 
             "WHERE p.pronamespace = n.oid \n" + 
             "AND   n.nspname = ? \n " +
						 "AND   p.prorettype = t.oid \n" + 
             "AND   p.proname = ?";		
		
		DataStore result = createProcColsDataStore();
		
		Savepoint sp = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			sp = this.connection.setSavepoint();
			
			stmt = this.connection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, schema);
			stmt.setString(2, procname);

			rs = stmt.executeQuery();
			if (rs.next())
			{
				String typeName = rs.getString("formatted_type");
				String pgType = rs.getString("pg_type");
				String types = rs.getString("argtypes");
				String allTypes = rs.getString("allargtypes");
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
				
				List<String> argNames = null;
				List<String> argTypes = null;
				List<String> argModes = StringUtil.stringToList(modes, ";", true, true);
				if (allTypes != null)
				{
					argTypes = StringUtil.stringToList(allTypes, ";", true, true);
					argNames = StringUtil.stringToList(names, ";", true, true);
				}	
				else
				{
					argTypes = StringUtil.stringToList(types, ";", true, true);
				}

				for (int i=0; i < argTypes.size(); i++)
				{
					int row = result.addRow();
					int typeOid = -1;
					try
					{
						typeOid = Integer.valueOf(argTypes.get(i));
					}
					catch (Exception e)
					{
						// 2278 = void
						typeOid = (voidType != null ? voidType.oid : 2278);
					}
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
				return super.getProcedureColumns(catalog, schema, procname);
			}
			
			this.connection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			this.connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.getProcedureHeader()", "Error retrieving header", e);
			return super.getProcedureColumns(catalog, schema, procname);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}
}

class PGType
{
	String rawType;
	String formattedType;
	int oid;
	
	public PGType()
	{
	}
}
