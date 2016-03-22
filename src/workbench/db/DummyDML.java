/*
 * DummyDML.java
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
package workbench.db;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.resource.Settings;

import workbench.storage.ResultInfo;

import workbench.sql.formatter.FormatterUtil;
import workbench.sql.formatter.WbSqlFormatter;

import workbench.util.SqlUtil;

/**
 * @author Thomas Kellerer
 */
public class DummyDML
  implements DbObject
{
  public static final String PROP_CONFIG_PREFIX = "workbench.sql.generate.dummydml.";
  public static final String PROP_CONFIG_MAKE_PREPARED = PROP_CONFIG_PREFIX + "prepared";
  public static final String PROP_CONFIG_GENERATE_LITERAL = PROP_CONFIG_PREFIX + "literal";
  public static final String PLACEHOLDER_COL_NAME = "${column_name}";
  public static final String PLACEHOLDER_TABLE_NAME = "${table_name}";

  protected boolean doFormat;
  protected final TableIdentifier table;
  private List<ColumnIdentifier> columns;

  protected DummyDML(TableIdentifier tbl)
  {
    this.table = tbl;
  }

  public DummyDML(TableIdentifier tbl, List<ColumnIdentifier> cols)
  {
    this.table = tbl;
    this.columns = new ArrayList<>(cols);
  }

  public void setDoFormatSql(boolean flag)
  {
    doFormat = flag;
  }

  @Override
  public String getComment()
  {
    return null;
  }

  @Override
  public void setComment(String c)
  {
  }

  @Override
  public String getCatalog()
  {
    return null;
  }

  @Override
  public String getFullyQualifiedName(WbConnection conn)
  {
    return getObjectExpression(conn);
  }

  @Override
  public String getObjectExpression(WbConnection conn)
  {
    return null;
  }

  @Override
  public String getObjectName()
  {
    if (table == null) return getObjectType(); // make sure something is returned to avoid "null" labels in the UI
    return table.getTableName();
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return null;
  }

  @Override
  public String getDropStatement(WbConnection con, boolean cascade)
  {
    return null;
  }

  @Override
  public String getObjectNameForDrop(WbConnection con)
  {
    return null;
  }

  @Override
  public String getObjectType()
  {
    throw new UnsupportedOperationException("Must be implemented in a descendant");
  }

  @Override
  public String getSchema()
  {
    return null;
  }

  @Override
  public CharSequence getSource(WbConnection con)
    throws SQLException
  {
    throw new UnsupportedOperationException("Must be implemented in a descendant");
  }

  protected List<ColumnIdentifier> getColumns(WbConnection con)
    throws SQLException
  {
    if (columns != null) return columns;

    ResultInfo info = new ResultInfo(table, con);
    return info.getColumnList();
  }

  public String getTemplateConfigKey()
  {
    return PROP_CONFIG_PREFIX + getObjectType().toLowerCase() + ".value.template";
  }

  public String getTemplateConfigKeyForType(int jdbcType)
  {
    return getTemplateConfigKeyForType(SqlUtil.getTypeName(jdbcType).toLowerCase());
  }

  public String getTemplateConfigKeyForType(String type)
  {
    return PROP_CONFIG_PREFIX + getObjectType().toLowerCase() + ".value.template." + type;
  }

  protected String getValueString(ColumnIdentifier col)
  {
    boolean makePrepared = Settings.getInstance().getBoolProperty(PROP_CONFIG_MAKE_PREPARED, false);
    if (makePrepared)
    {
      return "?";
    }

    String baseTemplate = Settings.getInstance().getProperty(getTemplateConfigKey(), PLACEHOLDER_COL_NAME + "_value");

    String template = Settings.getInstance().getProperty(getTemplateConfigKeyForType(col.getDataType()), baseTemplate);
    String name = FormatterUtil.getIdentifier(SqlUtil.removeObjectQuotes(col.getColumnName()));
    boolean makeLiteralDefault = Settings.getInstance().getBoolProperty(PROP_CONFIG_GENERATE_LITERAL, true);

    String typeKey = PROP_CONFIG_GENERATE_LITERAL + "." + SqlUtil.getTypeName(col.getDataType()).toLowerCase();
    boolean makeLiteral = Settings.getInstance().getBoolProperty(typeKey, makeLiteralDefault);

    String value = template.replace(PLACEHOLDER_COL_NAME, name);
    if (table != null)
    {
      value = value.replace(PLACEHOLDER_TABLE_NAME, FormatterUtil.getIdentifier(table.getRawTableName()));
    }

    if (makeLiteral)
    {
      int type = col.getDataType();
      if (SqlUtil.isCharacterType(type))
      {
        value = "'" + value + "'";
      }
    }
    return value;
  }

  protected String getColumnName(ColumnIdentifier col, WbConnection dbConnection)
  {
    String name = dbConnection.getMetadata().quoteObjectname(col.getColumnName());
    return FormatterUtil.getIdentifier(name);
  }

  protected String formatSql(String sql, WbConnection con)
  {
    if (doFormat)
    {
      WbSqlFormatter f = new WbSqlFormatter(sql, con == null ? null : con.getDbId());
      if (con != null)
      {
        f.setCatalogSeparator(con.getMetadata().getCatalogSeparator());
      }
      return f.getFormattedSql();
    }
    return sql;
  }

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
