/*
 * SchemaDiff.java
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
package workbench.db.diff;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionInfoBuilder;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.FKHandler;
import workbench.db.ProcedureDefinition;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleProcedureReader;
import workbench.db.report.ReportPackage;
import workbench.db.report.ReportProcedure;
import workbench.db.report.ReportSequence;
import workbench.db.report.ReportTable;
import workbench.db.report.ReportView;
import workbench.db.report.TagWriter;

import workbench.storage.RowActionMonitor;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * Compare two Schemas for differences in the definition of the tables
 *
 * @author Thomas Kellerer
 */
public class SchemaDiff
{
  public static final String TAG_ADD_TABLE = "add-table";
  public static final String TAG_ADD_VIEW = "add-view";
  public static final String TAG_DROP_TABLE = "drop-table";
  public static final String TAG_DROP_VIEW = "drop-view";
  public static final String TAG_DROP_PROC = "drop-procedure";
  public static final String TAG_ADD_PROC = "add-procedure";
  public static final String TAG_DROP_SEQUENCE = "drop-sequence";
  public static final String TAG_DROP_PCKG = "drop-package";

  public static final String TAG_REF_CONN = "reference-connection";
  public static final String TAG_TARGET_CONN = "target-connection";
  public static final String TAG_COMPARE_INFO = "compare-settings";
  public static final String TAG_VIEW_PAIR = "view-info";
  public static final String TAG_TABLE_PAIR = "table-info";
  public static final String TAG_PROC_PAIR = "procedure-info";
  public static final String TAG_PKG_PAIR = "package-info";
  public static final String TAG_SEQ_PAIR = "sequence-info";
  public static final String TAG_INDEX_INFO = "include-index";
  public static final String TAG_FK_INFO = "include-foreign-key";
  public static final String TAG_PK_INFO = "include-primary-key";
  public static final String TAG_GRANT_INFO = "include-tablegrants";
  public static final String TAG_CONSTRAINT_INFO = "include-constraints";
  public static final String TAG_TRIGGER_INFO = "include-triggers";
  public static final String TAG_TYPES = "included-types";
  public static final String TAG_VIEW_INFO = "include-views";
  public static final String TAG_PROC_INFO = "include-procs";
  public static final String TAG_SEQUENCE_INFO = "include-sequences";
  public static final String TAG_VIEWS_AS_TABLE = "views-as-tables";
  public static final String TAG_FULL_SOURCE = "full-object-source";

  private GenericDiffLoader objectDiffs;
  private List<Object> objectsToCompare;
  private List<TableIdentifier> tablesToDelete;
  private List<ProcedureDefinition> procsToDelete;
  private List<ReportPackage> packagesToDelete;
  private List<TableIdentifier> viewsToDelete;
  private List<SequenceDefinition> sequencesToDelete;

  private String encoding = "UTF-8";
  private boolean compareJdbcTypes;
  private boolean diffIndex = true;
  private boolean diffForeignKeys = true;
  private boolean diffPrimaryKeys = true;
  private boolean diffConstraints;
  private boolean diffGrants;
  private boolean diffViews = true;
  private boolean diffProcs = true;
  private boolean diffTriggers = true;
  private boolean diffSequences = true;
  private boolean diffPartitions;
  private boolean useFullSource;
  private boolean treatViewAsTable;
  private boolean compareConstraintsByName;
  private String[] additionalTypes;

  private RowActionMonitor monitor;
  private boolean cancel;

  private WbConnection referenceDb;
  private WbConnection targetDb;
  private String referenceSchema;
  private String targetSchema;
  private Set<String> tablesToIgnore = CollectionUtil.caseInsensitiveSet();

  public SchemaDiff()
  {
  }

  /**
   * Create a new SchemaDiff for the given connections with the given
   * namespace to be used when writing the XML.
   *
   * @param reference The connection to the reference schema
   * @param target the connection to the target schema
   */
  public SchemaDiff(WbConnection reference, WbConnection target)
  {
    referenceDb = reference;
    targetDb = target;
  }

  public void setCompareConstraintsByName(boolean flag)
  {
    this.compareConstraintsByName = flag;
  }

  public void setIncludeTriggers(boolean flag)
  {
    this.diffTriggers = flag;
  }

  public void setIncludePartitions(boolean flag)
  {
    this.diffPartitions = flag;
  }

  public void setAdditionalTypes(List<String> types)
  {
    if (CollectionUtil.isEmpty(types))
    {
      additionalTypes = null;
      return;
    }
    additionalTypes = new String[types.size()];
    for (int i=0; i < types.size(); i++)
    {
      additionalTypes[i] = types.get(i).toUpperCase();
    }
  }

  public boolean isSameDBMS()
  {
    return referenceDb.getDbId().equalsIgnoreCase(targetDb.getDbId());
  }

  public void setUseFullObjectSource(boolean flag)
  {
    useFullSource = flag;
  }

  public void setIncludeSequences(boolean flag)
  {
    this.diffSequences = flag;
  }

  /**
   * Control whether foreign keys should be compared as well.
   * The default is to compare foreign keys.
   */
  public void setIncludeForeignKeys(boolean flag) { this.diffForeignKeys = flag; }
  public boolean getIncludeForeignKeys() { return this.diffForeignKeys; }

  /**
   *  Control whether index definitions should be compared as well.
   *  The default is to compare index definitions
   */
  public void setIncludeIndex(boolean flag) { this.diffIndex = flag; }
  public boolean getIncludeIndex() { return this.diffIndex; }

  /**
   * Control whether primary keys should be compared as well.
   * The default is to compare primary keys.
   */
  public void setIncludePrimaryKeys(boolean flag) { this.diffPrimaryKeys = flag; }

  /**
   * Control whether table constraints should be compared as well.
   * The default is to not compare primary keys.
   */
  public void setIncludeTableConstraints(boolean flag)
  {
    this.diffConstraints = flag;
  }

  public void setCompareJdbcTypes(boolean flag)
  {
    this.compareJdbcTypes = flag;
  }

