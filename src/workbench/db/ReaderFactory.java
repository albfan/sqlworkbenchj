/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */

package workbench.db;

import workbench.db.derby.DerbyConstraintReader;
import workbench.db.firebird.FirebirdConstraintReader;
import workbench.db.firebird.FirebirdProcedureReader;
import workbench.db.firstsql.FirstSqlConstraintReader;
import workbench.db.h2database.H2ConstraintReader;
import workbench.db.hsqldb.HsqlConstraintReader;
import workbench.db.ibm.Db2ConstraintReader;
import workbench.db.mssql.SqlServerConstraintReader;
import workbench.db.mssql.SqlServerProcedureReader;
import workbench.db.mysql.MySqlProcedureReader;
import workbench.db.oracle.OracleConstraintReader;
import workbench.db.oracle.OracleIndexReader;
import workbench.db.oracle.OracleProcedureReader;
import workbench.db.postgres.PostgresConstraintReader;
import workbench.db.postgres.PostgresIndexReader;
import workbench.db.postgres.PostgresProcedureReader;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class ReaderFactory
{
	public static final Object READER_LOCK = new Object();

	public static ProcedureReader getProcedureReader(DbMetadata meta)
	{
		if (meta.isOracle())
		{
			return new OracleProcedureReader(meta.getWbConnection());
		}

		if (meta.isPostgres())
		{
			return new PostgresProcedureReader(meta.getWbConnection());
		}

		if (meta.isFirebird())
		{
			return new FirebirdProcedureReader(meta.getWbConnection());
		}

		if (meta.isSqlServer())
		{
			boolean useJdbc = Settings.getInstance().getBoolProperty("workbench.db.mssql.usejdbcprocreader", false);
			if (!useJdbc)
			{
				return new SqlServerProcedureReader(meta.getWbConnection());
			}
		}
		if (meta.isMySql())
		{
			return new MySqlProcedureReader(meta.getWbConnection());
		}
	return new JdbcProcedureReader(meta.getWbConnection());
	}
	
	public static IndexReader getIndexReader(DbMetadata meta)
	{
		if (meta.isOracle())
		{
			return new OracleIndexReader(meta);
		}
		if (meta.isPostgres())
		{
			return new PostgresIndexReader(meta);
		}
		return new JdbcIndexReader(meta);
	}
	
	public static ConstraintReader getConstraintReader(DbMetadata meta)
	{
		String dbid = meta.getDbId();
		if (meta.isPostgres())
		{
			return new PostgresConstraintReader();
		}
		if (meta.isOracle())
		{
			return new OracleConstraintReader();
		}
		if (meta.isHsql())
		{
			return new HsqlConstraintReader(meta.getSqlConnection());
		}
		if (meta.isSqlServer())
		{
			return new SqlServerConstraintReader();
		}
		if (dbid.startsWith("db2"))
		{
			return new Db2ConstraintReader();
		}
		if ("firebird".equals(dbid))
		{
			return new FirebirdConstraintReader();
		}
		if ("h2".equals(dbid))
		{
			return new H2ConstraintReader();
		}
		if (dbid.startsWith("adaptive_server"))
		{
			return new ASAConstraintReader();
		}
		if (meta.isApacheDerby())
		{
			return new DerbyConstraintReader();
		}
		if (meta.isFirstSql())
		{
			return new FirstSqlConstraintReader();
		}
		
		return null;
	}
}
