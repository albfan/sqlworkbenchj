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
	 * @param data the data source
	 * @param rows the rows to generate the merge for. If null, it will be generated for all rows.
	 * @param chunksize the number of rows to combine into one statement
	 *                  -1 let the generator decide
	 *                   0 everything in one statement
	 *                  >0 one statement per chunkSize rows
	 * @return one or more SQL statements to merge the data into an existing table.
	 *         might be null (e.g. if no update table is present)
	 */
	List<String> generateMerge(DataStore data, int[] rows, int chunkSize);
}
