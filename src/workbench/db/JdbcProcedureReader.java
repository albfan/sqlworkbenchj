/*
 * JdbcProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Retrieve information about stored procedures from the database.
 * To retrieve the source of the Stored procedure, SQL statements need
 * to be defined in the ProcSourceStatements.xml
 * 
 * @see workbench.db.MetaDataSqlManager
 * 
 * @author  support@sql-workbench.net
 */
public class JdbcProcedureReader
	implements ProcedureReader
{
	protected WbConnection connection;
	
	public JdbcProcedureReader(WbConnection conn)
	{
		this.connection = conn;
	}
	
	public StringBuilder getProcedureHeader(String catalog, String schema, String procName, int procType)
	{
		return StringUtil.emptyBuffer();
	}
	
	/**
	 * Checks if the given procedure exists in the database
	 */
	public boolean procedureExists(String catalog, String schema, String procName, int procType)
	{
		boolean exists = false;
		ResultSet rs = null;
		try
		{
			rs = this.connection.getSqlConnection().getMetaData().getProcedures(catalog, schema, procName);
			if (rs.next())
			{
				int type = rs.getInt(8);
				if (type == DatabaseMetaData.procedureResultUnknown || 
					  procType == DatabaseMetaData.procedureResultUnknown ||
						type == procType)
				{
					exists = true;
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("JdbcProcedureReader.procedureExists()", "Error checking procedure", e);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return exists;
	}
	
	public DataStore getProcedures(String aCatalog, String aSchema)
		throws SQLException
	{
		if ("*".equals(aSchema) || "%".equals(aSchema))
		{
			aSchema = null;
		}
		
		ResultSet rs = this.connection.getSqlConnection().getMetaData().getProcedures(aCatalog, aSchema, "%");
		return fillProcedureListDataStore(rs);
	}

	public DataStore buildProcedureListDataStore(DbMetadata meta)
	{
		String[] cols = new String[] {"PROCEDURE_NAME", "TYPE", meta.getCatalogTerm().toUpperCase(), meta.getSchemaTerm().toUpperCase(), "REMARKS"};
		final int types[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] = {30,12,10,10,20};

		DataStore ds = new DataStore(cols, types, sizes);
		return ds;
	}
	
	public DataStore fillProcedureListDataStore(ResultSet rs)
		throws SQLException
	{
		DataStore ds = buildProcedureListDataStore(this.connection.getMetadata());
		
		String versionDelimiter = this.connection.getMetadata().getDbSettings().getProcVersionDelimiter();
		boolean stripVersion = this.connection.getMetadata().getDbSettings().getStripProcedureVersion();
		try
		{
			while (rs.next())
			{
				String cat = rs.getString("PROCEDURE_CAT");
				String schema = rs.getString("PROCEDURE_SCHEM");
				String name = rs.getString("PROCEDURE_NAME");
				String remark = rs.getString("REMARKS");
				short type = rs.getShort("PROCEDURE_TYPE");
				Integer iType = null;
				if (rs.wasNull())
				{
					//sType = "N/A";
					iType = new Integer(DatabaseMetaData.procedureResultUnknown);
				}
				else
				{
					iType = new Integer(type);
				}
				int row = ds.addRow();


				if (stripVersion)
				{
					int pos = name.lastIndexOf(versionDelimiter);
					if (pos > 1)
					{
						name = name.substring(0,pos);
					}
				}
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, cat);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, name);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, iType);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("JdbcProcedureReader.getProcedures()", "Error while retrieving procedures", e);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
	}
	
	public static String convertProcType(int type)
	{
		if (type == DatabaseMetaData.procedureNoResult)
			return ProcedureReader.PROC_RESULT_NO;
		else if (type == DatabaseMetaData.procedureReturnsResult)
			return ProcedureReader.PROC_RESULT_YES;
		else
			return ProcedureReader.PROC_RESULT_UNKNOWN;
	}
	
	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException
	{
		final String cols[] = {"COLUMN_NAME", "TYPE", "TYPE_NAME", "REMARKS"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {20, 10, 18, 30};
		DataStore ds = new DataStore(cols, types, sizes);

		ResultSet rs = null;
		try
		{
			rs = this.connection.getSqlConnection().getMetaData().getProcedureColumns(aCatalog, aSchema, aProcname, "%");
			while (rs.next())
			{
				int row = ds.addRow();
				String colName = rs.getString("COLUMN_NAME");
				ds.setValue(row, 0, colName);
				int colType = rs.getInt("COLUMN_TYPE");
				String stype;

				if (colType == DatabaseMetaData.procedureColumnIn)
					stype = "IN";
				else if (colType == DatabaseMetaData.procedureColumnInOut)
					stype = "INOUT";
				else if (colType == DatabaseMetaData.procedureColumnOut)
					stype = "OUT";
				else if (colType == DatabaseMetaData.procedureColumnResult)
					stype = "RESULTSET";
				else if (colType == DatabaseMetaData.procedureColumnReturn)
					stype = "RETURN";
				else
					stype = "";
				ds.setValue(row, 1, stype);

				int sqlType = rs.getInt("DATA_TYPE");
				String typeName = rs.getString("TYPE_NAME");
				int digits = rs.getInt("PRECISION");
				int size = rs.getInt("LENGTH");
				String rem = rs.getString("REMARKS");

				String display = SqlUtil.getSqlTypeDisplay(typeName, sqlType, size, digits);
				ds.setValue(row, 2, display);
				ds.setValue(row, 3, rem);
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return ds;
	}
	
	public void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{
		if (def == null) return;

		GetMetaDataSql sql = this.connection.getMetadata().metaSqlMgr.getProcedureSourceSql();
		if (sql == null)
		{
			throw new NoConfigException();
		}
		
		String procName = def.getProcedureName();
		
		// MS SQL Server (sometimes?) appends a ;1 to
		// the end of the procedure name
		int i = procName.indexOf(';');
		if (i > -1 && this.connection.getMetadata().isSqlServer())
		{
			procName = procName.substring(0, i);
		}

		StringBuilder source = new StringBuilder(500);

		StringBuilder header = getProcedureHeader(def.getCatalog(), def.getSchema(), procName, def.getResultType());
		source.append(header);

		Statement stmt = null;
		ResultSet rs = null;
    int linecount = 0;
		
		try
		{
			sql.setSchema(def.getSchema());
			sql.setObjectName(procName);
			sql.setCatalog(def.getCatalog());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("DbMetadata.getProcedureSource()", "Using query=\n" + sql.getSql());
			}

			stmt = this.connection.createStatement();
			rs = stmt.executeQuery(sql.getSql());
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
				{
					linecount ++;
					source.append(line);
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("JdbcProcedureReader.getProcedureSource()", "Error retrieving procedure source", e);
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
			if (this.connection.getMetadata().isPostgres())
			{
				try { this.connection.rollback(); } catch (Throwable th) {}
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		boolean needsTerminator = this.connection.getDbSettings().proceduresNeedTerminator();

		if (!StringUtil.endsWith(source, ';') && needsTerminator)
		{
			source.append(';');
		}
		
		String result = source.toString();
		
		String dbId = this.connection.getMetadata().getDbId();
		boolean replaceNL = Settings.getInstance().getBoolProperty("workbench.db." + dbId + ".replacenl.proceduresource", false);
		
		if (replaceNL)
		{
			String nl = Settings.getInstance().getInternalEditorLineEnding();
			result = StringUtil.replace(source.toString(), "\\n", nl);
		}
		
		def.setSource(result);
	}
	

}
