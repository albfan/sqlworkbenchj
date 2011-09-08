/*
 * ColumnChanger.java
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

import java.util.List;
import workbench.db.oracle.OracleMetadata;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to generate ALTER statements for changes to column definitions
 * of a table. The necessary DBMS specific SQL statements are retrieved
 * through DbSettings
 *
 * @author Thomas Kellerer
 */
public class ColumnChanger
{
	public static final String PARAM_TABLE_NAME = MetaDataSqlManager.TABLE_NAME_PLACEHOLDER;
	public static final String PARAM_COL_NAME = MetaDataSqlManager.COLUMN_NAME_PLACEHOLDER;

	public static final String PARAM_NEW_COL_NAME = "%new_column_name%";
	public static final String PARAM_DATATYPE = "%datatype%";
	public static final String PARAM_NEW_DATATYPE = "%new_datatype%";

	/**
	 * The placeholder for the complete DEFAULT xxx expression when adding a new column
	 */
	public static final String PARAM_DEFAULT_EXPR = "%default_expression%";

	public static final String PARAM_NULLABLE = "%nullable%";

	/**
	 * The placeholder for the default <b>value</b> for generating ALTER column
	 * statements (the DEFAULT keyword is already part of the template string)
	 */
	public static final String PARAM_DEFAULT_VALUE = "%default_value%";


	// I'm storing connection and DbSettings in two different
	// variables so that I can initialize a ColumnChanger in the
	// Unit test without a connection.
	private WbConnection dbConn;
	private DbSettings dbSettings;
	private CommentSqlManager commentMgr;

	public ColumnChanger(WbConnection con)
	{
		dbConn = con;
		dbSettings = (con != null ? con.getDbSettings() : null);
		commentMgr = new CommentSqlManager(dbSettings != null ? dbSettings.getDbId() : "");
	}

	/**
	 * For unit testing
	 * @param settings the DB configuration to be used
	 */
	ColumnChanger(DbSettings settings)
	{
		dbConn = null;
		dbSettings = settings;
		commentMgr = new CommentSqlManager(dbSettings != null ? dbSettings.getDbId() : "");
	}

	public String getAlterScript(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		List<String> statements = getAlterStatements(table, oldDefinition, newDefinition);
		if (statements.isEmpty()) return null;

		StringBuilder result = new  StringBuilder(statements.size() * 50);

		if (dbConn != null && dbConn.getMetadata().isOracle() && oldDefinition != null)
		{
			String oldComment = oldDefinition.getComment();
			String newComment = newDefinition.getComment();
			if (!StringUtil.equalStringOrEmpty(oldComment, newComment) && !OracleMetadata.remarksEnabled(dbConn))
			{
				result.append("-- ");
				result.append(ResourceMgr.getString("MsgSchemaReporterOracleRemarksWarning"));
				result.append('\n');
			}
		}

		for (String sql : statements)
		{
			result.append(sql);
			result.append(";\n");
		}
		return result.toString();
	}

	public List<String> getAlterStatements(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		List<String> result = CollectionUtil.arrayList();
		if (oldDefinition == null && canAddColumn())
		{
			String sql = addColumn(table, newDefinition);
			if (sql != null) result.add(sql);

			if (StringUtil.isNonBlank(newDefinition.getComment()))
			{
				String comment = changeRemarks(table, null, newDefinition);
				if (comment != null) result.add(comment);
			}
		}
		else if (oldDefinition != null)
		{
			String sql = changeDataType(table, oldDefinition, newDefinition);
			if (sql != null) result.add(SqlUtil.trimSemicolon(sql));

			sql = changeDefault(table, oldDefinition, newDefinition);
			if (sql != null) result.add(SqlUtil.trimSemicolon(sql));

			sql = changeNullable(table, oldDefinition, newDefinition);
			if (sql != null) result.add(SqlUtil.trimSemicolon(sql));

			sql = changeRemarks(table, oldDefinition, newDefinition);
			if (sql != null) result.add(SqlUtil.trimSemicolon(sql));

			sql = renameColumn(table, oldDefinition, newDefinition);
			if (sql != null) result.add(SqlUtil.trimSemicolon(sql));
		}
		return result;
	}

	protected String changeCommonPlaceholders(String sql, ColumnIdentifier newCol)
	{
		// Some stubid DBMS require the full definition of the column (including nullable, default and so on)
		// even if only the type should be changed or if the column is only renamed
		sql = sql.replace(PARAM_NULLABLE, nullableSql(newCol.isNullable()));
		String comment = newCol.getComment();
		if (comment == null) comment = "";
		sql = sql.replace(CommentSqlManager.COMMENT_PLACEHOLDER, comment.replace("'", "''"));

		String defaultValue = newCol.getDefaultValue();
		if (StringUtil.isBlank(defaultValue))
		{
			sql = sql.replace("DEFAULT " + PARAM_DEFAULT_VALUE, "");
			sql = sql.replace(PARAM_DEFAULT_VALUE, "");
		}
		else
		{
			sql = sql.replace(PARAM_DEFAULT_VALUE, defaultValue);
		}

		String dataType = newCol.getDbmsType();
		sql = sql.replace(PARAM_NEW_DATATYPE, dataType);

		return sql;
	}

