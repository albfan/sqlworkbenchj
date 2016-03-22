/*
 * ReportTable.java
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

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.log.LogMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.ConstraintReader;
import workbench.db.DependencyNode;
import workbench.db.IndexDefinition;
import workbench.db.ReaderFactory;
import workbench.db.TableCommentReader;
import workbench.db.TableConstraint;
import workbench.db.TableDefinition;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleTablePartition;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to hold information about a database table that
 * will eventually be stored in an XML report.
 * It uses a {@link workbench.db.TableIdentifier} to store the
 * table's name, and {@link workbench.db.ColumnIdentifier} to
 * store the table's columns.
 * When initialized with a connection, it tries to find the primary
 * and foreign key constraints as well.
 *
 * Primary feature of this class is that it can create an XML
 * representation of itself.
 *
 * @author  Thomas Kellerer
 */
public class ReportTable
{
  public static final String TAG_TABLE_DEF = "table-def";
  public static final String TAG_TABLE_NAME = "table-name";
  public static final String TAG_TABLE_OWNER = "table-owner";
  public static final String TAG_TABLE_CATALOG = "table-catalog";
  public static final String TAG_TABLE_SCHEMA = "table-schema";
  public static final String TAG_TABLE_COMMENT = "table-comment";
  public static final String TAG_TABLE_PK_NAME = "primary-key-name";
  public static final String TAG_TABLE_CONSTRAINTS = "table-constraints";
  public static final String TAG_CONSTRAINT_DEF = "constraint-definition";
  public static final String TAG_CONSTRAINT_COMMENT = "constraint-comment";
  public static final String TAG_TABLESPACE = "tablespace";
  public static final String TAG_TABLE_TYPE = "table-type";

  private TableIdentifier table;
  private Map<String, ForeignKeyDefinition> foreignKeys = new TreeMap<>();
  private List<ReportColumn> columns;
  private IndexReporter reporter;
  private String tableComment;
  private TagWriter tagWriter = new TagWriter();
  private String schemaNameToUse;
  private boolean includePrimaryKey = true;
  private boolean includePartitions;
  private List<TableConstraint> tableConstraints;
  private List<TriggerDefinition> triggers;
  private ReportTableGrants grants;
  private final List<ObjectOption> dbmsOptions = new ArrayList<>();
  private boolean fixDefaultValues;

  /**
   * Initialize this ReportTable.
   * This will read the following information for the table:
   * <ul>
   *  <li>columns for the table using {@link workbench.db.DbMetadata#getTableColumns(TableIdentifier)}</li>
   *  <li>the comments for the table using {@link workbench.db.TableCommentReader#getTableComment(WbConnection, TableIdentifier)}</li>
   *  <li>The defined indexes for the table if includeIndex == true using an {@link IndexReporter}</li>
   *  <li>The defined foreign keys if includeFK == true</li>
   *  <li>Table constraints if includeConstraints == true {@link workbench.db.ConstraintReader#getTableConstraints(workbench.db.WbConnection, workbench.db.TableDefinition) }</li>
   *</ul>
   */
  public ReportTable(TableIdentifier tbl, WbConnection conn,
      boolean includeIndex,
      boolean includeFk,
      boolean includePk,
      boolean includeConstraints,
      boolean includeGrants,
      boolean includeTriggers,
      boolean includePartitioning)
    throws SQLException
  {
    this.includePrimaryKey = includePk;
    this.includePartitions = includePartitioning;
    this.fixDefaultValues = !conn.getDbSettings().returnsValidDefaultExpressions();

    // By using getTableDefinition() the TableIdentifier is completely initialized
    // (mainly it will contain the primary key name, which it doesn't when the TableIdentifier
    // was created using getTableList()
    TableDefinition def = conn.getMetadata().getTableDefinition(tbl);
    this.table = def.getTable();
    this.table.checkQuotesNeeded(conn);

    List<ColumnIdentifier> cols = def.getColumns();
    Collections.sort(cols);

    if (table.commentIsDefined())
    {
      this.tableComment = table.getComment();
    }
    else
    {
      TableCommentReader reader = new TableCommentReader();
      this.tableComment = reader.getTableComment(conn, this.table);
    }

    String schema = this.table.getSchema();
    if (schema == null || schema.length() == 0)
    {
      // This is important for e.g. Oracle. Otherwise the table definition
      // will contain multiple columns if a table exists more then once in
      // different schemas with the same name
      schema = conn.getMetadata().getSchemaToUse();
      if (schema != null) this.table.setSchema(schema);
    }

    this.setColumns(cols);

    if (includeIndex)
    {
      this.reporter = new IndexReporter(table, conn, includePartitioning);
    }

    if (includeFk)
    {
      this.readForeignKeys(conn);
    }

    if (includeConstraints)
    {
      ConstraintReader consReader = ReaderFactory.getConstraintReader(conn.getMetadata());
      this.tableConstraints = consReader.getTableConstraints(conn, def);
    }

    if (includeGrants)
    {
      grants = new ReportTableGrants(conn, this.table);
    }

    if (includeTriggers)
    {
      TriggerReader trgReader = TriggerReaderFactory.createReader(conn);
      try
      {
        triggers = trgReader.getTriggerList(table.getCatalog(), table.getSchema(), table.getTableName());
        if (triggers != null)
        {
          for (TriggerDefinition trg : triggers)
          {
            trg.setSource(trgReader.getTriggerSource(trg, false));
          }
        }
      }
      catch (SQLException e)
      {
        LogMgr.logError("ReportTable.<init>", "Could not retrieve table triggers", e);
        triggers = null;
      }
    }
    retrieveOptions(conn);
  }