  public boolean getCompareJdbcTypes()
  {
    return this.compareJdbcTypes;
  }

  public void setIncludeViews(boolean flag)
  {
    this.diffViews = flag;
  }

  public void setIncludeProcedures(boolean flag)
  {
    this.diffProcs = flag;
  }

  public void setIncludeTableGrants(boolean flag)
  {
    this.diffGrants = flag;
  }

  public boolean getIncludeTableGrants()
  {
    return this.diffGrants;
  }

  public void setTreatViewAsTable(boolean flag)
  {
    this.treatViewAsTable = flag;
  }

//  public void setIncludeComments(boolean flag) { this.diffComments = flag; }

  /**
   *  Set the {@link workbench.storage.RowActionMonitor} for reporting progress
   */
  public void setMonitor(RowActionMonitor mon)
  {
    this.monitor = mon;
  }

  /**
   *  Cancel the creation of the XML file
   *  @see #isCancelled()
   */
  public void cancel()
  {
    this.cancel = true;
  }

  /**
   *  Return if the XML generation has been cancelled
   * @return true if #cancel() has been called
   */
  public boolean isCancelled()
  {
    return this.cancel;
  }

  /**
   * Define table names to be compared. The table names in the passed
   * lists will be converted to TableIdentifiers and then passed
   * on to setTables(List<TableIdentifier>, List<TableIdentifier>)
   *
   * No name matching will take place. Thus it's possible to compare
   * tables that might have different names but are supposed to be identical
   * otherwise.
   *
   * @see #setTables(List, List)
   * @see #compareAll()
   */
  public void setTableNames(List<String> referenceList, List<String> targetList)
    throws SQLException
  {
    ArrayList<TableIdentifier> reference = new ArrayList<>(referenceList.size());
    ArrayList<TableIdentifier> target = new ArrayList<>(targetList.size());

    if (referenceList.size() != targetList.size()) throw new IllegalArgumentException("Size of lists does not match");

    for (int i=0; i < referenceList.size(); i++)
    {
      String rname = referenceList.get(i);
      TableIdentifier rtbl = referenceDb.getMetadata().findTable(new TableIdentifier(rname, referenceDb), false);
      String tname = targetList.get(i);
      TableIdentifier ttbl = targetDb.getMetadata().findTable(new TableIdentifier(tname, targetDb), false);
      if (rtbl != null && ttbl != null)
      {
        reference.add(rtbl);
        target.add(ttbl);
      }
      else
      {
        LogMgr.logWarning("SchemaDiff.setTableNames()", "Table combination not found: "
          + rname + " [" + rtbl + "] to " + tname + " [" + ttbl + "]");
      }
    }
    setTables(reference, target);
  }

  /**
   * Define the tables to be compared. They will be compared based
   * on the position in the arrays (i.e. reference at index 0 will be
   * compared to target at index 0...)
   *
   * No name matching will take place. Thus it's possible to compare
   * tables that might have different names but are supposed to be identical
   * otherwise.
   *
   * @see #setTables(List)
   * @see #compareAll()
   */
  public void setTables(List<TableIdentifier> referenceList, List<TableIdentifier> targetList)
    throws SQLException
  {
    if (referenceList == null) throw new NullPointerException("Source tables may not be null");
    if (targetList == null) throw new NullPointerException("Target tables may not be null");
    if (referenceList.size() != targetList.size()) throw new IllegalArgumentException("Number of source and target tables have to match");
    int count = referenceList.size();
    this.objectsToCompare = new ArrayList<>(count);

    if (this.monitor != null)
    {
      this.monitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
      this.monitor.setCurrentObject(ResourceMgr.getString("MsgDiffRetrieveDbInfo"), -1, -1);
    }

    for (int i=0; i < count; i++)
    {
      if (this.cancel)
      {
        this.objectsToCompare = null;
        break;
      }
      TableIdentifier reference = referenceList.get(i);

      if (reference == null) continue;

      TableIdentifier target = targetList.get(i);
      DiffEntry entry = new DiffEntry(reference, target);
      this.objectsToCompare.add(entry);
    }
  }

  private ReportView createReportViewInstance(TableIdentifier tbl, WbConnection con)
    throws SQLException
  {
    tbl.adjustCase(con);
    ReportView view = new ReportView(tbl, con, diffIndex, diffGrants, useFullSource);
    return view;
  }

  private ReportTable createReportTableInstance(TableIdentifier tbl, WbConnection con)
    throws SQLException
  {
    tbl.adjustCase(con);
    ReportTable rTable = new ReportTable(tbl, con, diffIndex, diffForeignKeys, diffPrimaryKeys, diffConstraints, diffGrants, diffTriggers, diffPartitions);
    return rTable;
  }

  /**
   * Define a list of table names that should not be compared.
   * Tables in the reference/source database that match one of the
   * names in this list will be skipped.
   */
  public void setExcludeTables(List<String> tables)
  {
    if (tables == null || tables.isEmpty())
    {
      this.tablesToIgnore.clear();
      return;
    }
    for (String tname : tables)
    {
      if (tname.indexOf('%') > -1 || tname.indexOf('*') > -1)
      {
        try
        {
          String toSearch = this.referenceDb.getMetadata().adjustObjectnameCase(tname.trim());
          List<TableIdentifier> tlist = this.referenceDb.getMetadata().getTableList(toSearch, (String)null);
          for (TableIdentifier t : tlist)
          {
            tablesToIgnore.add(t.getTableName());
          }
          toSearch = this.targetDb.getMetadata().adjustObjectnameCase(tname.trim());
          tlist = this.targetDb.getMetadata().getTableList(toSearch, (String)null);
          for (TableIdentifier t : tlist)
          {
            tablesToIgnore.add(t.getTableName());
          }
        }
        catch (SQLException e)
        {
          LogMgr.logError("SchemaDiff.setExcludeTables()","Could not retrieve excluded tables", e);
        }

      }
      else
      {
        this.tablesToIgnore.add(tname);
      }
    }
  }