	public boolean canAlterType()
	{
		String sql = dbSettings.getAlterColumnDataTypeSql();
		return (sql != null);
	}

	public boolean canRenameColumn()
	{
		String sql = dbSettings.getRenameColumnSql();
		return (sql != null);
	}

	public boolean canChangeNullable()
	{
		String dropNotNull = dbSettings.getAlterColumnDropNotNull();
		String setNotNull = dbSettings.getAlterColumnSetNotNull();
		return (dropNotNull != null && setNotNull != null);
	}

	public boolean canChangeDefault()
	{
		String alterDefault = dbSettings.getAlterColumnDefaultSql();
		String setDefault = dbSettings.getSetColumnDefaultSql();
		String dropDefault = dbSettings.getDropColumnDefaultSql();
		return (alterDefault != null || (setDefault != null && dropDefault != null));
	}

	public boolean canAddColumn()
	{
		String sql = dbSettings.getAddColumnSql();
		return sql != null;
	}

	public boolean canChangeComment()
	{
		String sql = commentMgr.getCommentSqlTemplate("column");
		return (sql != null);
	}

	protected String getColumnExpression(ColumnIdentifier column)
	{
		String colname = column.getColumnName();
		if (dbConn == null) return colname;
		if (dbConn.getMetadata().isReservedWord(colname)) return "\"" + colname + "\"";
		return colname;
	}

	protected String addColumn(TableIdentifier table, ColumnIdentifier newDefinition)
	{
		if (newDefinition == null) return null;
		String sql = dbSettings.getAddColumnSql();
		sql = sql.replace(PARAM_TABLE_NAME, table.getTableExpression(dbConn));
		sql = sql.replace(PARAM_COL_NAME, getColumnExpression(newDefinition));
		sql = sql.replace(PARAM_DATATYPE, newDefinition.getDbmsType());
		if (StringUtil.isBlank(newDefinition.getDefaultValue()))
		{
			sql = sql.replace(PARAM_DEFAULT_EXPR, "");
		}
		else
		{
			sql = sql.replace(PARAM_DEFAULT_EXPR, "DEFAULT " + newDefinition.getDefaultValue());
		}

		sql = ColumnDefinitionTemplate.replaceNullable(sql, dbSettings.getDbId(), newDefinition.isNullable());
		return sql;
	}

	protected String changeDataType(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		String sql = dbSettings.getAlterColumnDataTypeSql();
		if (StringUtil.isBlank(sql)) return null;

		String oldType = oldDefinition.getDbmsType();
		String newType = newDefinition.getDbmsType();
		if (oldType.trim().equalsIgnoreCase(newType.trim())) return null;
		sql = sql.replace(PARAM_COL_NAME, getColumnExpression(oldDefinition));
		sql = sql.replace(PARAM_TABLE_NAME, table.getTableExpression(dbConn));
		sql = sql.replace(PARAM_NEW_DATATYPE, newDefinition.getDbmsType());

		sql = changeCommonPlaceholders(sql, newDefinition);
		return sql;
	}

	protected String renameColumn(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		String sql = dbSettings.getRenameColumnSql();
		if (StringUtil.isBlank(sql)) return null;

		String oldName = getColumnExpression(oldDefinition);
		String newName = getColumnExpression(newDefinition);
		if (oldName.trim().equalsIgnoreCase(newName.trim())) return null;

		sql = sql.replace(PARAM_COL_NAME, getColumnExpression(oldDefinition));
		sql = sql.replace(PARAM_TABLE_NAME, table.getTableExpression(dbConn));
		sql = sql.replace(PARAM_NEW_COL_NAME, getColumnExpression(newDefinition));

		// Some stubid DBMS require the full data type definition of the column even if it should only be renamed...
		sql = changeCommonPlaceholders(sql, newDefinition);
		return sql;
	}

