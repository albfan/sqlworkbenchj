/*
 * DbObjectCache
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.objectcache;

import java.util.List;
import java.util.Set;
import workbench.db.ColumnIdentifier;
import workbench.db.ProcedureDefinition;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public interface DbObjectCache
{

	void addTable(TableDefinition table);

	void addTableList(DataStore tables, String schema);

	/**
	 * Disposes any db objects held in the cache
	 */
	void clear();

	/**
	 * Return the columns for the given table.
	 *
	 * If the table columns are not in the cache they are retrieved from the database.
	 *
	 * @return the columns of the table.
	 * @see DbMetadata#getTableDefinition(workbench.db.TableIdentifier)
	 */
	List<ColumnIdentifier> getColumns(TableIdentifier tbl);

	/**
	 * Get the procedures the are currently in the cache
	 */
	List<ProcedureDefinition> getProcedures(String schema);

	Set<TableIdentifier> getTables(String schema);

	/**
	 * Get the tables (and views) the are currently in the cache
	 */
	Set<TableIdentifier> getTables(String schema, List<String> type);

	void removeTable(TableIdentifier tbl);

}
