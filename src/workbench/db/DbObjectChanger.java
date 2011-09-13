/*
 * DbObjectChanger.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectChanger
{
	public static final String PARAM_OLD_OBJECT_NAME = "%object_name%";
	public static final String PARAM_NEW_OBJECT_NAME = "%new_object_name%";

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
			String sql = getRename(entry.getKey(), entry.getValue());
			if (sql != null)
			{
				result.append(sql);
				result.append(";\n");
			}
		}
		if (settings.ddlNeedsCommit())
		{
			result.append("\nCOMMIT;\n");
		}
		return result.toString();
	}

	public String getRename(DbObject oldTable, DbObject newTable)
	{
		if (newTable == null || oldTable == null) return null;

		String type = oldTable.getObjectType();
		String sql = getRenameObjectSql(type);
		if (sql == null) return null;

		String oldName = oldTable.getObjectName();
		String newName = newTable.getObjectName();

		if (StringUtil.equalStringOrEmpty(oldName.trim(), newName.trim(), true)) return null; // no change
		sql = sql.replace(PARAM_OLD_OBJECT_NAME, oldName.trim());
		sql = sql.replace(PARAM_NEW_OBJECT_NAME, newName.trim());
		return sql;
	}

	public String getCommentSql(DbObject oldDefinition, DbObject newDefinition)
	{
		if (commentMgr == null || newDefinition == null || oldDefinition == null) return null;

		String type = oldDefinition.getObjectType();

		String sql = getCommentSql(type);
		if (sql == null) return null;

		String oldComment = oldDefinition.getComment();
		String newComment = newDefinition.getComment();

		String schema = oldDefinition.getSchema();
		if (schema == null) schema = "";

		if (StringUtil.equalStringOrEmpty(oldComment, newComment, true)) return null; // no change
		String oldname = oldDefinition.getObjectName(dbConnection);
		if (oldname == null) oldname = "";

		// object_name placeholder is expected to be used where a fully qualified name is needed
		sql = sql.replace(CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, oldname);

		// schema and table name placeholders are intended where those names are individual parameters
		// this is mainly used for the kludgy and non-standard way SQL Server "supports" comments
		sql = sql.replace(ColumnChanger.PARAM_TABLE_NAME, oldDefinition.getObjectName());
		sql = sql.replace(CommentSqlManager.COMMENT_SCHEMA_PLACEHOLDER, schema);

		sql = sql.replace(CommentSqlManager.COMMENT_PLACEHOLDER, newComment.replace("'", "''"));
		return sql;
	}

	public String getRenameObjectSql(String type)
	{
		if (settings == null || type == null) return null;
		return settings.getRenameObjectSql(type);
	}

	public String getCommentSql(String type)
	{
		if (commentMgr == null || type == null) return null;
		return commentMgr.getCommentSqlTemplate(type);
	}

	public String getDropPKScript(TableIdentifier table)
	{
		String sql = getDropPK(table);
		if (StringUtil.isBlank(sql)) return null;
		StringBuilder script = new StringBuilder(sql);
		script.append(";\n");
		if (settings.ddlNeedsCommit())
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

		sql = sql.replace(MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableExpression(dbConnection));
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
		if (settings.ddlNeedsCommit())
		{
			script.append("\nCOMMIT;\n");
		}
		return script.toString();
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

		sql = sql.replace(MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableExpression(dbConnection));
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

}
