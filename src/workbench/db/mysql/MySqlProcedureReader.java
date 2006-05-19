/*
 * MySqlProcedureReader.java
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
public class MySqlProcedureReader
	extends JdbcProcedureReader
{
	private WbConnection conn;
	
	public MySqlProcedureReader(DbMetadata meta, WbConnection con)
	{
		super(meta);
		this.conn = con;
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
			
			DataStore ds = this.getProcedureColumns(aCatalog, aSchema, aProcname);
			int count = ds.getRowCount();
			int added = 0;
			for (int i=0; i < count; i++)
			{
				String ret = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if (ret.equals("RETURN") || ret.equals("RESULTSET")) continue;
				String vartype = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
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
