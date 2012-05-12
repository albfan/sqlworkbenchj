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
import workbench.db.oracle.OracleMergeGenerator;
import workbench.db.postgres.PostgresMergeGenerator;

/**
 *
 * @author Thomas Kellerer
 */
public interface MergeGenerator
{
	/**
	 * Generate MERGE statments (or something equivalent depending on the DBMS)
	 * based on the data in the DataStore.
	 *
	 * Depending on the capabilities of the DBMS, the result might be one
	 * statement for each row or one statement for all rows.
	 *
	 * @param data       the data source
	 * @param rows       the rows to generate the merge for. If null, it will be generated for all rows.
	 * @param chunksize  the number of rows to combine into one statement
	 *                   -1 let the generator decide
	 *                    0 everything in one statement
	 *                   >0 one statement per chunkSize rows
	 *                   the generator is free to ignore the chunksize parameter
	 *
	 * @return one or more SQL statements to merge the data into an existing table.
	 *         might be null (e.g. if no update table is present)
	 */
	List<String> generateMerge(DataStore data, int[] rows, int chunkSize);

	public final class Factory
	{
		public static MergeGenerator getGenerator(WbConnection conn)
		{
			if (conn == null) return null;
			if (conn.getMetadata().isOracle())
			{
				return new OracleMergeGenerator(conn);
			}
			if (conn.getMetadata().isPostgres())
			{
				return new PostgresMergeGenerator(conn);
			}
			return null;
		}
	}
}
