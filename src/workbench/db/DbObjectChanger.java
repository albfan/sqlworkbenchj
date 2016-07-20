/*
 * DbObjectChanger.java
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
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;

import workbench.db.mssql.SqlServerTableSourceBuilder;
import workbench.db.oracle.OracleTableSourceBuilder;
import workbench.db.sqltemplates.TemplateHandler;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectChanger
{
  public static final String PARAM_OLD_OBJECT_NAME = "%object_name%";
  public static final String PARAM_OLD_FQ_OBJECT_NAME = "%fq_object_name%";
  public static final String PARAM_NEW_FQ_OBJECT_NAME = "%new_fq_object_name%";
  public static final String PARAM_NEW_OBJECT_NAME = "%new_object_name%";

  public static final String PARAM_OLD_SCHEMA_NAME = "%schema_name%";
  public static final String PARAM_NEW_SCHEMA_NAME = "%new_schema_name%";

  public static final String PARAM_OLD_CATALOG_NAME = "%catalog_name%";
  public static final String PARAM_NEW_CATALOG_NAME = "%new_catalog_name%";

  private WbConnection dbConnection;
  private DbSettings settings;
  private CommentSqlManager commentMgr;

  public DbObjectChanger(WbConnection con)
  {
    if (con != null)
    {
      settings = con.getDbSettings();
      commentMgr = new CommentSqlManager(settings.getDbId());
    }
    dbConnection = con;
  }

  /**
   * For testing purposes
   *
   * @param dbSettings
   */
  DbObjectChanger(DbSettings dbSettings)
  {
    settings = dbSettings;
    commentMgr = new CommentSqlManager(settings.getDbId());
    dbConnection = null;
  }

  /**
   * Generate a complete SQL script to apply a rename or changed comment on the supplied objects.
   *
   * @param changedObjects old/new definitions of the objects
   * @return the complete srcipt to apply the changes
   */
  public String getAlterScript(Map<DbObject, DbObject> changedObjects)
  {
    StringBuilder result = new StringBuilder(changedObjects.size() * 50);
    for (Map.Entry<DbObject, DbObject> entry : changedObjects.entrySet())
    {
      String commentSql = getCommentSql(entry.getKey(), entry.getValue());
      if (commentSql != null)
      {
        result.append(commentSql);
        result.append(";\n");
      }
      String schema = getSchemaChange(entry.getKey(), entry.getValue());
      if (schema != null)
      {
        result.append(schema);
        result.append(";\n");
      }
      String cat = getCatalogChange(entry.getKey(), entry.getValue());
      if (cat != null)
      {
        result.append(cat);
        result.append(";\n");
      }
      String sql = getRename(entry.getKey(), entry.getValue());
      if (sql != null)
      {
        result.append(sql);
        result.append(";\n");
      }
    }
    if (needsCommit() && result.length() > 0)
    {
      result.append("\nCOMMIT;\n");
    }
    return result.toString();
  }

  public String getSchemaChange(DbObject oldTable, DbObject newTable)
  {
    if (newTable == null || oldTable == null) return null;
    String type = oldTable.getObjectType();
    String sql = getChangeSchemaSql(type);
    if (sql == null) return null;

    String oldSchema = oldTable.getSchema();
    String newSchema = newTable.getSchema();
    if (oldSchema == null || newSchema == null) return null;

    if (StringUtil.equalStringOrEmpty(oldSchema.trim(), newSchema.trim(), true)) return null; // no change

    sql = sql.replace(PARAM_OLD_SCHEMA_NAME, oldSchema.trim());
    sql = sql.replace(PARAM_NEW_SCHEMA_NAME, newSchema.trim());
    sql = TemplateHandler.replaceTablePlaceholder(sql, oldTable, dbConnection);
    sql = sql.replace(PARAM_OLD_OBJECT_NAME, oldTable.getObjectName(dbConnection));

    return sql;
  }

  public String getCatalogChange(DbObject oldTable, DbObject newTable)
  {
    if (newTable == null || oldTable == null) return null;
    String type = oldTable.getObjectType();
    String sql = getChangeCatalogSql(type);
    if (sql == null) return null;

    String oldCat = oldTable.getCatalog();
    String newCat = newTable.getCatalog();
    if (oldCat == null || newCat == null) return null;

    if (StringUtil.equalStringOrEmpty(oldCat.trim(), newCat.trim(), true)) return null; // no change

    sql = sql.replace(PARAM_OLD_CATALOG_NAME, oldCat.trim());
    sql = sql.replace(PARAM_NEW_CATALOG_NAME, newCat.trim());
    sql = TemplateHandler.replaceTablePlaceholder(sql, oldTable, dbConnection);

    return sql;
  }

  public String getRename(DbObject oldObject, DbObject newObject)
  {
    if (newObject == null || oldObject == null) return null;

    String type = oldObject.getObjectType();
    String sql = getRenameObjectSql(type);
    if (sql == null) return null;

    // don't check the fully qualified name. Any schema or catalog change will be handled separately
    String oldName = oldObject.getObjectName(dbConnection);
    String newName = newObject.getObjectName(dbConnection);

    if (StringUtil.equalStringOrEmpty(oldName.trim(), newName.trim(), true)) return null; // no change
    String fqOld = SqlUtil.fullyQualifiedName(dbConnection, oldObject);
    String fqNew = SqlUtil.fullyQualifiedName(dbConnection, newObject);

    sql = sql.replace(PARAM_OLD_OBJECT_NAME, oldObject.getObjectExpression(dbConnection));
    sql = sql.replace(PARAM_OLD_FQ_OBJECT_NAME, fqOld);
    sql = sql.replace(PARAM_NEW_OBJECT_NAME, newObject.getObjectExpression(dbConnection));
    sql = sql.replace(PARAM_NEW_FQ_OBJECT_NAME, fqNew);

    sql = sql.replace(MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, fqOld);
    return sql;
  }

  public String getCommentSql(DbObject oldDefinition, DbObject newDefinition)
  {
    if (commentMgr == null || newDefinition == null || oldDefinition == null) return null;

    String type = oldDefinition.getObjectType();

    String oldComment = oldDefinition.getComment();
    String newComment = newDefinition.getComment();

    String schema = oldDefinition.getSchema();
    if (schema == null) schema = "";

    if (StringUtil.equalStringOrEmpty(oldComment, newComment, false)) return null; // no change

    String action = CommentSqlManager.getAction(oldComment, newComment);

    String sql = getCommentSql(type, action);
    if (sql == null) return null;

    if (oldComment == null) oldComment = "";
    if (newComment == null) newComment = "";

    String oldname = oldDefinition.getObjectName(dbConnection);
    if (oldname == null) oldname = "";

    // object_name placeholder is expected to be used where a fully qualified name is needed
    sql = sql.replace(CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, oldname);

    String fqn = oldDefinition.getObjectNameForDrop(this.dbConnection);
    sql = sql.replace(CommentSqlManager.COMMENT_FQ_OBJECT_NAME_PLACEHOLDER, fqn);

    // schema and table name placeholders are intended where those names are individual parameters
    // this is mainly used for the kludgy and non-standard way SQL Server "supports" comments
    sql = sql.replace(MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, oldDefinition.getObjectName());
    sql = sql.replace(TableSourceBuilder.SCHEMA_PLACEHOLDER, schema);

    sql = sql.replace(CommentSqlManager.COMMENT_PLACEHOLDER, newComment.replace("'", "''"));
    return sql;
  }

  public String getRenameObjectSql(String type)
  {
    if (settings == null || type == null) return null;
    return settings.getRenameObjectSql(type);
  }

  public String getChangeSchemaSql(String type)
  {
    if (settings == null || type == null) return null;
    return settings.getChangeSchemaSql(type);
  }

  public String getChangeCatalogSql(String type)
  {
    if (settings == null || type == null) return null;
    return settings.getChangeCatalogSql(type);
  }

  public String getCommentSql(String type, String action)
  {
    if (commentMgr == null || type == null) return null;
    return commentMgr.getCommentSqlTemplate(type, action);
  }

  public String getDropPKScript(TableIdentifier table)
  {
    String sql = getDropPK(table);
    if (StringUtil.isBlank(sql)) return null;
    StringBuilder script = new StringBuilder(sql);
    script.append(";\n");

    if (needsCommit())
    {
      script.append("\nCOMMIT;\n");
    }
    return script.toString();
  }

  public String getDropPK(TableIdentifier table)
  {
    String type = table.getObjectType();
    if (StringUtil.isBlank(type)) return null;
    String sql = settings.getDropPrimaryKeySql(type);

    String pkConstraint = table.getPrimaryKeyName();
    if (StringUtil.isBlank(sql))
    {
      // The database doesn't support "DROP PRIMARY KEY", so we need to
      // drop the corresponding constraint
      if (StringUtil.isBlank(pkConstraint) && dbConnection != null)
      {
        try
        {
          TableDefinition def = dbConnection.getMetadata().getTableDefinition(table);
          pkConstraint = def.getTable().getPrimaryKeyName();
        }
        catch (SQLException e)
        {
          LogMgr.logError("DbObjectChanger.generateDropPK()", "Error retrieving table definition", e);
          return null;
        }
      }
      sql = settings.getDropConstraint(type);
    }

    if (sql == null) return null;

    sql = TemplateHandler.replaceTablePlaceholder(sql, table, dbConnection, false);
    if (pkConstraint != null)
    {
      sql = sql.replace(MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, pkConstraint);
    }
    return sql;
  }

  /**
   * Returns the SQL script to add a primary key to the table.
   *
   * If the DBMS supports transactional DDL, the script will contain
   * a COMMIT statement. Otherwise it will be identical to the result of getAddPK()
   *
   * @param table the table for which to create the PK
   * @param pkCols the (new) PK columns
   * @return null if adding a PK is not possible, the necessary statement otherwise
   * @see #getAddPK(workbench.db.TableIdentifier, java.util.List)
   */
  public String getAddPKScript(TableIdentifier table, List<ColumnIdentifier> pkCols)
  {
    String sql = getAddPK(table, pkCols);
    if (StringUtil.isBlank(sql)) return null;
    StringBuilder script = new StringBuilder(sql);
    script.append(";\n");
    if (needsCommit())
    {
      script.append("\nCOMMIT;\n");
    }
    return script.toString();
  }

  private boolean needsCommit()
  {
    if (settings.ddlNeedsCommit())
    {
      return dbConnection == null ? true : dbConnection.getAutoCommit() == false;
    }
    return false;
  }

  /**
   * Returns the SQL Statement to add a primary key to the table.
   *
   * The primary key will be created using the columns provide.
   *
   * @param table the table for which to create the PK
   * @param pkCols the (new) PK columns (the isPK() attribute for the columns is not checked or modified)
   * @return null if adding a PK is not possible, the necessary statement otherwise
   * @see #getAddPKScript(workbench.db.TableIdentifier, java.util.List)
   */
  public String getAddPK(TableIdentifier table, List<ColumnIdentifier> pkCols)
  {
    String type = table.getObjectType();
    if (StringUtil.isBlank(type)) return null;
    String sql = settings.getAddPK(type);
    if (StringUtil.isBlank(sql)) return null;

    String pkName = "PK_" + table.getTableName().toUpperCase();
    if (dbConnection != null && dbConnection.getMetadata().storesLowerCaseIdentifiers())
    {
      pkName = pkName.toLowerCase();
    }

    sql = TemplateHandler.replaceTablePlaceholder(sql, table, dbConnection, false);
    sql = TemplateHandler.removePlaceholder(sql, SqlServerTableSourceBuilder.CLUSTERED_PLACEHOLDER, true);
    sql = TemplateHandler.removePlaceholder(sql, OracleTableSourceBuilder.INDEX_USAGE_PLACEHOLDER, true);

    if (pkName != null)
    {
      sql = sql.replace(MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, pkName);
    }

    StringBuilder cols = new StringBuilder(pkCols.size() * 5);
    for (int i=0; i < pkCols.size(); i++)
    {
      if (i > 0) cols.append(", ");
      cols.append(pkCols.get(i).getColumnName(dbConnection));
    }
    sql = sql.replace(MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, cols);
    return sql;
  }

  public String getDropFK(TableIdentifier table, String fkName)
  {
    String type = table.getObjectType();
    if (StringUtil.isBlank(type)) return null;
    if (StringUtil.isBlank(fkName)) return null;

    String sql = settings.getDropConstraint(type);

    if (sql == null) return null;

    sql = TemplateHandler.replaceTablePlaceholder(sql, table, dbConnection, false);
    sql = sql.replace(MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, fkName);
    return sql;
  }

  public String getDropFKScript(Map<TableIdentifier, String> constraints)
  {
    if (CollectionUtil.isEmpty(constraints)) return null;

    StringBuilder script = new StringBuilder(50);
    for (Map.Entry<TableIdentifier, String> entry : constraints.entrySet())
    {
      String sql = getDropFK(entry.getKey(), entry.getValue());
      if (sql != null)
      {
        script.append(sql);
        script.append(";\n");
      }
    }
    if (needsCommit())
    {
      script.append("\nCOMMIT;\n");
    }
    return script.toString();
  }
}
