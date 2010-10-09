/*
 * IndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import workbench.storage.DataStore;

/**
 * An interface to retrieve index information for a database table.
 *
 * A default implementation uses the JDBC API to retrieve this information.
 *
 * Additional implementations are available to retrieve DBMS specific index information
 *
 * @author Thomas Kellerer
 */
public interface IndexReader
{
	int COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME = 0;
	int COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG = 1;
	int COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG = 2;
	int COLUMN_IDX_TABLE_INDEXLIST_COL_DEF = 3;
	int COLUMN_IDX_TABLE_INDEXLIST_TYPE = 4;

	/**
	 * Replacement for the JDBC's getIndexInfo method.
	 * After the returned ResultSet has been processed, indexInfoProcessed() has to be called!
	 * The TYPE column may not be an integer value but a String value that indicates
	 * the type of the index in plain text. So the column "TYPE" from the result set
	 * should always be accessed using getObject("TYPE")
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
	 *
	 * If tableNameToUse is non-null then that name will be used instead
	 * of the name of the TableIdentifier
	 */
	StringBuilder getIndexSource(TableIdentifier table, DataStore indexDefinition, String tableNameToUse);

	/**
	 * Return the CREATE INDEX for a single index
	 * @param table
	 * @param indexDefinition
	 * @param tableNameToUse
	 */
	CharSequence getIndexSource(TableIdentifier table, IndexDefinition indexDefinition, String tableNameToUse);

	/**
	 * 	Build a SQL statement (from scratch) to create a new index on the given table.
	 *
	 * 	@param table - The table for which the index should be constructed
	 * 	@param indexName - The name of the Index
	 * 	@param unique - Should the index be unique
	 *  @param columnList - The columns that should build the index
	 */
	String buildCreateIndexSql(TableIdentifier table, String indexName, boolean unique, List<IndexColumn> columnList);

	/**
	 * Post-Process the index definitions contained in the List
	 * This can be used to e.g. retrieve additional index information
	 * that can't be read with getIndexInfo()
	 */
	void processIndexList(TableIdentifier table, Collection<IndexDefinition> indexDefinitions);

	/**
	 * Return the index information for a table as a DataStore. This is
	 * should return the same information as getTableIndexList()
	 *
	 * @param table the table to get the indexes for
	 * @see #getTableIndexList(TableIdentifier)
	 */
	DataStore getTableIndexInformation(TableIdentifier table);

	/**
	 * Returns a list of indexes defined for the given table
	 * @param table the table to get the indexes for
	 */
	Collection<IndexDefinition> getTableIndexList(TableIdentifier table);

	/**
	 * Returns the name of the index that supports the Primary Key
	 */
	String getPrimaryKeyIndex(TableIdentifier table);

	/**
	 * For non-standard index type, return the source for this index
	 *
	 * @param table
	 * @param definition
	 * @return the CREATE INDEX statement
	 */
	//String getIndexSourceForType(TableIdentifier table, IndexDefinition definition);

	/**
	 * Return any addition option that should be appended to a CREATE INDEX statement
	 * @param index
	 * @return
	 */
	String getIndexOptions(IndexDefinition index);
}
