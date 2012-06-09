/*
 * SynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.List;
import workbench.db.derby.DerbySynonymReader;
import workbench.db.ibm.Db2SynonymReader;
import workbench.db.ibm.InformixSynonymReader;
import workbench.db.ingres.IngresSynonymReader;
import workbench.db.mssql.SqlServerSynonymReader;
import workbench.db.oracle.OracleSynonymReader;

/**
 * Read the definition of synonyms from the database.
 *
 * @author Thomas Kellerer
 */
public interface SynonymReader
{
	public static final String SYN_TYPE_NAME = "SYNONYM";

	String getSynonymSource(WbConnection con, String schema, String aSynonym)
			throws SQLException;

	TableIdentifier getSynonymTable(WbConnection con, String schema, String aSynonym)
			throws SQLException;

	List<TableIdentifier> getSynonymList(WbConnection con, String schema, String namePattern)
		throws SQLException;

	public final class Factory
	{
		public static SynonymReader getSynonymReader(WbConnection conn)
		{
			if (conn == null) return null;
			DbMetadata meta = conn.getMetadata();
			if (meta.isOracle())
			{
				return new OracleSynonymReader();
			}
			if (meta.isApacheDerby())
			{
				return new DerbySynonymReader();
			}
			if (meta.isSqlServer() && SqlServerSynonymReader.supportsSynonyms(conn))
			{
				return new SqlServerSynonymReader(meta);
			}
			if (conn.getDbId().startsWith("db2"))
			{
				return new Db2SynonymReader();
			}
			if (conn.getDbId().equals("informix_dynamic_server"))
			{
				return new InformixSynonymReader();
			}
			if (conn.getDbId().equals("ingres"))
			{
				return new IngresSynonymReader();
			}
			return null;
		}
	}
}
