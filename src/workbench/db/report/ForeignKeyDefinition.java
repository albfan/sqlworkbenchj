/*
 * ForeignKeyDefinition.java
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

import java.util.Iterator;
import java.util.Map;

import workbench.db.DependencyNode;

import workbench.util.NumberStringCache;

/**
 *
 * @author  Thomas Kellerer
 */
public class ForeignKeyDefinition
{
  public static final String TAG_FOREIGN_KEY = "foreign-key";
  public static final String TAG_CONSTRAINT_NAME = "constraint-name";
  public static final String TAG_SOURCE_COLS = "source-columns";
  public static final String TAG_TARGET_COLS = "referenced-columns";
  public static final String TAG_UPDATE_RULE = "update-rule";
  public static final String TAG_DELETE_RULE = "delete-rule";
  public static final String TAG_DEFER_RULE = "deferrable";

  private DependencyNode fkDefinition;

  private ReportTable foreignTable;
  private TagWriter tagWriter = new TagWriter();
  private boolean compareFKRules;

  public ForeignKeyDefinition(DependencyNode node)
  {
    this.fkDefinition = node;
  }

  public void setForeignTable(ReportTable table)
  {
    foreignTable = table;
  }

  public boolean isEnabled()
  {
    return fkDefinition.isEnabled();
  }

  public void setCompareFKRules(boolean flag)
  {
    compareFKRules = flag;
  }

  public int getUpdateRuleValue()
  {
    return fkDefinition.getUpdateActionValue();
  }

  public int getDeleteRuleValue()
  {
    return fkDefinition.getDeleteActionValue();
  }

  public int getDeferrableRuleValue()
  {
    return fkDefinition.getDeferrableValue();
  }

  public String getUpdateRule()
  {
    return fkDefinition.getUpdateAction();
  }

  public String getDeleteRule()
  {
    return fkDefinition.getDeleteAction();
  }

  public String getDeferRule()
  {
    return fkDefinition.getDeferrableType();
  }

  public ReportTable getForeignTable()
  {
    return foreignTable;
  }

  public String getFkName()
  {
    return fkDefinition.getFkName();
  }

  @Override
  public String toString()
  {
    return this.getFkName();
  }

  public StringBuilder getXml(StringBuilder indent)
  {
    StringBuilder result = new StringBuilder(250);
    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");
    tagWriter.appendOpenTag(result, indent, TAG_FOREIGN_KEY);
    result.append('\n');

    result.append(getInnerXml(myindent));

    tagWriter.appendCloseTag(result, indent, TAG_FOREIGN_KEY);

    return result;
  }

  public StringBuilder getInnerXml(StringBuilder indent)
  {
    StringBuilder result = new StringBuilder(250);
    StringBuilder colIndent = new StringBuilder(indent);
    colIndent.append("  ");
    if (isEnabled())
    {
      tagWriter.appendTag(result, indent, TAG_CONSTRAINT_NAME, this.getFkName());
    }
    else
    {
      tagWriter.appendTag(result, indent, TAG_CONSTRAINT_NAME, this.getFkName(), "enabled", Boolean.toString(isEnabled()));
    }
    tagWriter.appendOpenTag(result, indent, "references");
    result.append('\n');
    this.foreignTable.appendTableNameXml(result, colIndent);
    tagWriter.appendCloseTag(result, indent, "references");

    tagWriter.appendOpenTag(result, indent, TAG_SOURCE_COLS);
    result.append('\n');

    Map<String, String> columnMap = fkDefinition.getColumns();
    for (String col : columnMap.keySet())
    {
      tagWriter.appendTag(result, colIndent, "column", columnMap.get(col));
    }
    tagWriter.appendCloseTag(result, indent, TAG_SOURCE_COLS);

    tagWriter.appendOpenTag(result, indent, TAG_TARGET_COLS);
    result.append('\n');
    for (String col : columnMap.keySet())
    {
      tagWriter.appendTag(result, colIndent, "column", col);
    }
    tagWriter.appendCloseTag(result, indent, TAG_TARGET_COLS);

    tagWriter.appendTag(result, indent, TAG_DELETE_RULE, this.getDeleteRule(), "jdbcValue", NumberStringCache.getNumberString(this.getDeleteRuleValue()));
    tagWriter.appendTag(result, indent, TAG_UPDATE_RULE, this.getUpdateRule(), "jdbcValue", NumberStringCache.getNumberString(this.getUpdateRuleValue()));
    tagWriter.appendTag(result, indent, TAG_DEFER_RULE, this.getDeferRule(), "jdbcValue", NumberStringCache.getNumberString(this.getDeferrableRuleValue()));

    return result;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 53 * hash + (this.getFkName() != null ? this.getFkName().hashCode() : 0);
    hash = 53 * hash + (this.foreignTable != null ? this.foreignTable.hashCode() : 0);
    if (compareFKRules)
    {
      hash = 53 * hash + this.getUpdateRuleValue();
      hash = 53 * hash + this.getDeleteRuleValue();
      hash = 53 * hash + this.getDeferrableRuleValue();
    }
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == null) return false;
    if (o instanceof ForeignKeyDefinition) return equals((ForeignKeyDefinition)o);
    return false;
  }

  private boolean compareColumns(ForeignKeyDefinition other)
  {
    if (other == null) return false;
    Map<String, String> myCols = fkDefinition.getColumns();
    Map<String, String> otherCols = other.fkDefinition.getColumns();
    if (myCols.size() != otherCols.size()) return false;

    Iterator<String> myItr = myCols.keySet().iterator();
    Iterator<String> otherItr = otherCols.keySet().iterator();

    while (myItr.hasNext())
    {
      String myCol = myItr.next();
      String otherCol = otherItr.next();  // this is safe because both iterators have the same number of elements
      if (myCol.equalsIgnoreCase(otherCol))
      {
        String myFk = myCols.get(myCol);
        String otherFk = otherCols.get(otherCol);
        if (!myFk.equalsIgnoreCase(otherFk)) return false;
      }
      else
      {
        return false;
      }
    }
    return true;
  }

  public boolean isNameEqual(ForeignKeyDefinition ref)
  {
    try
    {
      return this.getFkName().equalsIgnoreCase(ref.getFkName());
    }
    catch (Exception e)
    {
      return false;
    }
  }

  public boolean isDefinitionEqual(ForeignKeyDefinition ref)
  {
    try
    {
      boolean baseEqual = compareColumns(ref);
      baseEqual = baseEqual && this.foreignTable.equals(ref.foreignTable);

      if (baseEqual && compareFKRules)
      {
        baseEqual = baseEqual &&
              (this.getUpdateRuleValue() == ref.getUpdateRuleValue()) &&
              (this.getDeleteRuleValue() == ref.getDeleteRuleValue()) &&
              (this.getDeferrableRuleValue() == ref.getDeferrableRuleValue());
      }
      return baseEqual;
    }
    catch (Exception e)
    {
      return false;
    }
  }

  public boolean equals(ForeignKeyDefinition ref)
  {
    try
    {
      return isDefinitionEqual(ref) && isNameEqual(ref);
    }
    catch (Exception e)
    {
      return false;
    }
  }
}
