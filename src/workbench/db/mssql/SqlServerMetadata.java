/*
 * MsSqlMetaData.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.mssql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;

/**
 *
 * @author  info@sql-workbench.net
 */
public class SqlServerMetadata
	implements ProcedureReader
{
	private Connection dbConn = null;
	private DbMetadata meta = null;
	private PreparedStatement procStatement = null;

	public SqlServerMetadata(DbMetadata db)
	{
		this.dbConn = db.getSqlConnection();
		this.meta = db;
	}
	
	public StrBuffer getProcedureHeader(String catalog, String schema, String procName)
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
	public DataStore getProcedures(String catalog, String schema)
		throws SQLException
	{
		this.procStatement = this.dbConn.prepareStatement(SqlServerMetadata.GET_PROC_SQL);
		if (schema == null)
		{
			this.procStatement.setString(1, "%");
		}
		else
		{
			this.procStatement.setString(1, schema);
		}
		ResultSet rs = this.procStatement.executeQuery();
		JdbcProcedureReader reader = new JdbcProcedureReader(this.meta);
		return reader.buildProcedureListDataStore(rs);
	}

	public void closeStatement()
	{
		if (this.procStatement != null)
		{
			try
			{
				this.procStatement.close();
			}
			catch (Throwable e)
			{
			}
		}
		this.procStatement = null;
	}

	public void done()
	{
		this.closeStatement();
	}

	private static final String GET_PROC_SQL =
					 "select db_name()  PROCEDURE_CAT, \n" +
           "	  convert(sysname,user_name(o.uid))  PROCEDURE_SCHEM, \n" +
           "	  convert(nvarchar(134),o.name) PROCEDURE_NAME, \n" +
           "	  null, \n" +
           "	  null, \n" +
           "	  null, \n" +
           "	  null REMARKS, \n" +
           "	  case type  \n" +
           "	    when 'P' then 1 \n" +
           "	    when 'FN' then 2 \n" +
					 "      else 0 \n " +
           "	  end PROCEDURE_TYPE \n" +
           "    from  sysobjects o  \n" +
           "    where o.type in ('P', 'FN', 'TF', 'IF') \n" +
           "      and permissions (o.id)&32 <> 0 \n" +
           "      and user_name(o.uid) like ? \n" +
           "	order by 2, 3 \n";
}
