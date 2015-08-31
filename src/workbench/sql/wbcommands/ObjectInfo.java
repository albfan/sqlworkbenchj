/*
 * ObjectInfo.java
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
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbSearchPath;
import workbench.db.DbSettings;
import workbench.db.DependencyNode;
import workbench.db.DropType;
import workbench.db.FKHandler;
import workbench.db.FKHandlerFactory;
import workbench.db.IndexReader;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableDefinition;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.WbConnection;

import workbench.storage.ColumnRemover;
import workbench.storage.DataStore;

import workbench.sql.StatementRunnerResult;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * Retrieves information about a database object.
 * <br>
 * Only the object name is necessary. This class will then search the database
 * for a databse object in the following order:
 * <ol>
 *		<li>Tables and Views</li>
 *		<li>Synonyms</li>
 *		<li>Sequences</li>
 *		<li>Procedures (and functions)</li>
 * </ol>
 *
 * @author Thomas Kellerer
 */
public class ObjectInfo
{
	public ObjectInfo()
	{
	}

	/**
	 * Tries to find the definition of the object identified by the given name.
	 *
	 * If it's a TABLE (or something similar) the result will contain several
	 * DataStores that show the definition of the object.
	 *
	 * If no "selectable" object with that name can be found, the method will first
	 * check if a procedure with that name exists, otherwise it checks for a trigger.
	 *
	 * For a procedure or trigger, the source code will be returned as the message
	 * of the result
	 *
	 * @param connection the database connection
	 * @param objectName the object name to test
	 * @param includeDependencies if true dependent objects (e.g. indexes, constraints) are retrieved as well
	 * @return a StatementRunnerResult with DataStores that contain the definion or the
	 *  source SQL of the object in the message of the result object
	 * @throws SQLException
	 */
	public StatementRunnerResult getObjectInfo(WbConnection connection, String objectName, boolean includeDependencies, boolean includeSource)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		long start = System.currentTimeMillis();
		TableIdentifier dbObject = new TableIdentifier(objectName, connection);

		List<String> searchPath = DbSearchPath.Factory.getSearchPathHandler(connection).getSearchPath(connection, null);
		boolean showSchema = dbObject.getSchema() == null && !searchPath.isEmpty();

		TableIdentifier toDescribe = findObject(connection, dbObject);

		DbSettings dbs = connection.getDbSettings();
		TableIdentifier synonymTarget = null;
		DataStore synonymInfo = null;