  /**
   *  Setup this SchemaDiff object to compare all tables that the user
   *  can access in the reference connection with all matching (=same name)
   *  tables in the target connection.
   *  This will retrieve all user tables from the reference (=source)
   *  connection and will match them to the tables in the target connection.
   *
   *  When using compareAll() drop statements will be created for tables
   *  present in the target connection but not existing in the reference
   *  connection.
   *
   * @see #setTables(List, List)
   * @see #setTables(List)
   */
  public void compareAll()
    throws SQLException
  {
    setSchemas(null, null);
  }

  /**
   * Define the reference and target schema without an automatic retrieval of all objects.
   *
   * This should be used when only specific tables should be compared, but stored procedures
   * and sequences as well. In this case, setting reference and target schema will limit
   * the list of procedures and sequences processed.
   *
   * @param rSchema the name of the references schema
   * @param tSchema the name of the target schema
   */
  public void setSchemaNames(String rSchema, String tSchema)
  {
    referenceSchema = rSchema;
    targetSchema = tSchema;
  }


  /**
   *  Setup this SchemaDiff object to compare all tables that the user
   *  can access in the reference connection with all matching (=same name)
   *  tables in the target connection.
   *
   *  This will retrieve all user tables from the reference (=source)
   *  connection and will match them to the tables in the target connection.
   *
   *  When using compareAll() drop statements will be created for tables
   *  present in the target connection but not existing in the reference
   *  connection.
   *
   * @param rSchema the reference schema. If null the "current schema" of the reference connection will be used
   * @param tSchema the target schema. If null the "current schema" of the target connection will be used
   *
   * @see #setTables(List, List)
   * @see #setTables(List)
   */
  public void setSchemas(String rSchema, String tSchema)
    throws SQLException
  {
    if (this.monitor != null)
    {
      this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
      this.monitor.setCurrentObject(ResourceMgr.getString("MsgDiffRetrieveDbInfo"), -1, -1);
    }

    if (rSchema == null)
    {
      if (referenceDb.getDbSettings().supportsSchemas())
      {
        referenceSchema = this.referenceDb.getMetadata().getCurrentSchema();
      }
      else
      {
        referenceSchema = this.referenceDb.getMetadata().getCurrentCatalog();
      }
    }
    else
    {
      this.referenceSchema = this.referenceDb.getMetadata().adjustSchemaNameCase(rSchema);
    }

    if (tSchema == null)
    {
      if (targetDb.getDbSettings().supportsSchemas())
      {
        targetSchema = this.targetDb.getMetadata().getCurrentSchema();
      }
      else
      {
        targetSchema = this.targetDb.getMetadata().getCurrentCatalog();
      }
    }
    else
    {
      this.targetSchema = this.referenceDb.getMetadata().adjustSchemaNameCase(tSchema);
    }

    String[] types;
    if (diffViews || treatViewAsTable)
    {
      types = this.referenceDb.getMetadata().getTablesAndViewTypes();
    }
    else
    {
      types = this.referenceDb.getMetadata().getTableTypesArray();
    }

    List<TableIdentifier> refTables = referenceDb.getMetadata().getObjectList(null, getReferenceCatalog(referenceSchema), getReferenceSchema(referenceSchema), types);
    if (this.cancel) return;

    List<TableIdentifier> target = targetDb.getMetadata().getObjectList(null, getTargetCatalog(this.targetSchema), getTargetSchema(this.targetSchema), types);
    if (this.cancel) return;

    if (treatViewAsTable)
    {
      String viewType = referenceDb.getMetadata().getViewTypeName();
      String tblType = referenceDb.getMetadata().getBaseTableTypeName();
      for (TableIdentifier table : refTables)
      {
        if (table.getType().equals(viewType))
        {
          table.setType(tblType);
        }
      }
    }

    if (this.cancel) return;

    processTableList(refTables, target);
  }

  /**
   * Return the catalog name to be used for the passed "schema input".
   *
   * If the target DBMS does not support schemas, the schemaName will
   * be returned to it can be used as a catalog.
   */
  private String getTargetCatalog(String schemaName)
  {
    if (targetDb.getDbSettings().supportsSchemas()) return null;
    return schemaName;
  }

  /**
   * Return the schema name to be used for the passed "schema input".
   *
   * If the target DBMS supports schemas, the schemaName will
   * be returned, otherwise null.
   */
  private String getTargetSchema(String schemaName)
  {
    if (targetDb.getDbSettings().supportsSchemas()) return schemaName;
    return null;
  }

  private String getReferenceCatalog(String schemaName)
  {
    if (referenceDb.getDbSettings().supportsSchemas()) return null;
    return schemaName;
  }

  private String getReferenceSchema(String schemaName)
  {
    if (referenceDb.getDbSettings().supportsSchemas()) return schemaName;
    return null;
  }

  private void buildSequenceList()
  {
    try
    {
      List<SequenceDefinition> refSeqs = Collections.emptyList();
      List<SequenceDefinition> tarSeqs = Collections.emptyList();
      SequenceReader refReader = referenceDb.getMetadata().getSequenceReader();
      SequenceReader tarReader = targetDb.getMetadata().getSequenceReader();
      if (refReader != null)
      {
        refSeqs = refReader.getSequences(null, this.referenceSchema, null);
      }
      if (tarReader != null)
      {
        tarSeqs = tarReader.getSequences(null, this.targetSchema, null);
      }
      processSequenceList(refSeqs, tarSeqs);
    }
    catch (SQLException sql)
    {
      LogMgr.logError("SchemaDiff.buildSequenceList()", "Error retrieving procedures", sql);
    }
  }

  private void buildProcedureList()
  {
    try
    {
      List<ProcedureDefinition> refProcs = referenceDb.getMetadata().getProcedureReader().getProcedureList(getReferenceCatalog(referenceSchema), getReferenceSchema(referenceSchema), null);
      List<ProcedureDefinition> targetProcs = targetDb.getMetadata().getProcedureReader().getProcedureList(getTargetCatalog(targetSchema), getTargetSchema(targetSchema), null);

      processProcedureList(refProcs, targetProcs);
    }
    catch (SQLException sql)
    {
      LogMgr.logError("SchemaDiff.buildProcedureList()", "Error retrieving procedures", sql);
    }
  }

