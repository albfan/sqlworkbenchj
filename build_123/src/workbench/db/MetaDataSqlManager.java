/*
 * MetaDataSqlManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.util.NumberStringCache;
import workbench.util.VersionNumber;
import workbench.util.WbPersistence;

/**
 * @author  Thomas Kellerer
 */
public class MetaDataSqlManager
{
  public static final String FQ_TABLE_NAME_PLACEHOLDER = "%fq_table_name%";
  public static final String FQ_NAME_PLACEHOLDER = "%fq_name%";
  public static final String NAME_PLACEHOLDER = "%name%";
  public static final String SCHEMA_NAME_PLACEHOLDER = "%schema_name%";
  public static final String CATALOG_NAME_PLACEHOLDER = "%catalog_name%";
  public static final String TABLE_NAME_PLACEHOLDER = "%table_name%";
  public static final String TABLE_EXPRESSION_PLACEHOLDER = "%table_expression%";
  public static final String SPECIFIC_NAME_PLACEHOLDER = "%specific_name%";
  public static final String INDEX_TYPE_PLACEHOLDER = "%indextype%";
  public static final String INDEX_NAME_PLACEHOLDER = "%indexname%";
  public static final String FQ_INDEX_NAME_PLACEHOLDER = "%fq_index_name%";
  public static final String PK_NAME_PLACEHOLDER = "%pk_name%";
  public static final String PK_INLINE_DEFINITION = "%pk_definition%";
  public static final String UNIQUE_PLACEHOLDER = "%unique_key%";
  public static final String IDX_DIRECTION_PLACEHOLDER = "%idx_dir%";
  public static final String COLUMN_NAME_PLACEHOLDER = "%column_name%";
  public static final String COLUMN_LIST_PLACEHOLDER = "%columnlist%";
  public static final String CONSTRAINT_NAME_PLACEHOLDER = "%constraint_name%";
  public static final String FK_TARGET_TABLE_PLACEHOLDER = "%targettable%";
  public static final String FK_TARGET_COLUMNS_PLACEHOLDER = "%targetcolumnlist%";
  public static final String FK_UPDATE_RULE = "%fk_update_rule%";
  public static final String FK_DELETE_RULE = "%fk_delete_rule%";
  public static final String DEFERRABLE = "%deferrable%";
  public static final String DEFERRED = "%deferred%";
  public static final String GENERAL_SQL = "All";

  public static final String CONS_ENABLED = "%enabled%";
  public static final String CONS_VALIDATED = "%validated%";

  private final String productName;
  private final String dbIdKey;
  private int majorVersion = -1;
  private int minorVersion = -1;

  private GetMetaDataSql procedureSource;
  private GetMetaDataSql viewSource;
  private GetMetaDataSql listTrigger;
  private GetMetaDataSql triggerSource;
  private GetMetaDataSql listIndexes;

  private final Object LOCK = new Object();

  public MetaDataSqlManager(String product, String dbId, VersionNumber version)
  {
    this.dbIdKey = "$dbid:" + dbId;
    if (product.toLowerCase().startsWith("firebird"))
    {
      // Jaybird 2.x reports the Firebird version in the productname.
      // To ease the DBMS handling we'll simply use Firebird as the product name here
      productName = "Firebird";
    }
    else
    {
      productName = product;
    }
    if (version != null)
    {
      this.majorVersion = version.getMajorVersion();
      this.minorVersion = version.getMinorVersion();
    }
  }

  public String getProductName()
  {
    return this.productName;
  }

  public GetMetaDataSql getListIndexesSql()
  {
    synchronized (LOCK)
    {
      if (this.listIndexes == null)
      {
        HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ListIndexStatements.xml");
        this.listIndexes = getEntry(sql, "index list");
      }
      return this.listIndexes;
    }
  }

  public GetMetaDataSql getProcedureSourceSql()
  {
    synchronized (LOCK)
    {
      if (this.procedureSource == null)
      {
        HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ProcSourceStatements.xml");
        this.procedureSource = getEntry(sql, "procedure source");
      }
      return this.procedureSource;
    }
  }