		if (toDescribe != null && dbs.isSynonymType(toDescribe.getType()))
		{
			try
			{
				synonymTarget = connection.getMetadata().getSynonymTable(toDescribe);
				if (synonymTarget != null)
				{
					synonymInfo = getPlainSynonymInfo(connection, toDescribe);
					String name = toDescribe.getFullyQualifiedName(connection);
					synonymInfo.setResultName(name + " (" + toDescribe.getObjectType() + ")");
				}
				if (!WbManager.getInstance().isGUIMode() || GuiSettings.showSynonymTargetInDbExplorer())
				{
					toDescribe = synonymTarget;
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving synonym table", e);
			}
		}

		if (toDescribe != null && connection.getMetadata().isSequenceType(toDescribe.getType()))
		{
			try
			{
				SequenceReader seqReader = connection.getMetadata().getSequenceReader();
				if (seqReader != null)
				{
					String schema = toDescribe.getSchema();
					String name = toDescribe.getObjectName();
					String catalog = toDescribe.getCatalog();
					SequenceDefinition seq = seqReader.getSequenceDefinition(catalog, schema, name);
					if (seq != null)
					{
						DataStore ds = seq.getRawDefinition();
						ds.setResultName(seq.getObjectType() + ": " + seq.getObjectName());
						result.addDataStore(ds);

						if (includeSource)
						{
							CharSequence source = seq.getSource();
							if (source == null)
							{
								// source was not build by the reader during initial retrieval
								source = seq.getSource(connection);
							}

							if (source != null)
							{
								String src = source.toString();
								result.addMessage("--------[ BEGIN " + StringUtil.capitalize(seq.getObjectType()) + ": " + seq.getObjectName() + " ]--------");
								result.addMessage(src);
								result.addMessage("--------[ END " + StringUtil.capitalize(seq.getObjectType()) + ": " + seq.getObjectName() + "   ]--------");
								if (StringUtil.isBlank(result.getSourceCommand()))
								{
									result.setSourceCommand(StringUtil.getMaxSubstring(src, 350, "..."));
								}
							}
						}
						return result;
					}
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving sequences", e);
			}
		}

		if (toDescribe == null)
		{
			try
			{
				// No table or something similar found, try to find a procedure with that name
				dbObject.adjustCase(connection);
				ProcedureReader reader = connection.getMetadata().getProcedureReader();
				ProcedureDefinition def = reader.findProcedureByName(dbObject);

				if (def != null)
				{
					String type = StringUtil.capitalize(def.getObjectType());
					String name = def.getFullyQualifiedName(connection);
					CharSequence source = def.getSource(connection);
					result.addMessage("--------[ BEGIN " + type  + ": " + name + " ]--------");
					result.addMessage(source);
					result.addMessage("--------[ END " + type  + ": " + name + "   ]--------");
					result.setSuccess();
					return result;
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving procedures", e);
			}
		}

		if (toDescribe == null)
		{
			// No table, view, procedure, trigger or something similar found
			String msg = ResourceMgr.getFormattedString("ErrTableOrViewNotFound", objectName);
			result.addErrorMessage(msg);
			return result;
		}

		DataStore details = null;
		TableDefinition def = null;
		if (connection.getMetadata().objectTypeCanContainData(toDescribe.getType()))
		{
			def = getTableDefinition(connection, toDescribe);
			details = new TableColumnsDatastore(def);
		}
		else
		{
			DataStore ds = connection.getMetadata().getObjectDetails(toDescribe);
			if (ds != null && ds.getRowCount() > 0)
			{
				details = ds;
			}
		}
		boolean isExtended = connection.getMetadata().isExtendedObject(toDescribe);

		CharSequence source = null;
		String displayName = "";

		if (synonymTarget != null && dbs.isViewType(synonymTarget.getType()))
		{
			if (includeSource) source = connection.getMetadata().getViewReader().getExtendedViewSource(synonymTarget, DropType.none);
			displayName = synonymTarget.getTableExpression(connection);
		}
		else if (toDescribe != null && dbs.isViewType(toDescribe.getType()))
		{
			if (includeSource) source = connection.getMetadata().getViewReader().getExtendedViewSource(def, DropType.none, false);
			displayName = showSchema ? def.getTable().getTableExpression() : def.getTable().getTableExpression(connection);
		}
		else if (isExtended)
		{
			source = connection.getMetadata().getObjectSource(toDescribe);
			displayName = toDescribe.getObjectName() + " (" + toDescribe.getObjectType() + ")";
		}
		else if (toDescribe != null)
		{
			displayName = toDescribe.getTableExpression(connection);
		}

		// in console/batch mode the synonym information should be displayed before the actual table information
		if (!WbManager.getInstance().isGUIMode() && synonymInfo != null)
		{
			result.addDataStore(synonymInfo);
		}

		if (details != null)
		{
			ColumnRemover remover = new ColumnRemover(details);
			DataStore cols = remover.removeColumnsByName(TableColumnsDatastore.JAVA_SQL_TYPE_COL_NAME, "SCALE/SIZE", "PRECISION");
			String fname = showSchema ? toDescribe.getTableExpression() : toDescribe.getTableExpression(connection);
			cols.setResultName(fname + " (" + toDescribe.getObjectType() + ")");
			cols.setGeneratingSql("DESCRIBE " + fname);
			result.setSourceCommand("DESCRIBE " + fname);
			result.addDataStore(cols);
			result.setSuccess();
		}

		// in GUI mode the synonym information should be displayed after the actual table information
		if (WbManager.getInstance().isGUIMode() && synonymInfo != null)
		{
			result.addDataStore(synonymInfo);
		}

		if (source != null)
		{
			result.addMessage("\n--------[ BEGIN " + StringUtil.capitalize(toDescribe.getObjectType()) + ": " +  displayName + " ]--------");
			result.addMessage(source.toString().trim());
			result.addMessage("--------[ END " + StringUtil.capitalize(toDescribe.getObjectType()) + ": " +  displayName + "   ]--------");
			result.setSourceCommand(StringUtil.getMaxSubstring(source.toString(), 350, " ... "));
			result.setSuccess();
		}

		if (toDescribe != null && connection.getMetadata().isTableType(toDescribe.getType()) && includeDependencies)
		{
			try
			{
				IndexReader idxReader = connection.getMetadata().getIndexReader();
				DataStore index = idxReader != null ? idxReader.getTableIndexInformation(toDescribe) : null;
				if (index != null && index.getRowCount() > 0)
				{
					index.setResultName(displayName +  " - " + ResourceMgr.getString("TxtDbExplorerIndexes"));
					result.addDataStore(index);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving index info for " + toDescribe, e);
			}

			try
			{
				TriggerReader trgReader = TriggerReaderFactory.createReader(connection);
				DataStore triggers = trgReader != null ? trgReader.getTableTriggers(toDescribe) : null;
				if (triggers != null && triggers.getRowCount() > 0)
				{
					triggers.setResultName(displayName +  " - " + ResourceMgr.getString("TxtDbExplorerTriggers"));
					result.addDataStore(triggers);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving triggers for " + toDescribe, e);
			}

			if (connection.getDbSettings().objectInfoWithFK())
			{
				try
				{
          retrieveForeignKeys(connection, displayName, toDescribe, result);
				}
				catch (Exception e)
				{
					LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving foreign keys for " + toDescribe, e);
				}
			}
		}

		result.setExecutionDuration(System.currentTimeMillis() - start);
		return result;
	}

  private void retrieveForeignKeys(WbConnection connection, String displayName, TableIdentifier toDescribe, StatementRunnerResult result)
    throws SQLException
  {
    retrieveReferencedTables(connection, displayName, toDescribe, result);
    retrieveReferencingTables(connection, displayName, toDescribe, result);
  }

  private void retrieveReferencingTables(WbConnection connection, String displayName, TableIdentifier toDescribe, StatementRunnerResult result)
    throws SQLException
  {
    DataStore referencedBy  = null;
    boolean useCache = connection.getDbSettings().useCacheForObjectInfo();

    if (useCache)
    {
      boolean busy = connection.isBusy();
      try
      {
        connection.setBusy(false);
        FKHandler fkHandler = FKHandlerFactory.createInstance(connection);

        List<DependencyNode> referencingTables = connection.getObjectCache().getReferencingTables(toDescribe);
        if (CollectionUtil.isNonEmpty(referencingTables))
        {
          referencedBy = TableDependency.createDisplayDataStore(connection, toDescribe, referencingTables, false, fkHandler.supportsStatus());
        }
      }
      finally
      {
        connection.setBusy(busy);
      }
    }
    else
    {
      TableDependency deps = new TableDependency(connection, toDescribe);
      referencedBy = deps.getDisplayDataStore(false);
    }

    if (referencedBy != null && referencedBy.getRowCount() > 0)
    {
      referencedBy.setResultName(displayName + " - " + ResourceMgr.getString("TxtDbExplorerReferencedColumns"));
      result.addDataStore(referencedBy);
    }
  }

  private void retrieveReferencedTables(WbConnection connection, String displayName, TableIdentifier toDescribe, StatementRunnerResult result)
    throws SQLException
  {
    DataStore references = null;
    boolean useCache = connection.getDbSettings().useCacheForObjectInfo();

    if (useCache)
    {
      boolean busy = connection.isBusy();
      try
      {
        connection.setBusy(false);
        FKHandler fkHandler = FKHandlerFactory.createInstance(connection);
        List<DependencyNode> refTables = connection.getObjectCache().getReferencedTables(toDescribe);
        if (CollectionUtil.isNonEmpty(refTables))
        {
          references = TableDependency.createDisplayDataStore(connection, toDescribe, refTables, true, fkHandler.supportsStatus());
        }
      }
      finally
      {
        connection.setBusy(busy);
      }
    }
    else
    {
      TableDependency deps = new TableDependency(connection, toDescribe);
      references = deps.getDisplayDataStore(true);
    }

    if (references != null && references.getRowCount() > 0)
    {
      references.setResultName(displayName + " - " + ResourceMgr.getString("TxtDbExplorerFkColumns"));
      result.addDataStore(references);
    }
  }


  private TableIdentifier findObject(WbConnection connection, TableIdentifier dbObject)
  {
    boolean busy = connection.isBusy();
    boolean useCache = connection.getDbSettings().useCacheForObjectInfo();
    try
    {
      connection.setBusy(false);
      TableIdentifier toDescribe = null;

      if (useCache)
      {
        toDescribe = connection.getObjectCache().getOrRetrieveTable(dbObject);
        if (toDescribe != null)
        {
          return toDescribe;
        }
      }

      List<String> searchPath = DbSearchPath.Factory.getSearchPathHandler(connection).getSearchPath(connection, null);
      boolean searchAllSchemas = connection.getDbSettings().getSearchAllSchemas();

      if (dbObject.getSchema() == null && !searchPath.isEmpty())
      {
        LogMgr.logDebug("ObjectInfo.findObject()", "Searching schemas: " + searchPath + " for " + dbObject.getTableName());
        for (String schema : searchPath)
        {
          TableIdentifier tb = dbObject.createCopy();
          tb.setSchema(schema);
          toDescribe = connection.getMetadata().findObject(tb, true, false);
          if (toDescribe != null) break;
        }
      }
      else
      {
        toDescribe = connection.getMetadata().findObject(dbObject, true, searchAllSchemas);
      }
      return toDescribe;
    }
    finally
    {
      connection.setBusy(busy);
    }
  }

  private TableDefinition getTableDefinition(WbConnection connection, TableIdentifier toDescribe)
    throws SQLException
  {
    TableDefinition def = null;
    boolean useCache = connection.getDbSettings().useCacheForObjectInfo();
    if (useCache)
    {
      boolean busy = connection.isBusy();
      try
      {
        // The object cache will not retrieve anything if the connection is busy
        connection.setBusy(false);
        TableIdentifier tbl = connection.getObjectCache().getOrRetrieveTable(toDescribe);
        List<ColumnIdentifier> columns = connection.getObjectCache().getColumns(tbl);
        def = new TableDefinition(toDescribe, columns);
      }
      finally
      {
        connection.setBusy(busy);
      }
    }
    else
    {
      def = connection.getMetadata().getTableDefinition(toDescribe);
      connection.getObjectCache().addTable(def);
    }
    return def;
  }

	public static DataStore getPlainSynonymInfo(WbConnection dbConnection, TableIdentifier syn)
	{
		String[] columns = {"NAME", "VALUE" };
		int[] types = { Types.VARCHAR, Types.VARCHAR };
		int[] sizes = {20, 20 };
		DataStore ds = new DataStore(columns, types, sizes);

		DbMetadata meta = dbConnection.getMetadata();
		DbSettings dbs = dbConnection.getDbSettings();

		if (meta == null || dbs == null) return ds;

		int row = -1;
		if (dbs.supportsCatalogs())
		{
			String catalogTerm = meta.getCatalogTerm().toUpperCase();
			row = ds.addRow();
			ds.setValue(row, 0, "SYNONYM_" + catalogTerm);
			ds.setValue(row, 1, syn.getCatalog());
		}
		if (dbs.supportsSchemas())
		{
			String schemaTerm = meta.getSchemaTerm().toUpperCase();
			row = ds.addRow();
			ds.setValue(row, 0, "SYNONYM_" + schemaTerm);
			ds.setValue(row, 1, syn.getSchema());
		}
		row = ds.addRow();
		ds.setValue(row, 0, "SYNONYM_NAME");
		ds.setValue(row, 1, syn.getObjectName());

		TableIdentifier baseTable = meta.getSynonymTable(syn);
		if (baseTable != null)
		{
			row = ds.addRow();
			ds.setValue(row, 0, "BASE_TABLE");
			ds.setValue(row, 1, baseTable.getTableExpression(dbConnection));
		}
		ds.resetStatus();
		return ds;
	}
}
