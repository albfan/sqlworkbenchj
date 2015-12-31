/*
 * SynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
	String SYN_TYPE_NAME = "SYNONYM";

  boolean supportsReplace(WbConnection con);
  
	String getSynonymTypeName();

	String getSynonymSource(WbConnection con, String catalog, String schema, String aSynonym)
			throws SQLException;

	TableIdentifier getSynonymTable(WbConnection con, String catalog, String schema, String aSynonym)
			throws SQLException;

	List<TableIdentifier> getSynonymList(WbConnection con, String catalogPattern, String schemaPattern, String namePattern)
		throws SQLException;

	class Factory
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
