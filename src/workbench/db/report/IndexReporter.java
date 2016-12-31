/*
 * IndexReporter.java
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
package workbench.db.report;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.log.LogMgr;

import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleIndexPartition;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Class to retrieve all index definitions for a table and
 * generate an XML string from that.
 *
 * @author  Thomas Kellerer
 */
public class IndexReporter
{
  public static final String TAG_INDEX = "index-def";

  public static final String TAG_INDEX_NAME = "name";
  public static final String TAG_INDEX_UNIQUE = "unique";
  public static final String TAG_INDEX_PK = "primary-key";
  public static final String TAG_INDEX_TYPE = "type";
  public static final String TAG_INDEX_EXPR = "index-expression";
  public static final String TAG_INDEX_COLUMN_LIST = "column-list";
  public static final String TAG_INDEX_COLUMN_NAME = "column";
  public static final String TAG_INDEX_OPTION = "index-option";
  public static final String TAG_INDEX_COMMENT = "comment";

  private List<IndexDefinition> indexList;
  private TagWriter tagWriter = new TagWriter();
  private String mainTagToUse;
  private Map<IndexDefinition, List<ObjectOption>> indexOptions = new TreeMap<>(IndexDefinition.getNameSorter());

  public IndexReporter(TableIdentifier tbl, WbConnection conn, boolean includePartitions)
  {
    indexList  = conn.getMetadata().getIndexReader().getTableIndexList(tbl, true);
    Collections.sort(indexList, IndexDefinition.getNameSorter());
    removeEmptyIndexes();
    if (includePartitions)
    {
      retrieveOracleOptions(conn);
    }
    retrieveSourceOptions(tbl, conn);
  }

  public IndexReporter(IndexDefinition index)
  {
    indexList = new ArrayList<>(1);
    indexList.add(index);
  }

  public void setMainTagToUse(String tag)
  {
    mainTagToUse = tag;
  }

  public void appendXml(StringBuilder result, StringBuilder indent, String pkName)
  {
    int numIndex = this.indexList.size();
    if (numIndex == 0) return;
    StringBuilder defIndent = new StringBuilder(indent);
    defIndent.append("  ");

    for (IndexDefinition index : indexList)
    {
      tagWriter.appendOpenTag(result, indent, mainTagToUse == null ? TAG_INDEX : mainTagToUse);
      result.append('\n');
      if (pkName != null && index.isPrimaryKeyIndex() )
      {
        tagWriter.appendTag(result, defIndent, TAG_INDEX_NAME, pkName);
      }
      else
      {
        tagWriter.appendTag(result, defIndent, TAG_INDEX_NAME, index.getName());
      }
      tagWriter.appendTag(result, defIndent, TAG_INDEX_EXPR, index.getExpression());
      tagWriter.appendTag(result, defIndent, TAG_INDEX_UNIQUE, index.isUnique());
      if (index.isUniqueConstraint())
      {
        tagWriter.appendTag(result, defIndent, ForeignKeyDefinition.TAG_CONSTRAINT_NAME, index.getUniqueConstraintName());
      }
      tagWriter.appendTag(result, defIndent, TAG_INDEX_PK, index.isPrimaryKeyIndex());
      tagWriter.appendTag(result, defIndent, TAG_INDEX_TYPE, index.getIndexType());
      if (StringUtil.isNonBlank(index.getComment()))
      {
        tagWriter.appendTag(result, defIndent, TAG_INDEX_COMMENT, index.getComment(), true);
      }

      List<IndexColumn> columns = index.getColumns();
      if (columns.size() > 0)
      {
        StringBuilder colIndent = new StringBuilder(defIndent);
        colIndent.append("  ");
        tagWriter.appendOpenTag(result, defIndent, TAG_INDEX_COLUMN_LIST);
        result.append('\n');
        for (IndexColumn col : columns)
        {

          List<TagAttribute> attrs = new ArrayList<>(2);
          attrs.add(new TagAttribute("name", SqlUtil.removeObjectQuotes(col.getColumn())));

          if (col.getDirection() != null)
          {
            attrs.add(new TagAttribute("direction", col.getDirection()));
          }
          tagWriter.appendOpenTag(result, colIndent, TAG_INDEX_COLUMN_NAME, attrs, false);
          result.append("/>\n");
        }
        tagWriter.appendCloseTag(result, defIndent, TAG_INDEX_COLUMN_LIST);
      }
      if (StringUtil.isNonBlank(index.getTablespace()))
      {
        tagWriter.appendTag(result, defIndent, ReportTable.TAG_TABLESPACE, index.getTablespace(), false);
      }
      writeDbmsOptions(result, defIndent, index);
      tagWriter.appendCloseTag(result, indent, mainTagToUse == null ? TAG_INDEX : mainTagToUse);
    }
  }

  private void writeDbmsOptions(StringBuilder output, StringBuilder indent, IndexDefinition index)
  {
    List<ObjectOption> options = indexOptions.get(index);
    if (CollectionUtil.isEmpty(options)) return;

    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");
    output.append(indent);
    output.append("<index-options>\n");
    for (ObjectOption option : options)
    {
      StringBuilder result = option.getXml(myindent);
      output.append(result);
    }
    output.append(indent);
    output.append("</index-options>\n");
  }

  private void retrieveSourceOptions(TableIdentifier table, WbConnection conn)
  {
    IndexReader reader = conn.getMetadata().getIndexReader();
    for (IndexDefinition index : indexList)
    {
      reader.getIndexOptions(table, index);
      Map<String, String> config = index.getSourceOptions().getConfigSettings();
      for (Map.Entry<String, String> entry : config.entrySet())
      {
        ObjectOption option = new ObjectOption(entry.getKey(), entry.getValue());
        option.setWriteFlaxXML(!TagWriter.needsCData(entry.getValue()));
        addOption(index, option);
      }
    }
  }

  private void retrieveOracleOptions(WbConnection conn)
  {
    if (!conn.getMetadata().isOracle()) return;

    try
    {
      for (IndexDefinition index : indexList)
      {
        OracleIndexPartition reader = new OracleIndexPartition(conn);
        reader.retrieve(index, conn);
        if (reader.isPartitioned())
        {
          ObjectOption option = new ObjectOption("partition", reader.getSourceForIndexDefinition());
          addOption(index, option);
        }
      }
    }
    catch (SQLException sql)
    {
      LogMgr.logWarning("IndexReporter.retrieveOracleOptions()", "Could not retrieve index options", sql);
    }
  }

  private void addOption(IndexDefinition index, ObjectOption option)
  {
    List<ObjectOption> options = indexOptions.get(index);
    if (options == null)
    {
      options = new ArrayList<>();
      indexOptions.put(index, options);
    }
    options.add(option);
  }

  public Collection<IndexDefinition> getIndexList()
  {
    return this.indexList;
  }

  private void removeEmptyIndexes()
  {
    if (indexList == null) return;
    Iterator<IndexDefinition> itr = indexList.iterator();
    while (itr.hasNext())
    {
      IndexDefinition idx = itr.next();
      if (idx.isEmpty())
      {
        itr.remove();
      }
    }
  }
}
