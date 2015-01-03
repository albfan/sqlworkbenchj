/*
 * MergeGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.storage;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.resource.Settings;

import workbench.db.AnsiSQLMergeGenerator;
import workbench.db.WbConnection;
import workbench.db.firebird.Firebird20MergeGenerator;
import workbench.db.firebird.Firebird21MergeGenerator;
import workbench.db.h2database.H2MergeGenerator;
import workbench.db.hsqldb.HsqlMergeGenerator;
import workbench.db.ibm.Db2MergeGenerator;
import workbench.db.mssql.SqlServerMergeGenerator;
import workbench.db.mysql.MySQLMergeGenerator;
import workbench.db.oracle.OracleMergeGenerator;
import workbench.db.postgres.PostgresMergeGenerator;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public interface MergeGenerator
{
	/**
	 * Generate a single MERGE statment (or something equivalent depending on the DBMS)
	 * based on the passed data.
	 *
	 * Depending on the capabilities of the DBMS, the result might be one
	 * statement for each row or one statement for all rows.
	 *
	 * @param data       the data source
	 *
	 * @return one or more SQL statements to merge the data into an existing table.
	 *         might be null (e.g. if no update table is present)
	 */
	String generateMerge(RowDataContainer data);

	/**
	 * Generate the start of a MERGE statement.
	 * <br/>
	 * The complete MERGE statement needs to be assembled using generateMergeStart(), addRow() and generateMergeEnd().
	 *
	 * @param data the data source
	 * @return  the start of a MERGE statement
	 */
	String generateMergeStart(RowDataContainer data);

	/**
	 * Generate the SQL for a single row in a MERGE statement.
	 *
	 * <br/>
	 * The complete MERGE statement needs to be assembled using generateMergeStart(), addRow() and generateMergeEnd().
	 *
	 * @param info      the metadata of the result
	 * @param row       the data
	 * @param rowIndex  the rowIndex in the result
	 * @return  the SQL for a single row inside a MERGE statement.
	 */
	String addRow(ResultInfo info, RowData row, long rowIndex);

	/**
	 * Generate the end of a MERGE statement.
	 * <br/>
	 * The complete MERGE statement needs to be assembled using generateMergeStart(), addRow() and generateMergeEnd().
	 *
	 * @param data the data source
	 * @return  the end of a MERGE statement
	 */
	String generateMergeEnd(RowDataContainer data);


	/**
	 * The factory go create MergeGenerator instances depending on the DBMS.
	 */
	class Factory
	{
		private static final Map<String, String> DBID_TO_TYPE_MAP = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
		static
		{
			DBID_TO_TYPE_MAP.put("postgresql", "postgres");
			DBID_TO_TYPE_MAP.put("h2database", "h2");
			DBID_TO_TYPE_MAP.put("microsoft_sql_server", "sqlserver");
			DBID_TO_TYPE_MAP.put("db2i", "db2");
			DBID_TO_TYPE_MAP.put("db2h", "db2");
			DBID_TO_TYPE_MAP.put("hsql_database_engine", "hsqldb");
		}

		/**
		 * Create a MergeGenerator for the DBMS identified by the connection.
		 *
		 * @param conn the connection identifying the DBMS
		 * @return the generator, never null (defaults to AnsiSQLMergeGenerator)
		 */
		public static MergeGenerator createGenerator(WbConnection conn)
		{
			if (conn == null) return new AnsiSQLMergeGenerator();
			return createGenerator(conn.getDbId());
		}

		/**
		 * Create a MergeGenerator for the specify DBMS.
		 *
		 * @param type the database identifier or the "generator type"
		 * @return the generator, never null (defaults to AnsiSQLMergeGenerator)
		 */
		public static MergeGenerator createGenerator(String type)
		{
			if (type == null) return null;

			if ("oracle".equals(type))
			{
				return new OracleMergeGenerator();
			}

			if (type.startsWith("postgres"))
			{
				return new PostgresMergeGenerator();
			}

			if ("mysql".equals(type))
			{
				return new MySQLMergeGenerator();
			}

			if ("microsoft_sql_server".equals(type) || "sqlserver".equals(type))
			{
				return new SqlServerMergeGenerator(type);
			}

			if (type.startsWith("hsql"))
			{
				return new HsqlMergeGenerator();
			}

			if (type.startsWith("db2"))
			{
				return new Db2MergeGenerator();
			}

			if (type.startsWith("h2"))
			{
				return new H2MergeGenerator();
			}

			if ("firebird".equals(type))
			{
				if (Settings.getInstance().getBoolProperty("workbench.db.firebird.mergegenerator.use.ansi", true))
				{
					return new Firebird21MergeGenerator();
				}
				return new Firebird20MergeGenerator();
			}

			return new AnsiSQLMergeGenerator();
		}

		public static List<String> getSupportedTypes()
		{
			return CollectionUtil.arrayList("ansi", "db2", "firebird", "h2", "hsqldb", "mysql", "oracle", "postgres", "sqlserver");
		}

		public static String getTypeForDBID(String dbid)
		{
			if (dbid == null) return "ansi";
			String type = DBID_TO_TYPE_MAP.get(dbid);
			if (type == null) return "ansi";
			return type;
		}
	}
}