  public GetMetaDataSql getViewSourceSql()
  {
    synchronized (LOCK)
    {
      if (this.viewSource == null)
      {
        HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ViewSourceStatements.xml");
        this.viewSource = getEntry(sql, "view source");
      }
      return this.viewSource;
    }
  }


  public GetMetaDataSql getListTriggerSql()
  {
    synchronized (LOCK)
    {
      if (this.listTrigger == null)
      {
        HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ListTriggersStatements.xml");
        this.listTrigger = getEntry(sql, "trigger list");
      }
      return this.listTrigger;
    }
  }

  public GetMetaDataSql getTriggerSourceSql()
  {
    synchronized (LOCK)
    {
      if (this.triggerSource == null)
      {
        HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("TriggerSourceStatements.xml");
        this.triggerSource = getEntry(sql, "trigger source");
      }
      return this.triggerSource;
    }
  }

  private GetMetaDataSql getEntry(Map<String, GetMetaDataSql> statements, String sourceType)
  {
    GetMetaDataSql metaSql = getEntryByKey(dbIdKey, statements);
    if (metaSql != null)
    {
      LogMgr.logInfo("DbMetadata.getEntry()", "Using template for " + sourceType + " based on DBID: [" + dbIdKey + "] instead of product name: " + productName);
      return metaSql;
    }
    return getEntryByKey(productName, statements);
  }

  private GetMetaDataSql getEntryByKey(String baseKey, Map<String, GetMetaDataSql> statements)
  {
    String key = null;
    GetMetaDataSql sql = null;

    if (majorVersion != -1 && minorVersion != -1)
    {
      key = baseKey + "-" + NumberStringCache.getNumberString(majorVersion) + "." + NumberStringCache.getNumberString(minorVersion);
      sql = statements.get(key);
    }

    // major+minor version specific entry found, use that
    if (sql != null) return sql;

    // second try: only major version
    if (majorVersion != -1)
    {
      key = baseKey + "-" + NumberStringCache.getNumberString(majorVersion);
      sql = statements.get(key);
    }

    // found something, use that
    if (sql != null) return sql;

    // last try: only the key
    return statements.get(baseKey);
  }

  @SuppressWarnings("unchecked")
  private HashMap readStatementTemplates(String aFilename)
  {
    HashMap result = null;

    BufferedInputStream in = new BufferedInputStream(DbMetadata.class.getResourceAsStream(aFilename));
    Object value;
    try
    {
      WbPersistence reader = new WbPersistence();
      value = reader.readObject(in);
    }
    catch (Throwable e)
    {
      LogMgr.logError("MetaDataSqlManager.readStatementTemplate()", "Error reading internal templates file " + aFilename,e);
      value = null;
    }

    if (value instanceof HashMap)
    {
      result = (HashMap)value;
    }


    File f = new File(Settings.getInstance().getConfigDir(), aFilename);
    if (!f.exists())
    {
      // not in the config directory, try the directory where the sqlworkbench.jar is located
      f = new File(WbManager.getInstance().getJarPath(), aFilename);
    }

    if (!f.exists())
    {
      // not in the config directory, not in the jar file's directry, try the current directory
      f = new File(aFilename);
    }

    if (f.exists())
    {
      try
      {
        WbPersistence reader = new WbPersistence(f.getAbsolutePath());
        value = reader.readObject();
        LogMgr.logInfo("DbMetadata.readStatementTemplates()", "Using SQL queries from user defined file " + f.getAbsolutePath());
      }
      catch (Throwable e)
      {
        LogMgr.logWarning("MetaDataSqlManager.readStatementTemplate()", "Error reading template file " + f.getAbsolutePath(), e);
      }
      if (value instanceof HashMap)
      {
        HashMap m = (HashMap<String, GetMetaDataSql>)value;
        if (result != null)
        {
          result.putAll(m);
        }
        else
        {
          // no built-in file found. Should not happen.
          result = m;
        }
      }
    }
    else if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("MetaDataSqlManager.readStatementTemplates()", "No user defined template file found for " + aFilename);
    }
    return result;
  }

  private void getEntry(String productName, Map<String, GetMetaDataSql> statements)
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
