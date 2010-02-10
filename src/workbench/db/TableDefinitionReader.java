/*
 * TableDefinitionReader
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
public interface TableDefinitionReader
{
	/**
	 * Return the definition of the given table.
	 * <br/>
	 * To display the columns for a table in a DataStore create an
	 * instance of {@link TableColumnsDatastore}.
	 *
	 * @param toRead The table for which the definition should be retrieved
	 * @param enhancer The ColumnDefinitionEnhancer to post-process the column list. May be null
	 * 
	 * @throws SQLException
	 * @return the definition of the table. If toRead was null, null is returned
	 * @see TableColumnsDatastore
	 */
	List<ColumnIdentifier> getTableColumns(TableIdentifier toRead, List<String> primaryKeyColumns, WbConnection dbConnection, DataTypeResolver typeResolver)
		throws SQLException;
}
