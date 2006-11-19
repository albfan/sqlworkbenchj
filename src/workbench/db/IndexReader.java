/*
 * IndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import workbench.storage.DataStore;

/**
 *
 * @author support@sql-workbench.net
 */
public interface IndexReader
{
	/**
	 * Replacement for the JDBC's getIndexInfo method.
	 * After the returned ResultSet has been processed, indexInfoProcessed() has to be called!
	 *
	 * @see #indexInfoProcessed()
	 */
	ResultSet getIndexInfo(TableIdentifier table, boolean unique)
		throws SQLException;
	
	/**
	 * This closes any resources opened by {@link #getIndexSource(workbench.db.TableIdentifier, workbench.storage.DataStore, String)}
	 * and should be called after the ResultSet obtained from {@link #getIndexInfo(TableIdentifier, boolean)} has
	 * been processed and closed
	 *
	 * @see #getIndexInfo(workbench.db.TableIdentifier, boolean)
	 */
	void indexInfoProcessed();

	/**
	 * Get the SQL source for all indexes defined in indexDefinition.
	 * If tableNameToUse is non-null then that name will be used instead
	 * of the name of the TableIdentifier
	 */
	StringBuffer getIndexSource(TableIdentifier table, DataStore indexDefinition, String tableNameToUse);
	
	/**
	 * 	Build the SQL statement to create an Index on the given table.
	 * 	@param table - The table for which the index should be constructed
	 * 	@param indexName - The name of the Index
	 * 	@param unique - Should the index be unique
	 *  @param columnList - The columns that should build the index
	 */
	String buildCreateIndexSql(TableIdentifier table, String indexName, boolean unique, String[] columnList);
	
	/**
	 * Post-Process the index definitions contained in the List
	 * This can be used to e.g. retrieve additional index information
	 * that can't be read with getIndexInfo()
	 */
	void processIndexList(TableIdentifier table, Collection indexDefinitions);
}
