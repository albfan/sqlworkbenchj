/*
 * SchemaReporter.java
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
package workbench.db.report;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.BaseObjectType;
import workbench.db.ConnectionInfoBuilder;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbObjectComparator;
import workbench.db.DbSettings;
import workbench.db.ProcedureDefinition;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.diff.SchemaDiff;

import workbench.storage.RowActionMonitor;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;


/**
 * Generate an report from a selection of database tables.
 *
 * @author Thomas Kellerer
 */
public class SchemaReporter
  implements Interruptable
{
  public static final String TAG_SETTINGS = "report-settings";
  public static final String TAG_TRG_INFO = "include-triggers";
  public static final String TAG_PARTITION_INFO = "include-partitions";
  public static final String TAG_OBJECT_TYPES = "included-types";

  private WbConnection dbConn;
  private List<DbObject> objects = new ArrayList<>();
  private List<ReportProcedure> procedures = new ArrayList<>();
  private List<ReportSequence> sequences = new ArrayList<>();
  private Set<String> types;
  private List<String> schemas;

  private TagWriter tagWriter = new TagWriter();
  private RowActionMonitor monitor;
  private String outputfile;
  protected volatile boolean cancel;
  private String procedureNames;
  private boolean includeGrants;
  private boolean includeTriggers;
  private boolean includePartitions;
  private String schemaNameToUse = null;
  private String reportTitle = null;
  private boolean fullObjectSource;

  /**
   * Creates a new SchemaReporter for the supplied connection
   * @param conn The connection that the schema report should use
   */
  public SchemaReporter(WbConnection conn)
  {
    this.dbConn = conn;
    types = CollectionUtil.caseInsensitiveSet();
    types.addAll(conn.getMetadata().getTableTypes());
    types.addAll(this.dbConn.getDbSettings().getViewTypes());
  }

  public void setProgressMonitor(RowActionMonitor mon)
  {
    this.monitor = mon;
  }

  public void setReportTitle(String title)
  {
    this.reportTitle = title;
  }

  /**
   * Define the list of tables to run the report on.
   * The tables are added to the existing list of tables
   *
   */
  public void setObjectList(List<? extends DbObject> objectList)
  {
    DbMetadata meta = dbConn.getMetadata();
    SequenceReader reader = meta.getSequenceReader();

    for (DbObject dbo : objectList)
    {
      if (meta.isSequenceType(dbo.getObjectType()) && reader != null)
      {
        SequenceDefinition seq = reader.getSequenceDefinition(dbo.getCatalog(), dbo.getSchema(), dbo.getObjectName());
        this.sequences.add(new ReportSequence(seq));
      }
      else if (meta.isExtendedObject(dbo) && dbo instanceof TableIdentifier)
      {
        DbObject details = meta.getObjectDefinition((TableIdentifier)dbo);
        this.objects.add(details);
      }
      else
      {
        this.objects.add(dbo);
      }
    }
  }

  /**
   * Return the count of "table like" objects.
   * Stored procedures are not taken into account.
   */
  public int getObjectCount()
  {
    return objects.size() + sequences.size() + procedures.size();
  }

  public void clearObjects()
  {
    objects.clear();
    sequences.clear();
    procedures.clear();
  }

  public void setSchemas(List<String> list)
  {
    if (list == null || list.isEmpty()) return;
    this.schemas = list;
    this.schemaNameToUse = null;
  }

  public void setSchemaNameToUse(String name)
  {
    if (this.schemas == null || this.schemas.size() == 1)
    {
      this.schemaNameToUse = name;
    }
  }

  public void setProcedureNames(String name)
  {
    this.procedureNames = name;
  }

  public boolean proceduresIncluded()
  {
    return procedureNames != null;
  }

  public void setCreateFullObjectSource(boolean flag)
  {
    fullObjectSource = flag;
  }

  public void setIncludeProcedures(boolean flag)
  {
    if (flag)
    {
      this.procedureNames = "*";
    }
    else
    {
      this.procedureNames = null;
    }
  }

  public void setIncludeGrants(boolean flag)
  {
    this.includeGrants = flag;
  }

  public void setIncludePartitions(boolean flag)
  {
    this.includePartitions = flag;
  }

  public void setIncludeTriggers(boolean flag)
  {
    includeTriggers = flag;
  }

  public void setOutputFilename(String filename)
  {
    this.outputfile = filename;
  }

  /**
   * Return the XML for the (selected) tables
   * @return The XML for all tables
   */
  public String getXml()
  {
    this.cancel = false;
    StringWriter out = new StringWriter(5000);
    try
    {
      this.writeXml(out);
    }
    catch (Exception e)
    {
      // Cannot happen with StrWriter
    }
    return out.toString();
  }

  public void writeXml()
    throws IOException, SQLException
  {
    BufferedWriter bw = null;

    try
    {
      bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.outputfile), "UTF-8"), 16*1024);
      this.writeXml(bw);
    }
    catch (IOException io)
    {
      LogMgr.logError("WbSchemaReport.execute()", "Error writing report", io);
      throw io;
    }
    finally
    {
      FileUtil.closeQuietely(bw);
    }
  }

  private void sortTableObjects()
  {
    Collections.sort(objects, new DbObjectComparator());
  }
  /**
   *  Write the XML into the supplied output
   */
  public void writeXml(Writer out)
    throws IOException, SQLException
  {
    this.cancel = false;

    sortTableObjects();
    if (proceduresIncluded() && this.procedures.isEmpty()) this.retrieveProcedures();
    if (this.cancel) return;

    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    out.write("<");
    out.write("schema-report>\n\n");
    writeReportInfo(out);
    out.write("\n");

    int totalCount = this.objects.size() + this.procedures.size() + this.sequences.size();
    int totalCurrent = 1;

    DbSettings dbs = dbConn.getMetadata().getDbSettings();

    for (DbObject object : objects)
    {
      try
      {
        if (this.cancel) break;

        String tableName = object.getObjectExpression(dbConn);
        if (this.monitor != null)
        {
          this.monitor.setCurrentObject(tableName, totalCurrent, totalCount);
        }

        String type = object.getObjectType();

        if (dbs.isViewType(type))
        {
          ReportView rview = new ReportView((TableIdentifier)object, this.dbConn, true, includeGrants, fullObjectSource);
          rview.setSchemaNameToUse(this.schemaNameToUse);
          rview.writeXml(out);
        }
        else if (object instanceof TableIdentifier)
        {
          ReportTable rtable = new ReportTable((TableIdentifier)object, this.dbConn, true, true, true, true, includeGrants, includeTriggers, includePartitions);
          rtable.setSchemaNameToUse(this.schemaNameToUse);
          rtable.writeXml(out);
          rtable.done();
        }
        else if (object instanceof BaseObjectType)
        {
          ReportObjectType repType = new ReportObjectType((BaseObjectType)object);
          repType.writeXml(out);
        }
        else
        {
          GenericReportObject genObject = new GenericReportObject(dbConn, object);
          genObject.writeXml(out);
        }
        out.flush();
        totalCurrent ++;
      }
      catch (Exception e)
      {
        LogMgr.logError("SchemaReporter.writeXml()", "Error writing table: " + object, e);
      }
    }

    if (this.procedures.size() > 0) out.write("\n");
    for (ReportProcedure proc : procedures)
    {
      if (this.monitor != null)
      {
        this.monitor.setCurrentObject(proc.getProcedureName(), totalCurrent, totalCount);
      }

      proc.writeXml(out);
      out.write('\n');
      totalCurrent ++;
      if (this.cancel) break;
    }

    if (this.sequences.size() > 0) out.write("\n");
    for (ReportSequence seq : sequences)
    {
      String name = seq.getSequence().getSequenceName();
      if (this.monitor != null)
      {
        this.monitor.setCurrentObject(name, totalCurrent, totalCount);
      }
      seq.writeXml(out);
      out.write('\n');
      totalCurrent ++;
      if (this.cancel) break;
    }

    out.write("</");
    out.write("schema-report>");
    out.flush();
  }

  /**
   *  Writes basic information about the report
   */
  private void writeReportInfo(Writer out)
    throws IOException
  {
    StringBuilder info = new StringBuilder();
    StringBuilder indent = new StringBuilder("  ");
    StringBuilder indent2 = new StringBuilder("    ");

    if (!StringUtil.isEmptyString(this.reportTitle))
    {
      this.tagWriter.appendTag(info, indent, "report-title", this.reportTitle);
    }

    ConnectionInfoBuilder builder = new ConnectionInfoBuilder();
    info.append(builder.getDatabaseInfoAsXml(dbConn, indent));

    TagWriter tw = new TagWriter();
    info.append(indent);
    info.append("<generated-at>");
    info.append(StringUtil.getCurrentTimestampWithTZString());
    info.append("</generated-at>\n\n");

    tw.appendOpenTag(info, indent, TAG_SETTINGS);
    info.append('\n');

    tw.appendTag(info, indent2, SchemaDiff.TAG_GRANT_INFO, includeGrants);
    tw.appendTag(info, indent2, TAG_PARTITION_INFO, includePartitions);
    tw.appendTag(info, indent2, TAG_TRG_INFO, includeTriggers);
    tw.appendTag(info, indent2, SchemaDiff.TAG_FULL_SOURCE, fullObjectSource);

    if (CollectionUtil.isNonEmpty(types))
    {
      tw.appendOpenTag(info, indent2, TAG_OBJECT_TYPES);
      info.append('\n');
      StringBuilder indent3 = new StringBuilder(indent2);
      indent3.append("  ");
      for (String type : types)
      {
        tw.appendTag(info, indent3, "type", type);
      }
      tw.appendCloseTag(info, indent2, TAG_OBJECT_TYPES);
    }
    tw.appendCloseTag(info, indent, TAG_SETTINGS);
    out.write(info.toString());
  }


  /**
   * Cancel the current reporting process (this might leave a corrupted XML file)
   */
  @Override
  public void cancelExecution()
  {
    this.cancel = true;
  }

  @Override
  public boolean confirmCancel()
  {
    return true;
  }

  public void retrieveProcedures()
  {
    if (!this.proceduresIncluded()) return;
    this.procedures.clear();

    if (this.monitor != null)
    {
      this.monitor.setCurrentObject(ResourceMgr.getString("MsgRetrievingProcedures"), -1, -1);
    }
    if (CollectionUtil.isEmpty(schemas))
    {
      this.retrieveProcedures(null);
    }
    else
    {
      for (String schema : schemas)
      {
        this.retrieveProcedures(schema);
        if (this.cancel) return;
      }
    }
    if (this.monitor != null)
    {
      this.monitor.setCurrentObject(null, -1, -1);
    }
  }

  private void retrieveProcedures(String targetSchema)
  {
    try
    {
      List<String> names = StringUtil.stringToList(procedureNames, ",", true, true, false, false);

      String schema = this.dbConn.getMetadata().adjustSchemaNameCase(targetSchema);
      List<ProcedureDefinition> procs = null;

      for (String name : names)
      {
        if (this.cancel) return;
        String searchName = dbConn.getMetadata().adjustObjectnameCase(name);
        if (this.dbConn.getDbSettings().supportsSchemas())
        {
          procs = this.dbConn.getMetadata().getProcedureReader().getProcedureList(null, schema, searchName);
        }
        else
        {
          procs = this.dbConn.getMetadata().getProcedureReader().getProcedureList(schema, null, searchName);
        }
      }

      Set<String> oraPackages = new HashSet<>();

      for (ProcedureDefinition def : procs)
      {
        // Object types are reported with the "normal" objects
        if (def.isOracleObjectType()) continue;

        if (def.isPackageProcedure())
        {
          // Make sure Oracle packages are only reported once
          // getProcedureList() will return a procedure definition for each procedure/function of a package
          if (oraPackages.contains(def.getPackageName())) continue;
          oraPackages.add(def.getPackageName());
        }
        ReportProcedure proc = new ReportProcedure(def, this.dbConn);
        this.procedures.add(proc);
        if (this.cancel) return;
      }
    }
    catch (SQLException e)
    {
      LogMgr.logError("SchemaReporter.retrieveProcedures()", "Error retrieving procedures", e);
    }
  }

}