	private String changeNullable(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		boolean wasNullable = oldDefinition.isNullable();
		boolean isNowNullable = newDefinition.isNullable();
		if (wasNullable == isNowNullable) return null;

		String dropNotNull = dbSettings.getAlterColumnDropNotNull();
		String setNotNull = dbSettings.getAlterColumnSetNotNull();
		String sql = null;

		if (wasNullable && !isNowNullable)
		{
			// need to SET NOT NULL
			if (setNotNull == null) return null;
			sql = setNotNull;
		}
		else if (!wasNullable && isNowNullable)
		{
			sql = dropNotNull;
		}
		if (sql != null)
		{
			sql = sql.replace(PARAM_TABLE_NAME, table.getTableExpression(dbConn));
			sql = sql.replace(PARAM_COL_NAME, getColumnExpression(oldDefinition));
			sql = sql.replace(PARAM_DATATYPE, oldDefinition.getDbmsType());
		}
		sql = changeCommonPlaceholders(sql, newDefinition);
		return sql;
	}

	private String changeRemarks(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		String sql = commentMgr.getCommentSqlTemplate("column");
		if (StringUtil.isBlank(sql)) return null;

		String oldRemarks = (oldDefinition == null ? "" : oldDefinition.getComment());
		String newRemarks = newDefinition.getComment();
		if (StringUtil.equalStringOrEmpty(oldRemarks, newRemarks)) return null;
		if (StringUtil.isBlank(newRemarks)) newRemarks = "";

		sql = sql.replace(CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, table.getTableExpression(dbConn));
		sql = sql.replace(PARAM_TABLE_NAME, table.getTableName());
		sql = sql.replace(CommentSqlManager.COMMENT_SCHEMA_PLACEHOLDER, table.getSchema() == null ? "" : table.getSchema());
		sql = sql.replace(CommentSqlManager.COMMENT_COLUMN_PLACEHOLDER, getColumnExpression(oldDefinition == null ? newDefinition : oldDefinition));
		sql = sql.replace(CommentSqlManager.COMMENT_PLACEHOLDER, newRemarks.replace("'", "''"));
		if (oldDefinition != null)
		{
			sql = sql.replace(PARAM_DATATYPE, oldDefinition.getDbmsType());
		}
		if (newDefinition != null)
		{
			sql = sql.replace(PARAM_NEW_DATATYPE, newDefinition.getDbmsType());
		}
		return sql;
	}

	public String getColumnCommentSql(DbObject table, ColumnIdentifier column)
	{
		String remarks = column.getComment();
		if (StringUtil.isBlank(remarks)) remarks = "";
		String sql = commentMgr.getCommentSqlTemplate("column");

		sql = sql.replace(CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, table.getObjectExpression(dbConn));
		sql = sql.replace(PARAM_TABLE_NAME, table.getObjectName());
		sql = sql.replace(CommentSqlManager.COMMENT_SCHEMA_PLACEHOLDER, table.getSchema() == null ? "" : table.getSchema());
		sql = sql.replace(CommentSqlManager.COMMENT_COLUMN_PLACEHOLDER, getColumnExpression(column));
		sql = sql.replace(CommentSqlManager.COMMENT_PLACEHOLDER, remarks.replace("'", "''"));
		if (column != null)
		{
			sql = sql.replace(PARAM_NEW_DATATYPE, column.getDbmsType());
		}
		return sql;
	}

	private String changeDefault(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		String alterDefault = dbSettings.getAlterColumnDefaultSql();
		String setDefault = dbSettings.getSetColumnDefaultSql();
		String dropDefault = dbSettings.getDropColumnDefaultSql();

		String oldDefault = oldDefinition.getDefaultValue();
		String newDefault = newDefinition.getDefaultValue();

		String sql = null;

		if (oldDefault == null && newDefault == null) return null;
		if (oldDefault != null && oldDefault.equals(newDefault)) return null;

		if (oldDefault != null && newDefault == null)
		{
			// drop default
			if (dropDefault == null) return null;
			sql = dropDefault.replace(PARAM_TABLE_NAME, table.getTableExpression(dbConn));
		}

		// Cannot alter, need SET DEFAULT or DROP DEFAULT
		if (newDefault != null)
		{
			if (setDefault != null)
			{
				sql = setDefault.replace(PARAM_TABLE_NAME, table.getTableExpression(dbConn));
				sql = sql.replace(PARAM_DEFAULT_VALUE, newDefault);
			}
			else if (alterDefault != null)
			{
				sql = alterDefault.replace(PARAM_TABLE_NAME, table.getTableExpression(dbConn));
				sql = sql.replace(PARAM_DEFAULT_VALUE, newDefault);
			}
		}
		if (sql != null)
		{
			sql = sql.replace(PARAM_COL_NAME, getColumnExpression(oldDefinition));
			sql = sql.replace(PARAM_DATATYPE, oldDefinition.getDbmsType());
		}
		return sql;
	}

	private String nullableSql(boolean flag)
	{
		if (flag)
		{
			return Settings.getInstance().getProperty("workbench.db." + dbSettings.getDbId() + ".nullkeyword", "NULL");
		}
		else
		{
			return "NOT NULL";
		}
	}
}
