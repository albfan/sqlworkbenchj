/*
 * IndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;

/**
 *
 * @author support@sql-workbench.net
 */
public interface IndexReader
{
	/**
	 * Replacement for the JDBC's getIndexInfo method
	 */
	ResultSet getIndexInfo(TableIdentifier table, boolean unique)
		throws SQLException;
	
	/**
	 * This closes any resources opened by {@link getIndexInfo(TableIdentifier, boolean)}
	 * and should be called after the ResultSet obtained from getIndexInfo() has
	 * been processed and closed
	 */
	void indexInfoProcessed();

	/**
	 * Get the SQL source for all indexes defined in indexDefinition.
	 * If tableNameToUse is non-null then that name will be used instead
	 * of the name of the TableIdentifier
	 */
	
	StrBuffer getIndexSource(TableIdentifier table, DataStore indexDefinition, String tableNameToUse);
	/**
	 * 	Build the SQL statement to create an Index on the given table.
	 * 	@param aTable - The table name for which the index should be constructed
	 * 	@param indexName - The name of the Index
	 * 	@param unique - Should the index be unique
	 *  @param columnList - The columns that should build the index
	 */
	String buildCreateIndexSql(TableIdentifier table, String indexName, boolean unique, String[] columnList);
}
