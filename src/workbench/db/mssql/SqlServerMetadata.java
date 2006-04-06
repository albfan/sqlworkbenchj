/*
 * SqlServerMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;

/**
 *
 * @author  support@sql-workbench.net
 */
public class SqlServerMetadata
	implements ProcedureReader
{
	private Connection dbConn = null;
	private DbMetadata meta = null;

	public SqlServerMetadata(DbMetadata db)
	{
		this.dbConn = db.getSqlConnection();
		this.meta = db;
	}
	
	public StrBuffer getProcedureHeader(String catalog, String schema, String procName, int procType)
	{
		return StrBuffer.EMPTY_BUFFER;
	}

	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException
	{
		JdbcProcedureReader reader = new JdbcProcedureReader(this.meta);
		return reader.getProcedureColumns(aCatalog, aSchema, aProcname);
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
		//PreparedStatement stmt = this.dbConn.prepareStatement(GET_PROC_SQL);
		CallableStatement cstmt = this.dbConn.prepareCall(GET_PROC_SQL);
		
		DataStore ds = null;
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
			ds = JdbcProcedureReader.buildProcedureListDataStore(this.meta);
			while (rs.next())
			{
				String dbname = rs.getString("PROCEDURE_QUALIFIER");
				String procOwner = rs.getString("PROCEDURE_OWNER");
				String name = rs.getString("PROCEDURE_NAME");
				String remark = rs.getString("REMARKS");
				short type = rs.getShort("PROCEDURE_TYPE");
				Integer iType = null;
				if (rs.wasNull())
				{
					iType = new Integer(DatabaseMetaData.procedureResultUnknown);
				}
				else
				{
					iType = new Integer(type);
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

	private final String GET_PROC_SQL = "{call sp_stored_procedures '%', ?}";
	
//	private final String GET_PROC_SQL =
//					 "select db_name()  PROCEDURE_CAT, \n" +
//           "	  convert(sysname,user_name(o.uid))  PROCEDURE_SCHEM, \n" +
//           "	  convert(nvarchar(134),o.name) PROCEDURE_NAME, \n" +
//           "	  null, \n" +
//           "	  null, \n" +
//           "	  null, \n" +
//           "	  null REMARKS, \n" +
//           "	  case type  \n" +
//           "	    when 'P' then 1 \n" +
//           "	    when 'FN' then 2 \n" +
//					 "      else 0 \n " +
//           "	  end PROCEDURE_TYPE \n" +
//           "    from  sysobjects o  \n" +
//           "    where o.type in ('P', 'FN', 'TF', 'IF') \n" +
//           "      and permissions (o.id)&32 <> 0 \n" +
//           "      and user_name(o.uid) like ? \n" +
//           "	order by 2, 3 \n";
}
