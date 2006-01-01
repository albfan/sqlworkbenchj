/*
 * MySqlMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mysql;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
/**
 * @author  support@sql-workbench.net
 */
public class MySqlMetadata
	implements ProcedureReader
{
	private DbMetadata metaData;
	private WbConnection conn;
	public MySqlMetadata(DbMetadata meta, WbConnection con)
	{
		this.metaData = meta;
		this.conn = con;
	}

	public DataStore getProcedures(String catalog, String schema)
		throws SQLException
	{
		PreparedStatement stmt = null;
		String sql = "SELECT NULL AS procedure_cat, \n" + 
             "       routine_schema AS procedure_schem, \n" + 
             "       routine_name AS procedure_name, \n" + 
             "       case when routine_type = 'PROCEDURE' then 1 \n" + 
             "            else 2 end as procedure_type, \n" + 
             "       NULL AS remarks \n" + 
             "FROM information_schema.routines " +
		         " WHERE routine_schema like ?";	
		DataStore ds = null;
		try 
		{
			stmt = this.conn.getSqlConnection().prepareStatement(sql);
			if (schema == null)
			{
				stmt.setString(1, "%");
			}
			else
			{
				stmt.setString(1, schema);
			}
			ResultSet rs = stmt.executeQuery();
			JdbcProcedureReader reader = new JdbcProcedureReader(this.metaData);
			// buildProcedureListDataStore will close the result set
			ds = reader.buildProcedureListDataStore(rs);
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
		return ds;		
	}
	
	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException
	{
		JdbcProcedureReader reader = new JdbcProcedureReader(this.metaData);
		return reader.getProcedureColumns(aCatalog, aSchema, aProcname);
	}
	
	public StrBuffer getProcedureHeader(String aCatalog, String aSchema, String aProcname, int procType)
	{
		StrBuffer source = new StrBuffer();
		source.append("CREATE ");
		
		String sql = "SELECT routine_type, dtd_identifier " +
             "FROM information_schema.routines " +
		         " WHERE routine_schema like ? " +
		         "  and  routine_name = ? ";
						 
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, (aSchema == null ? "%" : aSchema));
			stmt.setString(2, aProcname);
			rs = stmt.executeQuery();
			String proctype = "PROCEDURE";
			String returntype = "";
			if (rs.next())
			{
				proctype = rs.getString(1);
				returntype = rs.getString(2);
			}		
			source.append(proctype);
			source.append(' ');
			source.append(aProcname);
			source.append(" (");
			
			DataStore ds = this.metaData.getProcedureColumns(aCatalog, aSchema, aProcname);
			int count = ds.getRowCount();
			int added = 0;
			for (int i=0; i < count; i++)
			{
				String vartype = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String ret = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				String name = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
				if (added > 0) source.append(",");
				source.append(ret);
				source.append(' ');
				source.append(name);
				source.append(' ');
				source.append(vartype);
				added ++;
			}
			source.append(")\n");
			if ("FUNCTION".equals(proctype))
			{
				source.append("RETURNS ");
				source.append(returntype);
				source.append('\n');
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MySqlMetadata.getProcedureHeader()", "Error retrieving procedure header", e);
			source = new StrBuffer();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}

}
