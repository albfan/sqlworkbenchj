/*
 * IndexDiff.java
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
package workbench.db.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import workbench.db.IndexDefinition;
import workbench.db.report.IndexReporter;
import workbench.db.report.TagAttribute;
import workbench.db.report.TagWriter;

import workbench.util.StringUtil;

/**
 * Compare two index definitions and create an XML
 * representation of the differences.
 *
 * @author  Thomas Kellerer
 */
public class IndexDiff
{
  public static final String TAG_MODIFY_INDEX = "modify-index";
  public static final String TAG_RENAME_INDEX = "rename-index";
  public static final String TAG_ADD_INDEX = "add-index";
  public static final String TAG_DROP_INDEX = "drop-index";
  public static final String TAG_FILTER_EXPRESSION = "filter-expression";

  private Collection<IndexDefinition> reference = Collections.emptyList();
  private Collection<IndexDefinition> target = Collections.emptyList();
  private final TagWriter writer = new TagWriter();
  private StringBuilder indent = StringUtil.emptyBuilder();

  public IndexDiff(Collection<IndexDefinition> ref, Collection<IndexDefinition> targ)
  {
    if (ref != null) this.reference = ref;
    if (targ != null) this.target = targ;
  }

  public void setIndent(StringBuilder ind)
  {
    if (indent == null)
    {
      this.indent = StringUtil.emptyBuilder();
    }
    else
    {
      this.indent = ind;
    }
  }

  public StringBuilder getMigrateTargetXml()
  {
    StringBuilder result = new StringBuilder();
    List<IndexDefinition> indexToAdd = new ArrayList<>();
    List<IndexDefinition> indexToDrop = new ArrayList<>();

    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");

    StringBuilder idxIndent = new StringBuilder(myindent);
    idxIndent.append("  ");

    for (IndexDefinition refIndex : reference)
    {
      IndexDefinition ind = this.findIndexInTarget(refIndex);
      if (ind == null)
      {
        indexToAdd.add(refIndex);
      }
      else
      {
        boolean uniqueDiff = ind.isUnique() != refIndex.isUnique();
        boolean pkDiff = ind.isPrimaryKeyIndex() != refIndex.isPrimaryKeyIndex();
        boolean typeDiff = !(ind.getIndexType().equals(refIndex.getIndexType()));
        boolean nameDiff = !(ind.getName().equalsIgnoreCase(refIndex.getName()));
        boolean filterDiff = !(StringUtil.equalStringOrEmpty(ind.getFilterExpression(), refIndex.getFilterExpression(), false));

        if (uniqueDiff || pkDiff || typeDiff || nameDiff || filterDiff)
        {
          writer.appendOpenTag(result, myindent, TAG_MODIFY_INDEX, "name", ind.getName());
          result.append('\n');

          // In order to completely create the correct SQL for the index change,
          // the full definition of the reference index needs to be included in the XML
          IndexReporter rep = new IndexReporter(refIndex);
          rep.setMainTagToUse("reference-index");
          rep.appendXml(result, idxIndent, null);

          StringBuilder changedIndent = new StringBuilder(idxIndent);
          changedIndent.append("  ");
          writer.appendOpenTag(result, idxIndent, "modified");
          result.append('\n');

          if (nameDiff)
          {
            TagAttribute oldAtt = new TagAttribute("oldvalue", ind.getName());
            TagAttribute newAtt = new TagAttribute("newvalue", refIndex.getName());
            writer.appendOpenTag(result, changedIndent, IndexReporter.TAG_INDEX_NAME, false, oldAtt, newAtt);
            result.append("/>\n");
          }

          if (uniqueDiff)
          {
            TagAttribute oldAtt = new TagAttribute("oldvalue", Boolean.toString(ind.isUnique()));
            TagAttribute newAtt = new TagAttribute("newvalue", Boolean.toString(refIndex.isUnique()));
            writer.appendOpenTag(result, changedIndent, IndexReporter.TAG_INDEX_UNIQUE, false, oldAtt, newAtt);
            result.append("/>\n");
          }
          if (pkDiff)
          {
            TagAttribute oldAtt = new TagAttribute("oldvalue", Boolean.toString(ind.isPrimaryKeyIndex()));
            TagAttribute newAtt = new TagAttribute("newvalue", Boolean.toString(refIndex.isPrimaryKeyIndex()));
            writer.appendOpenTag(result, changedIndent, IndexReporter.TAG_INDEX_PK, false, oldAtt, newAtt);
            result.append("/>\n");
          }
          if (typeDiff)
          {
            TagAttribute oldAtt = new TagAttribute("oldvalue", ind.getIndexType());
            TagAttribute newAtt = new TagAttribute("newvalue", refIndex.getIndexType());
            writer.appendOpenTag(result, changedIndent, IndexReporter.TAG_INDEX_TYPE, false, oldAtt, newAtt);
            result.append("/>\n");
          }

          if (filterDiff)
          {
            writer.appendOpenTag(result, changedIndent, TAG_FILTER_EXPRESSION);
            result.append('\n');
            StringBuilder filterIndent = new StringBuilder(changedIndent);
            filterIndent.append("  ");
            writer.appendTag(result, filterIndent, "target-expression", ind.getFilterExpression());
            writer.appendTag(result, filterIndent, "reference-expression", refIndex.getFilterExpression());
            writer.appendCloseTag(result, changedIndent, TAG_FILTER_EXPRESSION);
          }

          writer.appendCloseTag(result, idxIndent, "modified");
          writer.appendCloseTag(result, myindent, TAG_MODIFY_INDEX);
        }
      }
    }

    for (IndexDefinition targetIndex : target)
    {
      IndexDefinition ind = this.findIndexInReference(targetIndex);
      if (ind == null)
      {
        indexToDrop.add(targetIndex);
      }
    }

    if (indexToAdd.size() > 0)
    {
      writer.appendOpenTag(result, myindent, TAG_ADD_INDEX);
      result.append('\n');
      for (IndexDefinition idx : indexToAdd)
      {
        IndexReporter rep = new IndexReporter(idx);
        rep.appendXml(result, idxIndent, null);
      }
      writer.appendCloseTag(result, myindent, TAG_ADD_INDEX);
    }

    if (indexToDrop.size() > 0)
    {
      for (IndexDefinition idx : indexToDrop)
      {
        writer.appendTag(result, myindent, TAG_DROP_INDEX, idx.getName());
      }
    }
    return result;
  }

  private IndexDefinition findIndexInTarget(IndexDefinition toCheck)
  {
    return findIndex(target, toCheck);
  }

  private IndexDefinition findIndexInReference(IndexDefinition toCheck)
  {
    return findIndex(reference, toCheck);
  }

  private IndexDefinition findIndex(Collection<IndexDefinition> defs, IndexDefinition toCheck)
  {
    for (IndexDefinition idx : defs)
    {
      if (idx.equals(toCheck)) return idx;
    }
    return null;
  }
}
