/*
 * TriggerReader.java
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

import java.sql.SQLException;
import java.util.List;

import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public interface TriggerReader {
	/**
	 * The column index in the DataStore returned by getTableTriggers which identifies
	 * the name of the trigger.
	 */
	int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME = 0;

	/**
	 * The column index in the DataStore returned by getTableTriggers which identifies
	 * the type (INSERT, UPDATE etc) of the trigger.
	 */
	int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE = 1;

	/**
	 * The column index in the DataStore returned by getTableTriggers which identifies
	 * the event (before, after) of the trigger.
	 */
	int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT = 2;

	/**
	 * The column index in the DataStore returned by getTableTriggers which identifies
	 * the table of the trigger.
	 */
	int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TABLE = 3;

	/**
	 * The column index in the DataStore returned by getTableTriggers which identifies
	 * the comment of the trigger.
	 */
	int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_COMMENT = 4;

	/**
	 * The column index in the DataStore returned by getTableTriggers which identifies
	 * the comment of the trigger.
	 */
	int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_STATUS = 5;

	String TRIGGER_COMMENT_COLUMN = "REMARKS";
	String TRIGGER_EVENT_COLUMN = "EVENT";
	String TRIGGER_NAME_COLUMN = "TRIGGER";
	String TRIGGER_TABLE_COLUMN = "TABLE";
	String TRIGGER_TYPE_COLUMN = "TYPE";
	String TRIGGER_STATUS_COLUMN = "STATUS";

	String[] LIST_COLUMNS = {
		TriggerReader.TRIGGER_NAME_COLUMN,
		TriggerReader.TRIGGER_TYPE_COLUMN,
		TriggerReader.TRIGGER_EVENT_COLUMN,
		TriggerReader.TRIGGER_TABLE_COLUMN,
		TriggerReader.TRIGGER_COMMENT_COLUMN,
		TriggerReader.TRIGGER_STATUS_COLUMN
	};

	TriggerDefinition findTrigger(String catalog, String schema, String name)
		throws SQLException;

	/**
	 * Return the list of defined triggers for the given table.
	 */
	DataStore getTableTriggers(TableIdentifier table)
		throws SQLException;

	List<TriggerDefinition> getTriggerList(String catalog, String schema, String baseTable)
		throws SQLException;

	String getTriggerSource(TriggerDefinition trigger, boolean includeDependencies)
		throws SQLException;

	/**
	 * Retrieve the SQL Source of the given trigger.
	 *
	 * @param aCatalog      The catalog in which the trigger is defined. This should be null if the DBMS does not support catalogs
	 * @param aSchema       The schema in which the trigger is defined. This should be null if the DBMS does not support schemas
	 * @param aTriggername  the name of the trigger
	 * @param triggerTable  the table for which the trigger is defined
	 * @param trgComment    the comment for the trigger
	 * @param includeDependencies  if true dependent objects should be included in the source (e.g. the trigger function in Postgres)
	 * @throws SQLException
	 * @return the trigger source
	 */
	String getTriggerSource(String aCatalog, String aSchema, String aTriggername, TableIdentifier triggerTable, String trgComment, boolean includeDependencies)
		throws SQLException;

	/**
	 * Retriev any additional source that is needed for the specified trigger.
	 *
	 * @param triggerCatalog The catalog in which the trigger is defined. This should be null if the DBMS does not support catalogs
	 * @param triggerSchema The schema in which the trigger is defined. This should be null if the DBMS does not support schemas
	 * @param triggerName the name of the trigger
	 * @param triggerTable the table for which the trigger is defined
	 * @return source of additional DB objects.
	 */
	CharSequence getDependentSource(String triggerCatalog, String triggerSchema, String triggerName, TableIdentifier triggerTable)
		throws SQLException;

	/**
	 * Return a list of triggers available in the given schema.
	 */
	DataStore getTriggers(String catalog, String schema)
		throws SQLException;

	/**
	 * Checks if the DBMS supports triggers on views.
	 */
	boolean supportsTriggersOnViews();
}