  public ReportTable(TableIdentifier tbl)
  {
    this.table = tbl;
  }

  public boolean grantsIncluded()
  {
    return grants != null;
  }

  public List<ObjectOption> getDbmsOptions()
  {
    return Collections.unmodifiableList(dbmsOptions);
  }

  private List<ColumnIdentifier> getColumnList()
  {
    List<ColumnIdentifier> cols = new ArrayList<>(columns.size());
    for (ReportColumn col : columns)
    {
      cols.add(col.getColumn());
    }
    return cols;
  }

  private void retrieveOptions(WbConnection conn)
    throws SQLException
  {
    TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(conn);

    if (conn.getMetadata().isOracle() && includePartitions)
    {
      OracleTablePartition partition = new OracleTablePartition(conn);
      partition.retrieve(this.table, conn);
      if (partition.isPartitioned())
      {
        String source = partition.getSourceForTableDefinition();
        ObjectOption option = new ObjectOption("partition", source);
        dbmsOptions.add(option);
      }
      builder.setIncludePartitions(false); // no need to retrieve it twice
    }
    else
    {
      builder.setIncludePartitions(includePartitions);
    }

    builder.readTableOptions(table, getColumnList());

    Map<String, String> options = table.getSourceOptions().getConfigSettings();
    if (!options.isEmpty())
    {
      for (Map.Entry<String, String> entry : options.entrySet())
      {
        ObjectOption option = new ObjectOption(entry.getKey(), entry.getValue());
        option.setWriteFlaxXML(true);
        dbmsOptions.add(option);
      }
    }
  }

  public List<TriggerDefinition> getTriggers()
  {
    return triggers;
  }

  /**
   * Returns the ReportTableGrants for this table. If table grants
   * are not included, it will return null.
   */
  public ReportTableGrants getGrants()
  {
    return grants;
  }

  /**
   *  Return the list of column names (String)
   *  that make up the primary key of this table
   *  If the table has no primary key, an empty list
   *  is returned.
   */
  public List<String> getPrimaryKeyColumns()
  {
    if (!includePrimaryKey) return Collections.emptyList();
    if (table.getPrimaryKey() == null) return Collections.emptyList();
    return table.getPrimaryKey().getColumns();
  }

  /**
   *  Return the name of the primary key
   */
  public String getPrimaryKeyName()
  {
    if (!includePrimaryKey) return null;
    if (table.getPrimaryKey() == null) return null;
    return table.getPrimaryKeyName();
  }

  /**
   * Define the columns that belong to this table
   */
  public final void setColumns(List<ColumnIdentifier> cols)
  {
    if (cols == null) return;
    int numCols = cols.size();
    this.columns = new ArrayList<>(numCols);
    for (ColumnIdentifier col : cols)
    {
      ReportColumn repCol = new ReportColumn(col);
      repCol.setFixDefaultValue(fixDefaultValues);
      columns.add(repCol);
    }
  }

