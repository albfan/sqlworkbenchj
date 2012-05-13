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

	String generateMergeStart(RowDataContainer data);

	String addRow(ResultInfo info, RowData row, long rowIndex);

	String generateMergeEnd(RowDataContainer data);

	public final class Factory
	{
		public static MergeGenerator createGenerator(WbConnection conn)
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