  private void processTableList(List<TableIdentifier> refTables, List<TableIdentifier> targetTables)
    throws SQLException
  {
    int count = refTables.size();
    HashSet<String> refTableNames = new HashSet<>();

    this.objectsToCompare = new ArrayList<>(count);

    if (this.monitor != null)
    {
      this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
    }

    for (int i=0; i < count; i++)
    {
      if (this.cancel)
      {
        this.objectsToCompare = null;
        break;
      }

      TableIdentifier rid = refTables.get(i);

      String tname = SqlUtil.removeObjectQuotes(rid.getTableName());

      if (this.tablesToIgnore.contains(tname)) continue;

      if (this.monitor != null)
      {
        this.monitor.setCurrentObject(ResourceMgr.getFormattedString("MsgLoadTableInfo", tname), -1, -1);
      }

      TableIdentifier tid = findTargetTable(rid, targetTables);
      DiffEntry entry = new DiffEntry(rid, tid);
      objectsToCompare.add(entry);
      refTableNames.add(tname);
    }

    if (cancel) return;

    this.tablesToDelete = new ArrayList<>();
    this.viewsToDelete = new ArrayList<>();

    if (targetTables != null)
    {
      DbMetadata meta = targetDb.getMetadata();
      count = targetTables.size();
      for (int i=0; i < count; i++)
      {
        TableIdentifier t = targetTables.get(i);
        String tbl = SqlUtil.removeObjectQuotes(t.getTableName());
        if (this.tablesToIgnore.contains(tbl)) continue;

        if (targetDb.getMetadata().isDefaultCase(tbl))
        {
          tbl = referenceDb.getMetadata().adjustObjectnameCase(tbl);
        }
        if (!refTableNames.contains(tbl))
        {
          if (meta.isTableType(t.getType()))
          {
            this.tablesToDelete.add(t);
          }
          else
          {
            this.viewsToDelete.add(t);
          }
        }
      }
    }
  }

  private TableIdentifier findTargetTable(TableIdentifier refTable, List<TableIdentifier> targetTables)
  {
    boolean isDefaultCase = referenceDb.getMetadata().isDefaultCase(refTable.getTableName());

    TableIdentifier tid = refTable.createCopy();

    if (isDefaultCase || !targetDb.getMetadata().needsQuotes(refTable.getTableName()))
    {
      // if the name does not need quoting, then adjust the case, otherwise use it as it is.
      tid.setNeverAdjustCase(false);
      tid.adjustCase(targetDb);
    }

    tid.setSchema(targetSchema);
    tid.setCatalog(null);

    TableIdentifier tbl = TableIdentifier.findTableByNameAndSchema(targetTables, tid);
    if (tbl != null) return tbl;

    tbl = TableIdentifier.findTableByName(targetTables, tid);
    if (tbl != null) return tbl;

    tid.setType(refTable.getType());
    return targetDb.getMetadata().findObject(tid);
  }

  private void processSequenceList(List<SequenceDefinition> refSeqs, List<SequenceDefinition> targetSeqs)
  {
    this.sequencesToDelete= new ArrayList<>();

    if (this.monitor != null)
    {
      this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
    }

    SequenceReader targetReader = this.targetDb.getMetadata().getSequenceReader();

    for (SequenceDefinition refSeq : refSeqs)
    {
      if (this.cancel)
      {
        this.objectsToCompare = null;
        break;
      }

      if (this.monitor != null)
      {
        this.monitor.setCurrentObject(ResourceMgr.getFormattedString("MsgLoadSeqInfo", refSeq.getSequenceName()), -1, -1);
      }

      SequenceDiffEntry entry;

      if (targetReader != null)
      {
        SequenceDefinition targetSequence = findSequence(targetSeqs, targetSchema, refSeq.getSequenceName());
        entry = new SequenceDiffEntry(refSeq, targetSequence);
        objectsToCompare.add(entry);
      }
    }

    if (cancel) return;

    for (SequenceDefinition tSeq : targetSeqs)
    {
      if (findSequence(refSeqs, referenceSchema, tSeq.getSequenceName()) == null)
      {
        this.sequencesToDelete.add(tSeq);
      }
    }
  }

  private SequenceDefinition findSequence(List<SequenceDefinition> sequences, String schema, String sequenceName)
  {
    for (SequenceDefinition seq : sequences)
    {
      if (StringUtil.equalStringIgnoreCase(seq.getObjectName(), sequenceName))
      {
        if (seq.getSchema() != null && schema != null)
        {
          if (StringUtil.equalStringIgnoreCase(seq.getSchema(), schema)) return seq;
        }
      }
    }
    return null;
  }

  private void buildObjectsList()
  {
    if (this.additionalTypes != null)
    {
      this.objectDiffs = new GenericDiffLoader(referenceDb, targetDb, referenceSchema, targetSchema, additionalTypes);
      this.objectDiffs.setProgressMonitor(monitor);
      this.objectDiffs.loadObjects();
    }
  }

