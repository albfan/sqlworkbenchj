/*
 * CommentSqlManager.java
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

import workbench.resource.Settings;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class CommentSqlManager
{
  /**
   * The placeholder for the fully qualified object (table, view, ...) name
   */
  public static final String COMMENT_FQ_OBJECT_NAME_PLACEHOLDER = "%fq_object_name%";
  public static final String COMMENT_OBJECT_NAME_PLACEHOLDER = "%object_name%";
  public static final String COMMENT_COLUMN_PLACEHOLDER = MetaDataSqlManager.COLUMN_NAME_PLACEHOLDER;
  public static final String COMMENT_PLACEHOLDER = "%comment%";

  public static final String COMMENT_ACTION_SET = "set";
  public static final String COMMENT_ACTION_DELETE = "delete";
  public static final String COMMENT_ACTION_UPDATE = "update";

  private String dbid;

  public CommentSqlManager(String id)
  {
    this.dbid = id;
  }

  public String getCommentSqlTemplate(String objectType, String action)
  {
    if (StringUtil.isBlank(objectType)) return null;

    objectType = objectType.toLowerCase().replace(" ", "_");

    String key = "workbench.db." + dbid + ".sql.comment." + objectType;
    String sql = null;

    if (action != null)
    {
      sql = Settings.getInstance().getProperty(key + "." + action, null);
    }

    String defaultValue = Settings.getInstance().getProperty("workbench.db.sql.comment." + objectType, null);
    if (sql == null)
    {
      // no action specific statement found, use a "plain" one
      sql = Settings.getInstance().getProperty(key, defaultValue);
    }

    if (StringUtil.isEmptyString(sql))
    {
      // If the DB specific property is present, but empty, this means
      // the database does not support this type of comments.
      // if I did not test for presence of the key, the default would
      // always be returned, and thus it would not be possible to "delete"
      // the default by overwriting it with an empty key
      if (Settings.getInstance().isPropertyDefined(key))
      {
        return null;
      }
      else
      {
        sql = defaultValue;
      }
    }
    sql = Settings.getInstance().replaceProperties(sql);
    return SqlUtil.trimSemicolon(sql);
  }

  public static String getAction(String oldComment, String newComment)
  {
    if (StringUtil.equalStringOrEmpty(oldComment, newComment, true)) return null;

    if (StringUtil.isEmptyString(oldComment) && StringUtil.isNonEmpty(newComment))
    {
      return COMMENT_ACTION_SET;
    }

    if (StringUtil.isEmptyString(newComment) && StringUtil.isNonEmpty(oldComment))
    {
      return COMMENT_ACTION_DELETE;
    }

    return COMMENT_ACTION_UPDATE;
  }

}
