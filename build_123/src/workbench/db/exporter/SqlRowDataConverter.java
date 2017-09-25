/*
 * SqlRowDataConverter.java
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
package workbench.db.exporter;

import java.util.ArrayList;
import java.util.List;

import workbench.interfaces.Committer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DropType;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.WbConnection;

import workbench.storage.BlobLiteralType;
import workbench.storage.DmlStatement;
import workbench.storage.MergeGenerator;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.RowDataContainer;
import workbench.storage.SqlLiteralFormatter;
import workbench.storage.StatementFactory;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

import static workbench.db.exporter.ExportType.*;

/**
 * Export data as SQL INSERT statements.
 *
 * @author Thomas Kellerer
 */
public class SqlRowDataConverter
  extends RowDataConverter
{
  // This instance can be re-used for several table exports from DataExporter
  // To prevent, that one failed export for the requested type resets the export type for subsequent tables
  // the requested sqlType is stored in sqlType.
  // Upon setting the ResultInfo in setResultInfo() the sqlTypeToUse is set accordingly and then
  // used in convertRowData()
  private ExportType sqlTypeToUse = ExportType.SQL_INSERT;
  private ExportType sqlType = ExportType.SQL_INSERT;

  private boolean createTable;
  private TableIdentifier alternateUpdateTable;
  private int commitEvery;
  private String concatString;
  private String chrFunction;
  private String concatFunction;
  private StatementFactory statementFactory;
  private String lineTerminator = "\n";
  private String doubleLineTerminator = "\n\n";
  private boolean includeOwner = true;
  private boolean doFormatting = true;
  private SqlLiteralFormatter literalFormatter;
  private boolean ignoreRowStatus = true;
  private String mergeType;
  private MergeGenerator mergeGenerator;
  private boolean transactionControl = true;
  private boolean includeIdentityCols;
  private boolean includeReadOnlyCols;
  private boolean useMultiRowInserts;
  private SQLLexer lexer;
  private boolean firstRow = true;

  public SqlRowDataConverter(WbConnection con)
  {
    super();
    setOriginalConnection(con);
    includeIdentityCols = !Settings.getInstance().getGenerateInsertIgnoreIdentity();
    includeReadOnlyCols = !Settings.getInstance().getCheckEditableColumns();
  }

  @Override
  public final void setOriginalConnection(WbConnection con)
  {
    super.setOriginalConnection(con);
    this.literalFormatter = new SqlLiteralFormatter(con);
    lexer = SQLLexerFactory.createLexer(con);
  }

  @Override
  public void setInfinityLiterals(InfinityLiterals literals)
  {
    super.setInfinityLiterals(literals);
    this.literalFormatter.setInfinityLiterals(literals);
  }

  public void setUseMultiRowInserts(boolean flag)
  {
    this.useMultiRowInserts = flag;
  }

  public void setTransactionControl(boolean flag)
  {
    this.transactionControl = flag;
  }

  public void setIncludeIdentityColumns(boolean flag)
  {
    includeIdentityCols = flag;
    if (statementFactory != null)
    {
      statementFactory.setIncludeIdentiyColumns(includeIdentityCols);
    }
  }

  public void setIncludeReadOnlyColumns(boolean flag)
  {
    includeReadOnlyCols = flag;
    if (statementFactory != null)
    {
      statementFactory.setIncludeReadOnlyColumns(includeReadOnlyCols );
    }
  }

  private boolean needPrimaryKey()
  {
    return this.sqlType == ExportType.SQL_DELETE_INSERT
        || this.sqlType == ExportType.SQL_UPDATE
        || this.sqlType == ExportType.SQL_MERGE
        || this.sqlType == ExportType.SQL_DELETE;
  }

  @Override
  public void setResultInfo(ResultInfo meta)
  {
    super.setResultInfo(meta);
    this.statementFactory = new StatementFactory(meta, this.originalConnection);
    this.statementFactory.setUseColumnLabel(true);
    this.needsUpdateTable = meta.getUpdateTable() == null;
    this.statementFactory.setIncludeTableOwner(this.includeOwner);
    this.statementFactory.setTableToUse(this.alternateUpdateTable);
    this.statementFactory.setIncludeIdentiyColumns(includeIdentityCols);
    this.statementFactory.setIncludeReadOnlyColumns(includeReadOnlyCols);

    boolean keysPresent = this.checkKeyColumns();
    this.sqlTypeToUse = this.sqlType;
    if (!keysPresent && needPrimaryKey())
    {
      String tbl = "";
      if (meta.getUpdateTable() != null)
      {
        tbl = " (" + meta.getUpdateTable().getTableName() + ")";
      }

      if (this.errorReporter != null)
      {
        String msg = ResourceMgr.getString("ErrExportNoKeys") + tbl;
        this.errorReporter.addWarning(msg);
      }

      LogMgr.logWarning("SqlRowDataConverter.setResultInfo()", "No key columns found" + tbl + " reverting back to INSERT generation");
      this.sqlTypeToUse = ExportType.SQL_INSERT;
    }
  }

  @Override
  public void setExporter(DataExporter exporter)
  {
    super.setExporter(exporter);
    if (exporter != null)
    {
      this.includeReadOnlyCols = exporter.getIncludeReadOnlyCols();
      this.includeIdentityCols = exporter.getIncludeIdentityCols();
    }
  }

  public void setMergeType(String type)
  {
    this.mergeType = type;
  }

  public void setDateLiteralType(String type)
  {
    if (this.literalFormatter != null)
    {
      this.literalFormatter.setDateLiteralType(type);
      this.literalFormatter.setInfinityLiterals(infinityLiterals);
    }
  }

  @Override
  public StringBuilder getEnd(long totalRows)
  {
    StringBuilder end = null;
    if (sqlTypeToUse == ExportType.SQL_MERGE)
    {
      end = new StringBuilder(100);
      end.append(getMergeEnd());
      end.append(lineTerminator);
    }

    if (sqlTypeToUse == ExportType.SQL_INSERT && useMultiRowInserts)
    {
      end = new StringBuilder(5);
      end.append(";");
      end.append(lineTerminator);
    }

    if (!transactionControl) return end;

    boolean writeCommit = true;
    if ( (commitEvery == Committer.NO_COMMIT_FLAG) || (commitEvery > 0 && (totalRows % commitEvery == 0)))
    {
      writeCommit = false;
    }

    if (writeCommit && totalRows > 0 || this.createTable && this.originalConnection.generateCommitForDDL())
    {
      if (end == null) end = new StringBuilder(12);
      end.append(lineTerminator);
      end.append("COMMIT;");
      end.append(lineTerminator);
    }
    return end;
  }

  public void setIgnoreColumnStatus(boolean flag)
  {
    this.ignoreRowStatus = flag;
  }

  public void setType(ExportType type)
  {
    switch (type)
    {
      case SQL_INSERT:
        setCreateInsert();
        break;
      case SQL_UPDATE:
        this.setCreateUpdate();
        break;
      case SQL_DELETE_INSERT:
        this.setCreateInsertDelete();
        break;
      case SQL_DELETE:
        this.setCreateDelete();
        break;
      case SQL_MERGE:
        this.setCreateMerge();
        break;
      default:
        throw new IllegalArgumentException("Invalid type specified");
    }
  }

  @Override
  public StringBuilder convertRowData(RowData row, long rowIndex)
  {
    if (sqlTypeToUse == ExportType.SQL_MERGE)
    {
      return appendMergeRow(row, rowIndex);
    }

    StringBuilder result = new StringBuilder();
    DmlStatement dml = null;

    this.statementFactory.setIncludeTableOwner(includeOwner);
    this.statementFactory.setIncludeIdentiyColumns(includeIdentityCols);
    this.statementFactory.setIncludeReadOnlyColumns(includeReadOnlyCols);
    this.statementFactory.setTableToUse(alternateUpdateTable);

    if (this.sqlTypeToUse == ExportType.SQL_DELETE_INSERT)
    {
      dml = this.statementFactory.createDeleteStatement(row, true);
      result.append(dml.getExecutableStatement(this.literalFormatter, this.originalConnection));
      result.append(';');
      result.append(lineTerminator);
    }

    if (this.sqlTypeToUse == ExportType.SQL_DELETE)
    {
      dml = this.statementFactory.createDeleteStatement(row, true);
    }

    if (this.sqlTypeToUse == ExportType.SQL_DELETE_INSERT || this.sqlType == ExportType.SQL_INSERT)
    {
      dml = this.statementFactory.createInsertStatement(row, ignoreRowStatus, "\n", this.exportColumns);
    }

    if (sqlType == ExportType.SQL_UPDATE)
    {
      dml = this.statementFactory.createUpdateStatement(row, ignoreRowStatus, "\n", this.exportColumns);
    }

    if (dml == null) return null;

    dml.setChrFunction(this.chrFunction);
    dml.setConcatString(this.concatString);
    dml.setConcatFunction(this.concatFunction);

    // Needed for formatting BLOBs in the literalFormatter
    this.currentRow = rowIndex;
    this.currentRowData = row;

    if (sqlTypeToUse == ExportType.SQL_INSERT && useMultiRowInserts)
    {
      dml.setFormatSql(false);
    }
    CharSequence sql = dml.getExecutableStatement(this.literalFormatter, this.originalConnection);

    if (sqlTypeToUse == ExportType.SQL_INSERT && useMultiRowInserts)
    {
      if (firstRow)
      {
        if (doFormatting)
        {
          appendWithLinebreaks(result, sql);
        }
        else
        {
          result.append(sql);
        }
        firstRow = false;
      }
      else
      {
        result.append(',');
        if (doFormatting)
        {
          result.append(lineTerminator);
          result.append("  ");
        }
        result.append(extractValuesPart(sql));
      }
    }
    else
    {
      result.append(sql);
      result.append(';');
      if (doFormatting)
      {
        result.append(doubleLineTerminator);
      }
      else
      {
        result.append(lineTerminator);
      }

      if (this.commitEvery > 0 && ((rowIndex + 1) % commitEvery) == 0)
      {
        result.append("COMMIT;");
        result.append(doubleLineTerminator);
      }
    }
    return result;
  }

  private MergeGenerator getMergeGenerator()
  {
    if (this.mergeGenerator == null)
    {
      if (mergeType != null)
      {
        mergeGenerator = MergeGenerator.Factory.createGenerator(mergeType);
      }
      else
      {
        mergeGenerator = MergeGenerator.Factory.createGenerator(originalConnection);
      }
    }
    return mergeGenerator;
  }

  private StringBuilder appendMergeRow(RowData row, long rowIndex)
  {
    MergeGenerator generator = getMergeGenerator();
    if (generator != null)
    {
      String merge = generator.addRow(metaData, row, rowIndex);
      StringBuilder result = new StringBuilder(merge);
      return result;
    }
    return null;
  }

  private String getMergeEnd()
  {
    MergeGenerator generator = getMergeGenerator();
    if (generator == null) return null;
    RowDataContainer data = RowDataContainer.Factory.createContainer(originalConnection, currentRowData, metaData);
    return generator.generateMergeEnd(data);
  }

  private StringBuilder getMergeStart()
  {
    MergeGenerator generator = getMergeGenerator();
    if (generator == null) return null;
    RowDataContainer data = RowDataContainer.Factory.createContainer(originalConnection, currentRowData, metaData);
    return new StringBuilder(generator.generateMergeStart(data));
  }

  @Override
  public StringBuilder getStart()
  {
    firstRow = true;

    if (sqlTypeToUse == ExportType.SQL_MERGE)
    {
      return getMergeStart();
    }

    if (!this.createTable) return null;

    TableIdentifier tableName = alternateUpdateTable != null ? alternateUpdateTable : this.metaData.getUpdateTable();
    if (tableName == null)
    {
      LogMgr.logError("SqlRowDataConverter.getStart()", "Cannot write create table without update table!",null);
      return null;
    }

    List<ColumnIdentifier> cols = CollectionUtil.arrayList(this.metaData.getColumns());
    TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(originalConnection);

    TableIdentifier toUse = tableName.createCopy();
    boolean useSchema = exporter == null ? true : exporter.getUseSchemaInSql();

    if (useSchema)
    {
      if (toUse.getSchema() == null)
      {
        toUse.setSchema(toUse.getSchemaToUse(originalConnection));
      }
      if (toUse.getCatalog() == null)
      {
        toUse.setCatalog(toUse.getCatalogToUse(originalConnection));
      }
    }
    else
    {
      toUse.setUseNameOnly(true);
    }
    boolean includePK = Settings.getInstance().getBoolProperty("workbench.sql.export.createtable.pk", true);

    CharSequence create = builder.getCreateTable(toUse, cols, null, null, DropType.none, false, includePK, useSchema);
    String source = create.toString();
    StringBuilder createSql = new StringBuilder(source);
    createSql.append(doubleLineTerminator);
    return createSql;
  }

  public void setCreateInsert()
  {
    this.sqlType = ExportType.SQL_INSERT;
    this.sqlTypeToUse = this.sqlType;
    this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.insert.doformat",true);
  }

  public void setCreateMerge()
  {
    this.sqlType = ExportType.SQL_MERGE;
    this.sqlTypeToUse = this.sqlType;
    this.doFormatting = false;
  }

  public void setCreateUpdate()
  {
    this.sqlType = ExportType.SQL_UPDATE;
    this.sqlTypeToUse = this.sqlType;
    this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.update.doformat",true);
  }

  public void setCreateInsertDelete()
  {
    this.sqlType = ExportType.SQL_DELETE_INSERT;
    this.sqlTypeToUse = this.sqlType;
    this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.insert.doformat",true);
  }

  public void setCreateDelete()
  {
    this.sqlType = ExportType.SQL_DELETE;
    this.sqlTypeToUse = this.sqlType;
    this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.delete.doformat",true);
  }

  public void setCommitEvery(int interval)
  {
    this.commitEvery = interval;
  }

  public void setConcatString(String concat)
  {
    if (concat == null) return;
    this.concatString = concat;
    this.concatFunction = null;
  }

  public void setConcatFunction(String func)
  {
    if (func == null) return;
    this.concatFunction = func;
    this.concatString = null;
  }

  public void setChrFunction(String function)
  {
    this.chrFunction = function;
  }

  /**
   * Setter for property createTable.
   * @param flag New value of property createTable.
   */
  public void setCreateTable(boolean flag)
  {
    this.createTable = flag;
  }

  /**
   * Setter for property alternateUpdateTable.
   * @param table New value of property alternateUpdateTable.
   */
  public void setAlternateUpdateTable(TableIdentifier table)
  {
    if (table != null)
    {
      this.alternateUpdateTable = table;
      this.needsUpdateTable = false;
    }
    else
    {
      this.alternateUpdateTable = null;
      this.needsUpdateTable = true;
    }
  }

  /**
   * Setter for property keyColumnsToUse.
   * @param cols New value of property keyColumnsToUse.
   */
  public void setKeyColumnsToUse(List<String> keyCols)
  {
    if (keyCols == null)
    {
      this.keyColumnsToUse = null;
    }
    else
    {
      this.keyColumnsToUse = new ArrayList<>(keyCols);
    }
  }

  public void setIncludeTableOwner(boolean flag)
  {
    this.includeOwner = flag;
  }

  public void setBlobMode(BlobMode type)
  {
    if (this.literalFormatter == null) return;

    if (type == BlobMode.DbmsLiteral)
    {
      literalFormatter.createDbmsBlobLiterals(originalConnection);
    }
    else if (type == BlobMode.AnsiLiteral)
    {
      literalFormatter.createAnsiBlobLiterals();
    }
    else if (type == BlobMode.SaveToFile)
    {
      literalFormatter.createBlobFiles(this);
    }
    else if (type == BlobMode.pgDecode)
    {
      literalFormatter.setBlobFormat(BlobLiteralType.pgDecode);
    }
    else if (type == BlobMode.pgEscape)
    {
      literalFormatter.setBlobFormat(BlobLiteralType.pgEscape);
    }
    else
    {
      literalFormatter.noBlobHandling();
    }
  }

  public void setClobAsFile(String encoding)
  {
    if (StringUtil.isEmptyString(encoding)) return;
    if (literalFormatter != null)
    {
      literalFormatter.setTreatClobAsFile(this, encoding);
    }
  }

  protected void appendWithLinebreaks(StringBuilder toAppend, CharSequence insert)
  {
    lexer.setInput(insert);
    SQLToken token = lexer.getNextToken(true, true);
    while (token != null)
    {
      if (token.getContents().equals("VALUES"))
      {
        toAppend.append(lineTerminator);
        toAppend.append(token.getContents());
        toAppend.append(lineTerminator);
        toAppend.append(' ');
      }
      else
      {
        toAppend.append(token.getText());
      }
      token = lexer.getNextToken(true, true);
    }
  }

  protected String extractValuesPart(CharSequence insert)
  {
    lexer.setInput(insert);
    SQLToken token = lexer.getNextToken(false, false);
    int valuesStart = -1; // the position of the first character after the VALUES clause
    while (token != null)
    {
      if (token.getText().equalsIgnoreCase("VALUES"))
      {
        valuesStart = token.getCharEnd();
      }
      token = lexer.getNextToken(false, false);
    }
    if (valuesStart > -1)
    {
      String values = insert.subSequence(valuesStart, insert.length()).toString();
      return values.trim();
    }
    return null;
  }

}