  private void processProcedureList(List<ProcedureDefinition> refProcs, List<ProcedureDefinition> targetProcs)
  {
    HashSet<String> refProcNames = new HashSet<>();
    this.procsToDelete = new ArrayList<>();
    this.packagesToDelete =new ArrayList<>();

    Set<ReportPackage> refPackages = new HashSet<>();

    DbMetadata targetMeta = this.targetDb.getMetadata();

    if (this.monitor != null)
    {
      this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
    }

    if (refProcs == null) refProcs = Collections.emptyList();
    if (targetProcs == null) targetProcs = Collections.emptyList();

    for (ProcedureDefinition refProc : refProcs)
    {
      if (refProc.isOracleObjectType()) continue;

      if (this.cancel)
      {
        this.objectsToCompare = null;
        break;
      }
      if (this.monitor != null)

      {
        this.monitor.setCurrentObject(ResourceMgr.getFormattedString("MsgLoadProcInfo", refProc.getProcedureName()), -1, -1);
      }

      if (refProc.isPackageProcedure())
      {
        // Handle packages differently than procedures to avoid
        // comparing them multiple times for each procedure or function
        PackageDiffEntry entry;
        ReportPackage pkg = new ReportPackage(refProc);
        if (!refPackages.contains(pkg))
        {
          // keep track of processed packages as they are returned once for each procedure
          refPackages.add(pkg);
          if (targetMeta.isOracle())
          {
            OracleProcedureReader ora = (OracleProcedureReader)targetMeta.getProcedureReader();
            boolean exists = ora.packageExists(targetSchema, refProc.getPackageName());
            if (exists)
            {
              ReportPackage tpkg = new ReportPackage(targetSchema, refProc.getPackageName());
              entry = new PackageDiffEntry(pkg, tpkg);
            }
            else
            {
              entry = new PackageDiffEntry(pkg, null);
            }
            objectsToCompare.add(entry);
          }
        }
      }
      else
      {
        ProcDiffEntry entry;

        ProcedureDefinition toFind = new ProcedureDefinition(getTargetCatalog(targetSchema), getTargetSchema(targetSchema), refProc.getProcedureName(), refProc.getResultType());
        toFind.setParameters(refProc.getParameters(referenceDb));

        ProcedureDefinition targetProc = targetMeta.getProcedureReader().findProcedureDefinition(toFind);
        if (targetProc != null)
        {
          entry = new ProcDiffEntry(refProc, targetProc);
        }
        else
        {
          entry = new ProcDiffEntry(refProc, null);
        }
        objectsToCompare.add(entry);
      }
      refProcNames.add(refProc.getDisplayName());
    }

    for (ProcedureDefinition tProc : targetProcs)
    {
      if (tProc.isOracleObjectType()) continue;
      if (tProc.isPackageProcedure()) continue;

      if (this.cancel)
      {
        this.objectsToCompare = null;
        break;
      }

      String procname = tProc.getDisplayName();
      if (!refProcNames.contains(procname))
      {
        this.procsToDelete.add(tProc);
      }
    }

    if (targetMeta.isOracle())
    {
      Set<ReportPackage> deleted = new HashSet<>();
      for (ProcedureDefinition tProc : targetProcs)
      {
        if (!tProc.isPackageProcedure()) continue;

        if (this.cancel)
        {
          this.objectsToCompare = null;
          break;
        }

        ReportPackage tp = new ReportPackage(tProc);
        if (!deleted.contains(tp))
        {
          ReportPackage pckg = findPackage(refPackages, tProc.getPackageName());
          if (pckg == null)
          {
            this.packagesToDelete.add(tp);
          }
          deleted.add(tp);
        }
      }
    }
  }

  private ReportPackage findPackage(Collection<ReportPackage> list, String packageName)
  {
    for (ReportPackage pckg : list)
    {
      if (pckg.getPackageName().equals(packageName)) return pckg;
    }
    return null;
  }
  /**
   * Define the reference tables to be compared with the matching
   * tables (based on the name) in the target connection. The list
   * has to contain objects of type {@link workbench.db.TableIdentifier}
   *
   * @see #setTables(List, List)
   * @see #compareAll()
   */
  public void setTables(List<TableIdentifier> reference)
    throws SQLException
  {
    this.processTableList(reference, null);
  }

  /**
   *  Return the XML that describes how the target schema needs to be
   *  modified in order to get the same structure as the reference schema.
   *
   *  For this, each defined table in the reference schema will be compared
   *  to the corresponding table in the target schema.
   *
   *  @see TableDiff#getMigrateTargetXml()
   */
  public String getMigrateTargetXml()
  {
    StringWriter writer = new StringWriter(5000);

    FKHandler sourceFK = FKHandler.createInstance(referenceDb);
    FKHandler targetFK = FKHandler.createInstance(targetDb);

    try
    {
      sourceFK.initializeSharedCache();
      targetFK.initializeSharedCache();

      this.writeXml(writer);
    }
    catch (Exception e)
    {
      LogMgr.logError("SchemaDiff.getMigrateTargetXml()", "Error getting XML", e);
    }
    finally
    {
      sourceFK.clearSharedCache();
      targetFK.clearSharedCache();
    }
    return writer.toString();
  }

  /**
   *  Set the encoding that is used for writing the XML. This will
   *  be put into the <?xml> tag at the beginning of the generated XML
   */
  public void setEncoding(String enc)
  {
    if (StringUtil.isNonBlank(enc))
    {
      this.encoding = enc;
    }
  }

