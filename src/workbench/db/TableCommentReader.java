/*
 * TableCommentReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.Savepoint;
import java.util.List;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
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

		String commentStatement = mgr.getCommentSqlTemplate(table.getType());

		if (StringUtil.isBlank(commentStatement))
		{
			return null;
		}

		String comment = null;

		if (!table.commentIsDefined())
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
				result = StringUtil.replace(commentStatement, CommentSqlManager.COMMENT_FQ_OBJECT_NAME_PLACEHOLDER, table.getTableExpression(dbConnection));
			}
			else
			{
				result = StringUtil.replace(commentStatement, CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, table.getTableName());
				result = replaceObjectNamePlaceholder(result, CommentSqlManager.COMMENT_SCHEMA_PLACEHOLDER, table.getSchema());
				result = replaceObjectNamePlaceholder(result, CommentSqlManager.COMMENT_CATALOG_PLACEHOLDER, table.getCatalog());
			}
			result = StringUtil.replace(result, CommentSqlManager.COMMENT_PLACEHOLDER, comment == null ? "" : comment.replace("'", "''"));
			result += ";";
		}

		return result;
	}

	public String getTableComment(WbConnection dbConnection, TableIdentifier tbl)
	{
		TableIdentifier table = tbl.createCopy();
		table.adjustCase(dbConnection);
		ResultSet rs = null;
		String result = null;
		Savepoint sp = null;
		try
		{
			if (dbConnection.getDbSettings().useSavePointForDML())
			{
				sp = dbConnection.setSavepoint();
			}
			rs = dbConnection.getSqlConnection().getMetaData().getTables(table.getRawCatalog(), table.getRawSchema(), table.getRawTableName(), null);
			if (rs.next())
			{
				result = rs.getString("REMARKS");
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("TableCommentReader.getTableComment()", "Error retrieving comment for table " + table.getTableExpression(), e);
			result = null;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return result;
	}

	/**
	 * Return the SQL that is needed to re-create the comment on the given columns.
	 * The syntax to be used, can be configured in the workbench.settings file.
	 * @see CommentSqlManager#getCommentSqlTemplate(java.lang.String) 
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

		String columnStatement = mgr.getCommentSqlTemplate("column");
		if (StringUtil.isBlank(columnStatement)) return null;
		StringBuilder result = new StringBuilder(columns.size() * 25);
		for (ColumnIdentifier col : columns)
		{
			String column = col.getColumnName();
			String comment = col.getComment();
			if (Settings.getInstance().getIncludeEmptyComments() || StringUtil.isNonBlank(comment))
			{
				try
				{
					String commentSql = null;
					if (columnStatement.contains(CommentSqlManager.COMMENT_FQ_OBJECT_NAME_PLACEHOLDER))
					{
						commentSql = StringUtil.replace(columnStatement, CommentSqlManager.COMMENT_FQ_OBJECT_NAME_PLACEHOLDER, table.getTableExpression(con));
					}
					else
					{
						commentSql = StringUtil.replace(columnStatement, CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, table.getTableName());
						commentSql = replaceObjectNamePlaceholder(commentSql, CommentSqlManager.COMMENT_SCHEMA_PLACEHOLDER, table.getSchema());
						commentSql = replaceObjectNamePlaceholder(commentSql, CommentSqlManager.COMMENT_CATALOG_PLACEHOLDER, table.getCatalog());
					}
					commentSql = StringUtil.replace(commentSql, CommentSqlManager.COMMENT_COLUMN_PLACEHOLDER, column);
					commentSql = StringUtil.replace(commentSql, CommentSqlManager.COMMENT_PLACEHOLDER, comment == null ? "" : SqlUtil.escapeQuotes(comment));
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
