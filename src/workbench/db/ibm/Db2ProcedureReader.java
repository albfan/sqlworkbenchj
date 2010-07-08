/*
 * Db2ProcedureReader
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.ibm;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.JdbcProcedureReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * DB2's JDBC driver only returns procedures, not functions.
 * <br/>
 * This class uses its own SQL Statement to retrieve both objects from the database.
 *
 * @author Thomas Kellerer
 */
public class Db2ProcedureReader
	extends JdbcProcedureReader
{
	private boolean useJDBC;

	public Db2ProcedureReader(WbConnection conn)
	{
		super(conn);
		useJDBC = conn.getMetadata().getDbId().equals("db2i");
	}

	@Override
	public DataStore getProcedures(String catalog, String schemaPattern, String namePattern)
		throws SQLException
	{
		if (useJDBC)
		{
			return super.getProcedures(catalog, schemaPattern, namePattern);
		}

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			String sql = getSQL(schemaPattern, namePattern);
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			DataStore ds = fillProcedureListDataStore(rs);
			return ds;
		}
		catch (Exception e)
		{
			LogMgr.logError("Db2ProcedureReader.getProcedures()", "Error retrieving procedures. Using JDBC...", e);
			useJDBC = true;
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
//		if (this.connection.getMetadata().getDbId().equals("db2i"))
//		{
//			sql.append("SELECT '' as PROCEDURE_CAT,  \n" +
//             "       ROUTINE_SCHEMA  as PROCEDURE_SCHEM, \n" +
//             "       ROUTINE_NAME as PROCEDURE_NAME, \n" +
//             "       LONG_COMMENT AS REMARKS, \n" +
//             "       CASE  \n" +
//             "         WHEN RESULT_SETS > 0 THEN " + DatabaseMetaData.procedureReturnsResult + "  \n" +
//             "         ELSE " + DatabaseMetaData.procedureNoResult + "  \n" +
//             "       END as PROCEDURE_TYPE \n" +
//             "FROM qsys2.sysprocs ");
//
//			appendCondition(sql, "ROUTINE_SCHEMA", schemaPattern);
//			appendCondition(sql, "ROUTINE_NAME", namePattern);
//		}

		if (this.connection.getMetadata().getDbId().equals("db2h"))
		{
			// DB Host
			sql.append("SELECT '' as PROCEDURE_CAT,  \n" +
             "       schema  as PROCEDURE_SCHEM, \n" +
             "       name as PROCEDURE_NAME, \n" +
             "       remarks, \n" +
             "       CASE  \n" +
             "         WHEN routinetype = 'F' THEN " + DatabaseMetaData.procedureReturnsResult + "  \n" +
             "         ELSE " + DatabaseMetaData.procedureNoResult + "  \n" +
             "       END as PROCEDURE_TYPE \n" +
             "FROM SYSIBM.SYSROUTINES \n" +
             "WHERE routinetype in ('F', 'P') \n" +
             "AND origin in ('Q', 'U') ");

			appendCondition(sql, "schema", schemaPattern);
			appendCondition(sql, "name", namePattern);
		}
		else
		{
			// DB LUW
			sql.append("SELECT '' as PROCEDURE_CAT,  \n" +
					 "       routineschema as PROCEDURE_SCHEM, \n" +
					 "       routinename as PROCEDURE_NAME, \n" +
					 "       remarks, \n" +
					 "       CASE  \n" +
					 "         WHEN routinetype = 'F' THEN " + DatabaseMetaData.procedureReturnsResult + "  \n" +
					 "         ELSE " + DatabaseMetaData.procedureNoResult + "  \n" +
					 "       END as PROCEDURE_TYPE \n" +
					 "FROM syscat.routines \n" +
					 "WHERE routinetype in ('F', 'P') \n" +
					 "AND origin in ('Q', 'U') \n");
			
			appendCondition(sql, "routineschema", schemaPattern);
			appendCondition(sql, "routinename", namePattern);
		}
		return sql.toString();
	}

	private void appendCondition(StringBuilder baseSql, String column, String value)
	{
		if (StringUtil.isBlank(value)) return;
		baseSql.append(" AND ");
		baseSql.append(column);

		if (value.indexOf('%') > -1)
		{
			baseSql.append(" LIKE '");
			baseSql.append(value);
		}
		else
		{
			baseSql.append(" = '");
			baseSql.append(value);
		}
		baseSql.append("'");
	}
}
