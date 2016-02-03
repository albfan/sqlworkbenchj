/*
 * TableCommentReader.java
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

import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.sqltemplates.ColumnChanger;

import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class TableCommentReader
{
	public TableCommentReader()
	{
	}

	/**
	 * Return the SQL that is needed to re-create the comment on the given table.
	 * The syntax to be used, can be configured in the workbench.settings file.
	 */
	public String getTableCommentSql(WbConnection dbConnection, TableIdentifier table)
	{
		return getTableCommentSql(dbConnection.getDbSettings().getDbId(), dbConnection, table);
	}

	String getTableCommentSql(String dbId, WbConnection dbConnection, TableIdentifier table)
	{
		CommentSqlManager mgr = new CommentSqlManager(dbConnection.getMetadata().getDbId());

		String commentStatement = mgr.getCommentSqlTemplate(table.getType(), CommentSqlManager.COMMENT_ACTION_SET);

		if (StringUtil.isBlank(commentStatement))
		{
			return null;
		}

		String comment = null;

		if (table.commentIsDefined())
		{
			comment = table.getComment();
		}
		else
		{
			comment = getTableComment(dbConnection, table);
		}

		String result = null;
		if (Settings.getInstance().getIncludeEmptyComments() || StringUtil.isNonBlank(comment))
		{
			if (commentStatement.contains(CommentSqlManager.COMMENT_FQ_OBJECT_NAME_PLACEHOLDER))
			{
				result = StringUtil.replace(commentStatement, CommentSqlManager.COMMENT_FQ_OBJECT_NAME_PLACEHOLDER, table.getFullyQualifiedName(dbConnection));
			}
			else
			{
        result = StringUtil.replace(commentStatement, CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, table.getObjectExpression(dbConnection));
				result = replaceObjectNamePlaceholder(result, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, dbConnection.getMetadata().quoteObjectname(table.getTableName()));
        result = replaceObjectNamePlaceholder(result, MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, table.getFullyQualifiedName(dbConnection));
				result = replaceObjectNamePlaceholder(result, TableSourceBuilder.SCHEMA_PLACEHOLDER, dbConnection.getMetadata().quoteObjectname(table.getSchema()));
				result = replaceObjectNamePlaceholder(result, TableSourceBuilder.CATALOG_PLACEHOLDER, dbConnection.getMetadata().quoteObjectname(table.getCatalog()));
			}
			result = StringUtil.replace(result, CommentSqlManager.COMMENT_PLACEHOLDER, comment == null ? "" : comment.replace("'", "''"));
			result += ";";
		}

		return result;
	}

	public String getTableComment(WbConnection dbConnection, TableIdentifier tbl)
	{
		TableIdentifier id = dbConnection.getMetadata().findObject(tbl);
		if (id == null) return null;
		return id.getComment();
	}

	/**
	 * Return the SQL that is needed to re-create the comment on the given columns.
	 *
	 * The syntax to be used, can be configured in the workbench.settings file.
	 *
	 * @see CommentSqlManager#getCommentSqlTemplate(java.lang.String, java.lang.String)
	 */
	public StringBuilder getTableColumnCommentsSql(WbConnection con, TableIdentifier table, List<ColumnIdentifier> columns)
	{
		return getTableColumnCommentsSql(con.getMetadata().getDbId(), con, table, columns);
	}

	/**
	 * For Unit-Testing only
	 */
	StringBuilder getTableColumnCommentsSql(String dbId, WbConnection con, TableIdentifier table, List<ColumnIdentifier> columns)
	{
		CommentSqlManager mgr = new CommentSqlManager(dbId);

		String columnStatement = mgr.getCommentSqlTemplate("column", CommentSqlManager.COMMENT_ACTION_SET);
		if (StringUtil.isBlank(columnStatement)) return null;
		StringBuilder result = new StringBuilder(columns.size() * 25);
		ColumnChanger colChanger = new ColumnChanger(con);

		for (ColumnIdentifier col : columns)
		{
			String comment = col.getComment();
			if (Settings.getInstance().getIncludeEmptyComments() || StringUtil.isNonBlank(comment))
			{
				try
				{
					String commentSql = colChanger.getColumnCommentSql(table, col);
					result.append(commentSql);
					result.append(";\n");
				}
				catch (Exception e)
				{
					LogMgr.logError("TableCommentReader.getTableColumnCommentsSql()", "Error creating comments SQL for remark=" + comment, e);
				}
			}
		}
		return result;
	}

	private String replaceObjectNamePlaceholder(String source, String placeHolder, String replacement)
	{
		if (StringUtil.isBlank(replacement))
		{
			return source.replace(placeHolder + ".", "");
		}
		return source.replace(placeHolder, replacement);
	}

}
