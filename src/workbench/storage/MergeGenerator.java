/*
 * MergeGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.util.List;
import workbench.db.WbConnection;
import workbench.db.mssql.SqlServerMergeGenerator;
import workbench.db.mysql.MySQLMergeGenerator;
import workbench.db.oracle.OracleMergeGenerator;
import workbench.db.postgres.PostgresMergeGenerator;
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
	 * @param data the data source
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
	public final class Factory
	{
		/**
		 * Create a MergeGenerator for the DBMS identified by the connection.
		 *
		 * @param conn the connection identifying the DBMS
		 * @return the generator, might be null.
		 */
		public static MergeGenerator createGenerator(WbConnection conn)
		{
			return createGenerator(conn == null ? null : conn.getDbId());
		}

		/**
		 * Create a MergeGenerator for the specify DBMS.
		 *
		 * @param dbid the database identifier identifying the DBMS
		 * @return the generator, might be null.
		 */
		public static MergeGenerator createGenerator(String dbid)
		{
			if (dbid == null) return null;
			if ("oracle".equals(dbid))
			{
				return new OracleMergeGenerator();
			}
			if ("postgresql".equals(dbid))
			{
				return new PostgresMergeGenerator();
			}
			if ("mysql".equals(dbid))
			{
				return new MySQLMergeGenerator();
			}
			if ("microsoft_sql_server".equals(dbid))
			{
				return new SqlServerMergeGenerator(dbid);
			}
			return null;
		}

		public static MergeGenerator createGeneratorByType(String type)
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
			if ("sqlserver".equals(type))
			{
				return new SqlServerMergeGenerator("microsoft_sql_server");
			}
			return null;
		}

		public static List<String> getSupportedTypes()
		{
			return CollectionUtil.arrayList("oracle", "postgres", "mysql", "sqlserver", "default");
		}
		
	}
}
