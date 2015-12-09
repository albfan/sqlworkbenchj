/*
 * IndexReader.java
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
	int COLUMN_IDX_TABLE_INDEXLIST_TBL_SPACE = 5;
	int COLUMN_IDX_TABLE_INDEXLIST_IDX_STATUS = 6;

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
	 * This closes any resources opened by {@link #getIndexInfo(workbench.db.TableIdentifier, boolean) }
	 * and should be called after the ResultSet obtained from {@link #getIndexInfo(TableIdentifier, boolean)} has
	 * been processed and closed
	 *
	 * @see #getIndexInfo(workbench.db.TableIdentifier, boolean)
	 */
	void indexInfoProcessed();

	/**
	 * Get the SQL source for all indexes defined in indexList.
	 *
	 * If tableNameToUse is non-null then that name will be used instead
	 * of the name of the TableIdentifier
	 */
	StringBuilder getIndexSource(TableIdentifier table, List<IndexDefinition> indexList);

	/**
	 * Return the CREATE INDEX for a single index
	 * @param table
	 * @param indexDefinition
	 */
	CharSequence getIndexSource(TableIdentifier table, IndexDefinition indexDefinition);

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
	 * Post-Process the index definitions contained in the List.
	 *
	 * This can be used to e.g. retrieve additional index information
	 * that can't be read with getIndexInfo()
	 */
	void processIndexList(Collection<IndexDefinition> indexDefinitions);

	/**
	 * Return the index information for a table as a DataStore. This is
	 * should return the same information as getTableIndexList()
	 *
	 * @param table the table to get the indexes for
	 * @see #getTableIndexList(TableIdentifier)
	 */
	DataStore getTableIndexInformation(TableIdentifier table);

	DataStore fillDataStore(Collection<IndexDefinition> indexes, boolean includeTableName);

	/**
	 * Returns all indexes defined for the given table.
	 *
	 * @param table the table to get the indexes for
	 */
	List<IndexDefinition> getTableIndexList(TableIdentifier table, boolean includeUniqueConstraints);

	/**
	 * Returns a list of unique indexes defined for the given table.
	 *
	 * @param table the table to get the indexes for
	 */
	List<IndexDefinition> getUniqueIndexes(TableIdentifier table);

	/**
	 * Returns the name of the index that supports the Primary Key
	 * If the passed table did not have a PkDefinitioin associated with it,
	 * the returned PK will also be set as the table's PK.
	 *
	 * @see TableIdentifier#setPrimaryKey(workbench.db.PkDefinition)
	 */
	PkDefinition getPrimaryKey(TableIdentifier table);

	/**
	 * Return any addition option that should be appended to a CREATE INDEX statement
	 *
	 * @param index the index to check
	 * @return additionial option
	 *
	 */
	String getIndexOptions(TableIdentifier table, IndexDefinition index);

	boolean supportsTableSpaces();

	/**
	 * Return a list of indexes defined in the system.
	 *
	 * @param catalogPattern   the catalog for which to retrieve the indexes, may be null
	 * @param schemaPattern    the schema for which to retrieve the indexes, may be null
	 * @return a list of indexes
	 *
	 * @see #supportsIndexList()
	 */
	List<IndexDefinition> getIndexes(String catalogPattern, String schemaPattern, String tablePattern, String indexNamePattern);

	/**
	 * Returns true if this IndexReader supports retrieving a list of indexes
	 * without any table parameter.
	 *
	 * @return  true if getInd
	 * @see #getIndexes(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	boolean supportsIndexList();
}