  /**
   *  Write the XML of the schema differences to the supplied writer.
   *  This writes some meta information about the compare, and then
   *  creates a {@link TableDiff} object for each pair of tables that
   *  needs to be compared. The output of {@link TableDiff#getMigrateTargetXml()}
   *  will then be written into the writer.
   */
  public void writeXml(Writer out)
    throws IOException
  {
    if (diffProcs)
    {
      buildProcedureList();
    }

    if (diffSequences)
    {
      buildSequenceList();
    }

    buildObjectsList();

    if (objectsToCompare == null && (objectDiffs == null || objectDiffs.getObjectCount() == 0))
    {
      // nothing to do
      return;
    }

    StringBuilder indent = new StringBuilder("  ");
    StringBuilder tblIndent = new StringBuilder("    ");
    out.write("<?xml version=\"1.0\" encoding=\"");
    out.write(this.encoding);
    out.write("\"?>\n");

    if (this.monitor != null)
    {
      this.monitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
    }

    writeTag(out, null, "schema-diff", true);
    writeDiffInfo(out);
    int count = this.objectsToCompare.size();
    List<ViewDiff> viewDiffs = new ArrayList<>();

    // First we have to process the tables
    for (int i=0; i < count; i++)
    {
      Object o = objectsToCompare.get(i);
      if (o instanceof ProcDiffEntry) continue;
      if (o instanceof SequenceDiffEntry) continue;
      if (o instanceof PackageDiffEntry) continue;

      DiffEntry entry = (DiffEntry)o;
      if (this.cancel) break;

      if (this.monitor != null)
      {
        this.monitor.setCurrentObject(entry.reference.getTableExpression(), i+1, count);
      }

      try
      {
        String refType = entry.reference.getType();

        if (referenceDb.getMetadata().isTableType(refType)
            && !refType.equals(referenceDb.getMetadata().getMViewTypeName()))
        {
          ReportTable source = createReportTableInstance(entry.reference, this.referenceDb);
          if (entry.target == null)
          {
            out.write("\n");
            writeTag(out, indent, TAG_ADD_TABLE, true, "name", entry.reference.getTableName());
            StringBuilder s = source.getXml(tblIndent);
            out.write(s.toString());
            writeTag(out, indent, TAG_ADD_TABLE, false);
          }
          else
          {
            ReportTable target = createReportTableInstance(entry.target, this.targetDb);
            TableDiff d = new TableDiff(source, target, this);
            d.setIndent(indent);
            d.setExactConstraintMatch(compareConstraintsByName);
            StringBuilder s = d.getMigrateTargetXml();
            if (s.length() > 0)
            {
              out.write("\n");
              out.write(s.toString());
            }
          }
        }
        else
        {
          // We cannot write out the diff for the views immediately
          // because they should be listed after the table diffs
          ReportView source = createReportViewInstance(entry.reference, referenceDb);
          ReportView target = null;
          if (entry.target != null)
          {
            target = createReportViewInstance(entry.target, targetDb);
          }
          ViewDiff d = new ViewDiff(source, target);
          d.setIndent(indent);
          viewDiffs.add(d);
        }
      }
      catch (SQLException sql)
      {
        LogMgr.logError("SchemaDiff.writeXml()", "Error comparing " + entry.toString(), sql);
      }
    }

    if (this.cancel) return;

    this.appendDropTables(out, indent);
    out.write("\n");

    if (this.diffViews)
    {
      this.appendViewDiff(viewDiffs, out);
      //out.write("\n");
      this.appendDropViews(out, indent);
    }

    if (this.cancel) return;

    if (this.diffSequences)
    {
      this.appendSequenceDiff(out, indent);
      out.write("\n");
    }

    if (this.cancel) return;

    if (this.diffProcs)
    {
      this.appendProcDiff(out, indent);
      this.appendPackageDiff(out, indent);
      out.write("\n");
    }

    if (this.cancel) return;

    if (objectDiffs != null)
    {
      StringBuilder xml = objectDiffs.getMigrateTargetXml(indent);
      if (xml != null && xml.length() > 0)
      {
        out.write(xml.toString());
      }
    }
    writeTag(out, null, "schema-diff", false);

  }

  private void appendSequenceDiff(Writer out, StringBuilder indent)
    throws IOException
  {
    int count = this.objectsToCompare.size();
    for (int i=0; i < count; i++)
    {
      Object o = objectsToCompare.get(i);
      if (o instanceof SequenceDiffEntry)
      {
        SequenceDiffEntry entry = (SequenceDiffEntry)o;
        ReportSequence rp = new ReportSequence(entry.reference);
        ReportSequence tp = (entry.target == null ? null : new ReportSequence(entry.target));
        SequenceDiff diff = new SequenceDiff(rp, tp, targetSchema);
        diff.setIndent(indent);
        StringBuilder xml = diff.getMigrateTargetXml();
        if (xml.length() > 0)
        {
          out.write("\n");
          out.write(xml.toString());
        }
      }
    }

    if (this.sequencesToDelete == null || sequencesToDelete.isEmpty()) return;

    out.write('\n');
    writeTag(out, indent, TAG_DROP_SEQUENCE, true);
    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");
    StringBuilder myindent2 = new StringBuilder(myindent);
    myindent2.append("  ");

    for (SequenceDefinition def : sequencesToDelete)
    {
      writeTag(out, myindent, ReportSequence.TAG_SEQ_DEF, true);
      if (StringUtil.isNonEmpty(def.getCatalog()))
      {
        writeTagValue(out, myindent2, ReportSequence.TAG_SEQ_CATALOG, def.getCatalog());
      }
      writeTagValue(out, myindent2, ReportSequence.TAG_SEQ_SCHEMA, def.getSchema());
      writeTagValue(out, myindent2, ReportSequence.TAG_SEQ_NAME, def.getSequenceName());
      writeTag(out, myindent, ReportSequence.TAG_SEQ_DEF, false);
    }
    writeTag(out, indent, TAG_DROP_SEQUENCE, false);
  }

  private void appendProcDiff(Writer out, StringBuilder indent)
    throws IOException
  {
    int count = this.objectsToCompare.size();
    for (int i=0; i < count; i++)
    {
      Object o = objectsToCompare.get(i);
      if (o instanceof ProcDiffEntry)
      {
        ProcDiffEntry entry = (ProcDiffEntry)o;
        ReportProcedure rp = new ReportProcedure(entry.reference, this.referenceDb);
        ReportProcedure tp = new ReportProcedure(entry.target, this.targetDb);

        String tschema = tp.getSchema() == null ? targetSchema : tp.getSchema();
        if (StringUtil.stringsAreNotEqual(rp.getSchema(), tschema))
        {
          // pretend both procedures are stored in the same schema
          // otherwise a comparison of the source code that includes the schema name will not work.
          // this will not work for every DBMS though (i.e. every ProcedureReader)
          rp.setSchemaToUse(tschema);
        }

        ProcDiff diff = new ProcDiff(rp, tp);
        diff.setIndent(indent);
        StringBuilder xml = diff.getMigrateTargetXml();
        if (xml.length() > 0)
        {
          out.write("\n");
          out.write(xml.toString());
        }
      }
    }

    if (CollectionUtil.isEmpty(procsToDelete)) return;

    out.write('\n');
    writeTag(out, indent, TAG_DROP_PROC, true);
    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");
    for (ProcedureDefinition def : procsToDelete)
    {
      ReportProcedure rp = new ReportProcedure(def, targetDb);
      rp.setIndent(myindent);
      StringBuilder xml = rp.getXml(false);
      out.write(xml.toString());
    }
    writeTag(out, indent, TAG_DROP_PROC, false);
  }

