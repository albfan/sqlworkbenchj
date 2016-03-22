/*
 * DbObjectCache.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.objectcache;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DependencyNode;
import workbench.db.IndexDefinition;
import workbench.db.PkDefinition;
import workbench.db.ProcedureDefinition;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;

import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.WbThread;


/**
 * A wrapper around ObjectCache in order to avoid having to supply the Connection for each call.
 *
 * @author Thomas Kellerer
 */
public class DbObjectCache
{
  private final ObjectCache objectCache;
  private final WbConnection dbConnection;
  private WbThread retrievalThread;

  DbObjectCache(ObjectCache cache, WbConnection connection)
  {
    assert cache != null;
    assert connection != null;
    dbConnection = connection;
    objectCache = cache;
  }

  public List<DependencyNode> getReferencedTables(TableIdentifier table)
  {
    return objectCache.getReferencedTables(dbConnection, table);
  }

  public List<DependencyNode> getReferencingTables(TableIdentifier table)
  {
    return objectCache.getReferencingTables(dbConnection, table);
  }

  public void addReferencedTables(TableIdentifier table, List<DependencyNode> referenced)
  {
    objectCache.addReferencedTables(table, referenced);
  }

  public void addReferencingTables(TableIdentifier table, List<DependencyNode> referencing)
  {
    objectCache.addReferencingTables(table, referencing);
  }

  public void addTable(TableDefinition table)
  {
    objectCache.addTable(table, dbConnection);
  }

  public void addTableList(DataStore tables, String schema)
  {
    objectCache.addTableList(dbConnection, tables, schema);
  }

  public void addProcedureList(DataStore procs, String schema)
  {
    objectCache.addProcedureList(procs, schema);
  }

  /**
   * Removes all entries from this cache.
   */
  public void clear()
  {
    objectCache.clear();
  }

  /**
   * Removes all entries from this cache and deletes any saved cache file.
   *
   * @see #clear()
   * @see ObjectCachePersistence#deleteCacheFile(java.lang.String, java.lang.String)
   */
  public void removeAll()
  {
    clear();
    if (dbConnection != null && dbConnection.getProfile() != null)
    {
      ConnectionProfile prof = dbConnection.getProfile();
      ObjectCachePersistence persistence = new ObjectCachePersistence();
      persistence.deleteCacheFile(prof.getUrl(), prof.getLoginUser());
    }
  }

  public synchronized void addSynonym(TableIdentifier synonym, TableIdentifier baseTable)
  {
    objectCache.addSynonym(synonym, baseTable);
  }

  public TableIdentifier getSynonymTable(TableIdentifier synonym)
  {
    return objectCache.getSynonymTable(dbConnection, synonym);
  }

  public List<ColumnIdentifier> getColumns(TableIdentifier tbl)
  {
    return objectCache.getColumns(dbConnection, tbl);
  }

  public List<ProcedureDefinition> getProcedures(String schema)
  {
    return objectCache.getProcedures(dbConnection, schema);
  }

  public Set<TableIdentifier> getTables(String schema)
  {
    return objectCache.getTables(dbConnection, schema, null);
  }

  public Set<TableIdentifier> getTables(String schema, Collection<String> type)
  {
    return objectCache.getTables(dbConnection, schema, type);
  }

  public void removeEntry(Object obj)
  {
    if (obj instanceof TableIdentifier)
    {
      removeTable((TableIdentifier)obj);
    }
    else if (obj instanceof ProcedureDefinition)
    {
      objectCache.removeProcedure(dbConnection, (ProcedureDefinition)obj);
    }
  }

  public void removeTable(TableIdentifier tbl)
  {
    objectCache.removeTable(dbConnection, tbl);
  }

  public boolean supportsSearchPath()
  {
    return (dbConnection != null && dbConnection.getMetadata().isPostgres());
  }

  public List<String> getSearchPath(String defaultSchema)
  {
    return objectCache.getSearchPath(dbConnection, defaultSchema);
  }

  public TableIdentifier getTable(TableIdentifier table)
  {
    return objectCache.findEntry(dbConnection, table);
  }

  public List<IndexDefinition> getUniqueIndexes(TableIdentifier table)
  {
    return objectCache.getUniqueIndexes(dbConnection, table);
  }

  public PkDefinition getPrimaryKey(TableIdentifier table)
  {
    return objectCache.getPrimaryKey(dbConnection, table);
  }

  public TableIdentifier getOrRetrieveTable(TableIdentifier table)
  {
    TableIdentifier realTable = objectCache.findEntry(dbConnection, table);
    if (realTable == null)
    {
      realTable = dbConnection.getMetadata().searchSelectableObjectOnPath(table);
      if (realTable != null)
      {
        objectCache.addTable(realTable, dbConnection);
      }
    }
    return realTable;
  }

  public void retrieveColumnsInBackground(final List<TableIdentifier> tables)
  {
    if (retrievalThread != null) return;
    if (CollectionUtil.isEmpty(tables)) return;

    retrievalThread = new WbThread(new Runnable()
    {
      @Override
      public void run()
      {
        _retrieveColumnsInBackground(tables);
      }
    }, "ObjectCache Background Retrieval");
    retrievalThread.start();
  }

  private void _retrieveColumnsInBackground(List<TableIdentifier> tables)
  {
    WbConnection conn = null;
    try
    {
      LogMgr.logDebug("DbObjectCache._retrieveColumnsInBackground()", "Retrieving columns for " + tables.size() + " tables");
      conn = ConnectionMgr.getInstance().getConnection(dbConnection.getProfile(), "ObjectCache-Retrieval");
      for (TableIdentifier tbl : tables)
      {
        objectCache.getColumns(conn, tbl);
      }
    }
    catch (Throwable th)
    {
      LogMgr.logWarning("DbObjectCache._retrieveColumnsInBackground()", "Could not retrieve table columns", th);
    }
    finally
    {
      if (conn != null) conn.disconnect();
    }
    retrievalThread = null;
  }
}
