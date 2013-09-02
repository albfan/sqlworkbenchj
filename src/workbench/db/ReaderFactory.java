/*
 * ReaderFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.resource.Settings;

import workbench.db.cubrid.CubridSequenceReader;
import workbench.db.derby.DerbyConstraintReader;
import workbench.db.derby.DerbySequenceReader;
import workbench.db.firebird.FirebirdConstraintReader;
import workbench.db.firebird.FirebirdIndexReader;
import workbench.db.firebird.FirebirdProcedureReader;
import workbench.db.firebird.FirebirdSequenceReader;
import workbench.db.firstsql.FirstSqlConstraintReader;
import workbench.db.h2database.H2ConstraintReader;
import workbench.db.h2database.H2IndexReader;
import workbench.db.h2database.H2SequenceReader;
import workbench.db.hsqldb.HsqlConstraintReader;
import workbench.db.hsqldb.HsqlSequenceReader;
import workbench.db.ibm.Db2ConstraintReader;
import workbench.db.ibm.Db2SequenceReader;
import workbench.db.ibm.InformixSequenceReader;
import workbench.db.ingres.IngresSequenceReader;
import workbench.db.monetdb.MonetDbIndexReader;
import workbench.db.monetdb.MonetDbProcedureReader;
import workbench.db.monetdb.MonetDbSequenceReader;
import workbench.db.mssql.SqlServerConstraintReader;
import workbench.db.mssql.SqlServerIndexReader;
import workbench.db.mssql.SqlServerProcedureReader;
import workbench.db.mssql.SqlServerSequenceReader;
import workbench.db.mssql.SqlServerUtil;
import workbench.db.mysql.MySQLIndexReader;
import workbench.db.mysql.MySqlProcedureReader;
import workbench.db.nuodb.NuoDBSequenceReader;
import workbench.db.oracle.OracleConstraintReader;
import workbench.db.oracle.OracleErrorInformationReader;
import workbench.db.oracle.OracleIndexReader;
import workbench.db.oracle.OracleProcedureReader;
import workbench.db.oracle.OracleSequenceReader;
import workbench.db.postgres.PostgresConstraintReader;
import workbench.db.postgres.PostgresIndexReader;
import workbench.db.postgres.PostgresProcedureReader;
import workbench.db.postgres.PostgresSequenceReader;
import workbench.db.vertica.VerticaSequenceReader;

/**
 * A factory to create instances of the various readers specific for a DBMS.
 *
 * @author Thomas Kellerer
 */
public class ReaderFactory
{
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
		if (meta.getDbId().equals("monetdb") && !Settings.getInstance().getBoolProperty("workbench.db.monetdb.procedurelist.usedriver"))
		{
			return new MonetDbProcedureReader(meta.getWbConnection());
		}
		return new JdbcProcedureReader(meta.getWbConnection());
	}

	public static SequenceReader getSequenceReader(WbConnection con)
	{
		DbMetadata meta = con.getMetadata();
		if (meta.isPostgres())
		{
			return new PostgresSequenceReader(con);
		}
		if (meta.isOracle())
		{
			return new OracleSequenceReader(con);
		}
		if (meta.isHsql())
		{
			return new HsqlSequenceReader(con);
		}
		if (meta.isApacheDerby() && JdbcUtils.hasMinimumServerVersion(con, "10.6"))
		{
			return new DerbySequenceReader(con);
		}
		if (meta.isH2())
		{
			return new H2SequenceReader(con);
		}
		if (meta.isFirebird())
		{
			return new FirebirdSequenceReader(con);
		}
		if (meta.getDbId().startsWith("db2"))
		{
			return new Db2SequenceReader(con, meta.getDbId());
		}
		if (meta.getDbId().equals("cubrid"))
		{
			return new CubridSequenceReader(con);
		}
		if (meta.getDbId().equals("vertica_database"))
		{
			return new VerticaSequenceReader(con);
		}
		if (meta.isSqlServer() && SqlServerUtil.isSqlServer2012(con))
		{
			return new SqlServerSequenceReader(con);
		}
		if (con.getDbId().equals("informix_dynamic_server"))
		{
			return new InformixSequenceReader(con);
		}
		if (con.getDbId().equals("ingres"))
		{
			return new IngresSequenceReader(con);
		}
		if (con.getDbId().equals("nuodb"))
		{
			return new NuoDBSequenceReader(con);
		}
		if (con.getDbId().equals("monetdb"))
		{
			return new MonetDbSequenceReader(con);
		}
		return null;
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
		if (meta.isH2())
		{
			return new H2IndexReader(meta);
		}
		if (meta.isFirebird() && JdbcUtils.hasMinimumServerVersion(meta.getWbConnection(), "2.5"))
		{
			return new FirebirdIndexReader(meta);
		}
		if (meta.isMySql())
		{
			return new MySQLIndexReader(meta);
		}
		if (meta.isSqlServer())
		{
			return new SqlServerIndexReader(meta);
		}
		if (meta.getDbId().equals("monetdb"))
		{
			return new MonetDbIndexReader(meta);
		}
		return new JdbcIndexReader(meta);
	}

	public static ConstraintReader getConstraintReader(DbMetadata meta)
	{
		String dbid = meta.getDbId();
		if (meta.isPostgres())
		{
			return new PostgresConstraintReader(meta.getDbId());
		}
		if (meta.isOracle())
		{
			return new OracleConstraintReader(meta.getDbId());
		}
		if (meta.isHsql())
		{
			return new HsqlConstraintReader(meta.getWbConnection());
		}
		if (meta.isSqlServer())
		{
			return new SqlServerConstraintReader(meta.getWbConnection());
		}
		if (dbid.startsWith("db2"))
		{
			return new Db2ConstraintReader(meta.getWbConnection());
		}
		if (meta.isFirebird())
		{
			return new FirebirdConstraintReader();
		}
		if ("h2".equals(dbid))
		{
			return new H2ConstraintReader();
		}
		if (dbid.startsWith("adaptive_server"))
		{
			return new SybaseConstraintReader(meta.getWbConnection());
		}
		if (meta.isApacheDerby())
		{
			return new DerbyConstraintReader();
		}
		if (dbid.startsWith("firstsql"))
		{
			return new FirstSqlConstraintReader();
		}
		return ConstraintReader.NULL_READER;
	}

	public static ErrorInformationReader getErrorInformationReader(WbConnection conn)
	{
		if (conn == null) return null;
		if (conn.getMetadata().isOracle())
		{
			return new OracleErrorInformationReader(conn);
		}
		return null;
	}
}