  private void appendPackageDiff(Writer out, StringBuilder indent)
    throws IOException
  {
    int count = this.objectsToCompare.size();
    for (int i=0; i < count; i++)
    {
      Object o = objectsToCompare.get(i);
      if (o instanceof PackageDiffEntry)
      {
        PackageDiffEntry entry = (PackageDiffEntry)o;
        if (entry.reference != null)
        {
          entry.reference.readSource(referenceDb);
        }
        if (entry.target != null)
        {
          entry.target.readSource(targetDb);
        }
        PackageDiff diff = new PackageDiff(entry.reference, entry.target);
        diff.setIndent(indent);
        StringBuilder xml = diff.getMigrateTargetXml();
        if (xml.length() > 0)
        {
          out.write("\n");
          out.write(xml.toString());
        }
      }
    }

    if (CollectionUtil.isEmpty(packagesToDelete)) return;

    out.write('\n');
    writeTag(out, indent, TAG_DROP_PCKG, true);
    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");
    for (ReportPackage def : packagesToDelete)
    {
      def.setSchemaToUse(targetSchema);
      def.setIndent(myindent);
      StringBuilder xml = def.getXml(false);
      out.write(xml.toString());
      def.setSchemaToUse(null);
    }
    writeTag(out, indent, TAG_DROP_PCKG, false);
  }

  private void appendViewDiff(List<ViewDiff> diffs, Writer out)
    throws IOException
  {
    for (ViewDiff d : diffs)
    {
      StringBuilder source = d.getMigrateTargetXml();
      if (source.length() > 0)
      {
        out.write("\n");
        out.write(source.toString());
      }
    }
  }

  private void appendDropViews(Writer out, StringBuilder indent)
    throws IOException
  {
    if (this.viewsToDelete == null || this.viewsToDelete.isEmpty()) return;
    out.write("\n");
    writeTag(out, indent, TAG_DROP_VIEW, true);
    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");
    String tCatalog = targetDb.getCurrentCatalog();
    for (TableIdentifier view : viewsToDelete)
    {
      writeTagValue(out, myindent, ReportView.TAG_VIEW_CATALOG, tCatalog);
      writeTagValue(out, myindent, ReportView.TAG_VIEW_SCHEMA, targetSchema);
      writeTagValue(out, myindent, ReportView.TAG_VIEW_NAME, view.getTableName());
    }
    writeTag(out, indent, TAG_DROP_VIEW, false);
  }

  private void appendDropTables(Writer out, StringBuilder indent)
    throws IOException
  {
    if (this.tablesToDelete == null || this.tablesToDelete.isEmpty()) return;
    out.write("\n");

    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");
    String tCatalog = targetDb.getCurrentCatalog();
    for (TableIdentifier tbl : tablesToDelete)
    {
      writeTag(out, indent, TAG_DROP_TABLE, true);
      writeTagValue(out, myindent, ReportTable.TAG_TABLE_CATALOG, tCatalog);
      writeTagValue(out, myindent, ReportTable.TAG_TABLE_SCHEMA, targetSchema);
      writeTagValue(out, myindent, ReportTable.TAG_TABLE_NAME, tbl.getTableName());
      writeTag(out, indent, TAG_DROP_TABLE, false);
    }
  }

