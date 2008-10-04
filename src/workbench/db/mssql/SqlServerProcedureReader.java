/*
 * SqlServerProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A ProcedureReader for Microsoft SQL Server.
 * 
 * @author  support@sql-workbench.net
 */
public class SqlServerProcedureReader
	extends JdbcProcedureReader
{
	private final String GET_PROC_SQL = "{call sp_stored_procedures '%', ?}";

	public SqlServerProcedureReader(WbConnection db)
	{
		super(db);
	}
	
	public StringBuilder getProcedureHeader(String catalog, String schema, String procName, int procType)
	{
		return StringUtil.emptyBuffer();
	}

	/**
	 *	The MS JDBC driver does not return the PROCEDURE_TYPE column correctly
	 *  so we implement it ourselves (MS always returns RESULT which is
	 *  - strictly speaking - true, but as MS still distinguished between
	 *  procedures and functions we need to return this correctly
	 */
	public DataStore getProcedures(String catalog, String owner)
		throws SQLException
	{
		CallableStatement cstmt = this.connection.getSqlConnection().prepareCall(GET_PROC_SQL);
		
		DataStore ds;
		ResultSet rs = null;
		try 
		{
			if (owner == null || "*".equals(owner))
			{
				cstmt.setString(1, "%");
			}
			else
			{
				cstmt.setString(1, owner);
			}
			rs = cstmt.executeQuery();
			ds = buildProcedureListDataStore(this.connection.getMetadata(), false);
			while (rs.next())
			{
				String dbname = rs.getString("PROCEDURE_QUALIFIER");
				String procOwner = rs.getString("PROCEDURE_OWNER");
				String name = rs.getString("PROCEDURE_NAME");
				char procType = 0;
				if (name.indexOf(';') == name.length() - 2)
				{
					procType = name.charAt(name.length() - 1);
					name = name.substring(0, name.length() - 2);
				}
				String remark = rs.getString("REMARKS");
				int type = rs.getShort("PROCEDURE_TYPE");
				Integer iType;
				if (rs.wasNull())
				{
					iType = Integer.valueOf(DatabaseMetaData.procedureResultUnknown);
				}
				else
				{
					if (procType != 0)
					{
						if (procType == '0')
						{
							iType = Integer.valueOf(DatabaseMetaData.procedureReturnsResult);
						}
						else
						{
							iType = Integer.valueOf(DatabaseMetaData.procedureNoResult);
						}
					}
					else
					{
						iType = Integer.valueOf(type);
					}
				}
				int row = ds.addRow();
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, dbname);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, procOwner);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, name);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, iType);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, cstmt);
		}
		return ds;
	}

}