  private void readForeignKeys(WbConnection conn)
  {
    TableDependency dep = new TableDependency(conn, this.table);
    List<DependencyNode> keys = dep.getOutgoingForeignKeys();
    for (DependencyNode node : keys)
    {
      ForeignKeyDefinition def = new ForeignKeyDefinition(node);
      def.setCompareFKRules(true);

      TableIdentifier tbl = node.getTable().createCopy();
      if (tbl.getSchema() == null)
      {
        tbl.setSchema(this.table.getSchema());
      }
      if (tbl.getCatalog() == null)
      {
        tbl.setCatalog(this.table.getCatalog());
      }
      def.setForeignTable(new ReportTable(tbl));

      Map<String, String> colMap = node.getColumns();
      for (Map.Entry<String, String> entry : colMap.entrySet())
      {
        ReportColumn rcol = this.findColumn(entry.getValue());
        if (rcol != null)
        {
          ColumnReference ref = new ColumnReference(def);
          ref.setForeignColumn(entry.getKey());
          rcol.setForeignKeyReference(ref);
        }
      }
      foreignKeys.put(node.getFkName(), def);
    }
  }

  public Map<String, ForeignKeyDefinition> getForeignKeys()
  {
    return foreignKeys;
  }

  /**
   * Find a column witht the given name.
   */
  public ReportColumn findColumn(String col)
  {
    return findColumn(columns, col);
  }

  public static ReportColumn findColumn(List<ReportColumn> cols, String toFind)
  {
    if (toFind == null) return null;
    if (cols == null) return null;

    ReportColumn result = null;

    for (ReportColumn col : cols)
    {
      if (toFind.equalsIgnoreCase(col.getColumn().getColumnName()))
      {
        result = col;
        break;
      }
    }
    return result;
  }

  public Collection<IndexDefinition> getIndexList()
  {
    if (this.reporter == null) return null;
    return this.reporter.getIndexList();
  }

  public List<ReportColumn> getColumnsSorted()
  {
    Comparator<ReportColumn> comp = (ReportColumn o1, ReportColumn o2) ->
    {
      int pos1 = o1.getColumn().getPosition();
      int pos2 = o2.getColumn().getPosition();
      return pos1 - pos2;
    };
    List<ReportColumn> result = new ArrayList<>(columns.size());
    result.addAll(columns);
    Collections.sort(result, comp);
    return result;
  }

  public List<ReportColumn> getColumns()
  {
    return new ArrayList<>(this.columns);
  }

  public TableIdentifier getTable()
  {
    return this.table;
  }

  public void setSchemaNameToUse(String name)
  {
    this.schemaNameToUse = name;
  }

  public void writeXml(Writer out)
    throws IOException
  {
    StringBuilder line = this.getXml();
    out.append(line);
  }

  public StringBuilder getXml()
  {
    return getXml(new StringBuilder("  "));
  }

  @Override
  public String toString()
  {
    return this.table.toString();
  }

  public String getTableComment()
  {
    return this.tableComment;
  }

  public List<TableConstraint> getTableConstraints()
  {
    return this.tableConstraints;
  }

  public void appendTableNameXml(StringBuilder toAppend, StringBuilder indent)
  {
    tagWriter.appendTag(toAppend, indent, TAG_TABLE_CATALOG, SqlUtil.removeObjectQuotes(this.table.getCatalog()));
    tagWriter.appendTag(toAppend, indent, TAG_TABLE_SCHEMA, (StringUtil.isBlank(this.schemaNameToUse) ? SqlUtil.removeObjectQuotes(this.table.getSchema()) : this.schemaNameToUse));
    tagWriter.appendTag(toAppend, indent, TAG_TABLE_NAME, SqlUtil.removeObjectQuotes(this.table.getTableName()));
    String owner = table.getOwner();
    if (owner != null)
    {
      tagWriter.appendTag(toAppend, indent, TAG_TABLE_OWNER, SqlUtil.removeObjectQuotes(owner));
    }
  }