  private void writeDiffInfo(Writer out)
    throws IOException
  {
    StringBuilder indent = new StringBuilder("  ");
    StringBuilder indent2 = new StringBuilder("    ");

    TagWriter tw = new TagWriter();

    TagWriter.writeWorkbenchVersion(out, indent);
    out.write(indent.toString());
    out.write("<generated-at>");
    out.write(StringUtil.getCurrentTimestampWithTZString());
    out.write("</generated-at>\n\n");

    ConnectionInfoBuilder builder = new ConnectionInfoBuilder();

    writeTag(out, indent, TAG_REF_CONN, true);
    StringBuilder info = builder.getDatabaseInfoAsXml(this.referenceDb, indent2);
    out.write(info.toString());
    writeTag(out, indent, TAG_REF_CONN, false);
    out.write("\n");
    out.write("  <!-- If the target connection is modified according to the  -->\n");
    out.write("  <!-- defintions in this file, then its structure will be    -->\n");
    out.write("  <!-- the same as the reference connection -->\n");
    writeTag(out, indent, TAG_TARGET_CONN, true);
    info = builder.getDatabaseInfoAsXml(this.targetDb, indent2);
    out.write(info.toString());
    writeTag(out, indent, TAG_TARGET_CONN, false);
    out.write("\n");

    info = new StringBuilder();

    tw.appendOpenTag(info, indent, TAG_COMPARE_INFO);
    info.append('\n');
    tw.appendTag(info, indent2, TAG_INDEX_INFO, this.diffIndex);
    tw.appendTag(info, indent2, TAG_FK_INFO, this.diffForeignKeys);
    tw.appendTag(info, indent2, TAG_PK_INFO, this.diffPrimaryKeys);
    tw.appendTag(info, indent2, TAG_CONSTRAINT_INFO, Boolean.toString(this.diffConstraints), "compare-names", Boolean.toString(compareConstraintsByName));
    tw.appendTag(info, indent2, TAG_TRIGGER_INFO, this.diffTriggers);
    tw.appendTag(info, indent2, TAG_GRANT_INFO, this.diffGrants);
    tw.appendTag(info, indent2, TAG_VIEW_INFO, this.diffViews);
    if (additionalTypes != null)
    {
      StringBuilder indent3 = new StringBuilder(indent2);
      indent3.append("  ");
      tw.appendOpenTag(info, indent2, TAG_TYPES
      );
      info.append('\n');
      for (String type : additionalTypes)
      {
        tw.appendTag(info, indent3, "type-name", type);
      }
      tw.appendCloseTag(info, indent2, TAG_TYPES);
    }

    tw.appendTag(info, indent2, TAG_VIEWS_AS_TABLE, this.treatViewAsTable);
    tw.appendTag(info, indent2, TAG_FULL_SOURCE, useFullSource);

    if (this.referenceSchema != null && this.targetSchema != null)
    {
      tw.appendTag(info, indent2, "reference-schema", this.referenceSchema);
      tw.appendTag(info, indent2, "target-schema", this.targetSchema);
    }
    int count = this.objectsToCompare.size();
    String[] tattr = new String[] { "type", "reference", "compareTo"};
    String[] pattr = new String[] { "referenceProcedure", "compareTo" };
    String[] pkgattr = new String[] { "referencePackage", "compareTo" };
    String[] sattr = new String[] { "referenceSequence", "compareTo" };
    String[] tbls = new String[3];
    DbSettings dbs = this.referenceDb.getMetadata().getDbSettings();
    for (int i=0; i < count; i++)
    {
      // check for ignored tables
      //if (this.referenceTables[i] == null) continue;
      Object o = objectsToCompare.get(i);
      if (o instanceof DiffEntry)
      {
        DiffEntry de = (DiffEntry)o;
        tbls[0] = de.reference.getType();
        tbls[1] = (de.target == null ? "" : de.target.getFullyQualifiedName(targetDb));
        tbls[2] = de.reference.getFullyQualifiedName(referenceDb);
        if (dbs.isViewType(tbls[0]))
        {
          tw.appendOpenTag(info, indent2, TAG_VIEW_PAIR, tattr, tbls, false);
        }
        else
        {
          tw.appendOpenTag(info, indent2, TAG_TABLE_PAIR, tattr, tbls, false);
        }
      }
      else if (o instanceof ProcDiffEntry)
      {
        ProcDiffEntry pe = (ProcDiffEntry)o;
        String[] procs = new String[] { getProcedureNameInfo(referenceDb, pe.reference), getProcedureNameInfo(targetDb, pe.target) };
        tw.appendOpenTag(info, indent2, TAG_PROC_PAIR, pattr, procs, false);
      }
      else if (o instanceof PackageDiffEntry)
      {
        PackageDiffEntry pe = (PackageDiffEntry)o;
        tbls[0] = pe.reference.getPackageName();
        tbls[1] = (pe.target == null ? "" : pe.target.getPackageName());

        tw.appendOpenTag(info, indent2, TAG_PKG_PAIR, pkgattr, tbls, false);
      }
      else if (o instanceof SequenceDiffEntry)
      {
        SequenceDiffEntry pe = (SequenceDiffEntry)o;
        tbls[0] = pe.reference.getFullyQualifiedName(referenceDb);
        tbls[1] = (pe.target == null ? "" : pe.target.getFullyQualifiedName(targetDb));
        tw.appendOpenTag(info, indent2, TAG_SEQ_PAIR, sattr, tbls, false);
      }
      info.append("/>\n");

    }
    tw.appendCloseTag(info, indent, TAG_COMPARE_INFO);

    out.write(info.toString());
  }

  private String getProcedureNameInfo(WbConnection conn, ProcedureDefinition def)
  {
    if (def == null) return "";
    String result = "";

    if (StringUtil.isNonBlank(def.getCatalog()))
    {
      result += def.getCatalog().trim() + conn.getMetadata().getCatalogSeparator();
    }
    if (StringUtil.isNonBlank(def.getSchema()))
    {
      result += def.getSchema().trim() + conn.getMetadata().getSchemaSeparator();
    }
    result += def.getDisplayName();
    return result;
  }

  private void writeTag(Writer out, StringBuilder indent, String tag, boolean isOpeningTag)
    throws IOException
  {
    writeTag(out, indent, tag, isOpeningTag, null, null);
  }

  private void writeTag(Writer out, StringBuilder indent, String tag, boolean isOpeningTag, String attr, String attrValue)
    throws IOException
  {
    if (indent != null) out.write(indent.toString());
    if (isOpeningTag)
    {
      out.write("<");
    }
    else
    {
      out.write("</");
    }
    out.write(tag);
    if (isOpeningTag && attr != null)
    {
      out.write(' ');
      out.write(attr);
      out.write("=\"");
      out.write(attrValue);
      out.write('"');
    }
    out.write(">\n");
  }

  private void writeTagValue(Writer out, StringBuilder indent, String tag, String value)
    throws IOException
  {
    if (indent != null) out.write(indent.toString());
    out.write("<");
    out.write(tag);
    out.write(">");
    out.write(value == null ? "" : value);
    out.write("</");
    out.write(tag);
    out.write(">\n");
  }
}

class SequenceDiffEntry
{
  SequenceDefinition reference;
  SequenceDefinition target;

  SequenceDiffEntry(SequenceDefinition ref, SequenceDefinition tar)
  {
    reference = ref;
    target = tar;
  }
}

class PackageDiffEntry
{
  ReportPackage reference;
  ReportPackage target;

  PackageDiffEntry(ReportPackage reference, ReportPackage target)
  {
    this.reference = reference;
    this.target = target;
  }
}

class ProcDiffEntry
{
  ProcedureDefinition reference;
  ProcedureDefinition target;

  ProcDiffEntry(ProcedureDefinition ref, ProcedureDefinition tar)
  {
    reference = ref;
    target = tar;
  }
}

class DiffEntry
{
  TableIdentifier reference;
  TableIdentifier target;

  DiffEntry(TableIdentifier ref, TableIdentifier tar)
  {
    reference = ref;
    target = tar;
  }

  @Override
  public String toString()
  {
    if (target == null)
    {
      return reference.getType() + ": " + reference.getTableExpression();
    }
    else
    {
      return reference.getType() + ": " + reference.getTableExpression() + " to " + target.getType() + ": " + target.getTableExpression();
    }
  }
}