  /**
   * Return an XML representation of this table information.
   * The columns will be listed alphabetically not in the order
   * they were retrieved from the database.
   */
  public StringBuilder getXml(StringBuilder indent)
  {
    StringBuilder line = new StringBuilder(500);
    StringBuilder colindent = new StringBuilder(indent);
    colindent.append("  ");

    String type = this.table.getType();

    if (!"TABLE".equalsIgnoreCase(type))
    {
      String[] att = new String[] {"name", "type"};
      String[] val = new String[] { SqlUtil.removeObjectQuotes(this.table.getTableName()), type };
      tagWriter.appendOpenTag(line, indent, TAG_TABLE_DEF, att, val);
    }
    else
    {
      tagWriter.appendOpenTag(line, indent, TAG_TABLE_DEF, "name", SqlUtil.removeObjectQuotes(this.table.getTableName()));
    }
    line.append('\n');
    appendTableNameXml(line, colindent);
    tagWriter.appendTag(line, colindent, TAG_TABLE_PK_NAME, table.getPrimaryKeyName(), false);
    tagWriter.appendTag(line, colindent, TAG_TABLE_COMMENT, this.tableComment, true);

    if (StringUtil.isNonBlank(table.getTablespace()))
    {
      tagWriter.appendTag(line, colindent, TAG_TABLESPACE, table.getTablespace(), false);
    }

    String modifier = table.getSourceOptions().getTypeModifier();
    if (StringUtil.isNonBlank(modifier))
    {
      tagWriter.appendTag(line, colindent, TAG_TABLE_TYPE, modifier, false);
    }

    for (ReportColumn col : columns)
    {
      col.appendXml(line, colindent);
    }
    if (this.reporter != null) this.reporter.appendXml(line, colindent);

    writeConstraints(tableConstraints, tagWriter, line, colindent);

    if (this.foreignKeys.size() > 0)
    {
      tagWriter.appendOpenTag(line, colindent, "foreign-keys");
      line.append('\n');
      StringBuilder fkindent = new StringBuilder(colindent);
      fkindent.append("  ");
      for (ForeignKeyDefinition def : foreignKeys.values())
      {
        line.append(def.getXml(fkindent));
      }
      tagWriter.appendCloseTag(line, colindent, "foreign-keys");
    }
    if (this.grants != null)
    {
      this.grants.appendXml(line, colindent);
    }
    if (triggers != null)
    {
      for (TriggerDefinition trg : triggers)
      {
        ReportTrigger rtrig = new ReportTrigger(trg);
        rtrig.setIndent(colindent);
        line.append(rtrig.getXml());
      }
    }
    writeDBMSOptions(line, indent);
    tagWriter.appendCloseTag(line, indent, TAG_TABLE_DEF);
    return line;
  }

  private void writeDBMSOptions(StringBuilder output, StringBuilder indent)
  {
    if (CollectionUtil.isEmpty(dbmsOptions)) return;

    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");
    output.append(myindent);
    output.append("<table-options>\n");
    StringBuilder nextindent = new StringBuilder(myindent);
    nextindent.append("  ");
    for (ObjectOption option : dbmsOptions)
    {
      StringBuilder result = option.getXml(nextindent);
      output.append(result);
    }
    output.append(myindent);
    output.append("</table-options>\n");
  }

  public static void writeConstraints(List<TableConstraint> constraints, TagWriter tagWriter, StringBuilder line, StringBuilder indent)
  {
    if (constraints != null && constraints.size() > 0)
    {
      tagWriter.appendOpenTag(line, indent, TAG_TABLE_CONSTRAINTS);
      line.append('\n');
      StringBuilder consIndent = new StringBuilder(indent);
      consIndent.append("  ");
      for (TableConstraint cons : constraints)
      {
        writeConstraint(cons, tagWriter, line, consIndent);
      }
      tagWriter.appendCloseTag(line, indent, TAG_TABLE_CONSTRAINTS);
    }
  }

  public static void writeConstraint(TableConstraint constraint, TagWriter tagWriter, StringBuilder line, StringBuilder indent)
  {
    if (constraint == null) return;
    String name = constraint.getConstraintName();
    String expr = constraint.getExpression();
    String systemName = Boolean.toString(constraint.isSystemName());

    TagAttribute type = new TagAttribute("type", constraint.getType());
    TagAttribute sysName = null;
    TagAttribute nameAttr = null;

    if (name != null)
    {
      nameAttr = new TagAttribute("name", name);
      sysName = new TagAttribute("generated-name", systemName);
    }
    tagWriter.appendCDATATag(line, indent, ReportTable.TAG_CONSTRAINT_DEF, expr, type, sysName, nameAttr);
  }

  public void done()
  {
    this.columns = null;
  }

  @Override
  public int hashCode()
  {
    int hash = 17 * 7 + (this.table != null ? this.table.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object other)
  {
    if (other instanceof ReportTable)
    {
      return equals((ReportTable)other);
    }
    return false;
  }

  public boolean equals(ReportTable other)
  {
    return this.table.getTableName().equalsIgnoreCase(other.table.getTableName());
  }

}
