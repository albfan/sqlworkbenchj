/*
 * ColumnChanger
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import workbench.db.oracle.OracleMetadata;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnChanger
{
	public static final String PARAM_TABLE_NAME = "%table_name%";
	
	public static final String PARAM_COL_NAME = "%column_name%";
	public static final String PARAM_NEW_COL_NAME = "%new_column_name%";
	public static final String PARAM_DATATYPE = "%datatype%";
	public static final String PARAM_NEW_DATATYPE = "%new_datatype%";

	public static final String PARAM_NULLABLE = "%nullable%";
	public static final String PARAM_DEFAULT_VALUE = "%default_value%";


	// I'm storing connection and DbSettings in two different
	// variables so that I can initialize a ColumnChanger in the
	// Unit test without a connection.
	private WbConnection dbConn;
	private DbSettings dbSettings;

	public ColumnChanger(WbConnection con)
	{
		dbConn = con;
		dbSettings = (con != null ? con.getDbSettings() : null);
	}

	/**
	 * For unit testing
	 * @param con
	 */
	ColumnChanger(DbSettings settings)
	{
		dbConn = null;
		dbSettings = settings;
	}

	public String getAlterScript(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		List<String> statements = getAlterStatements(table, oldDefinition, newDefinition);
		if (statements.size() == 0) return null;

		StringBuilder result = new  StringBuilder(statements.size() * 50);

		if (dbConn.getMetadata().isOracle())
		{
			String oldComment = oldDefinition.getComment();
			String newComment = newDefinition.getComment();
			if (!StringUtil.equalStringOrEmpty(oldComment, newComment))
			{
				if (!OracleMetadata.remarksEnabled(dbConn))
				{
					result.append("-- ");
					result.append(ResourceMgr.getString("MsgSchemaReporterOracleRemarksWarning"));
				}
			}
		}

		for (String sql : statements)
		{
			result.append(sql);
			result.append(";\n");
		}
		return result.toString();
	}

	public void alterColumns(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
		throws SQLException
	{
		List<String> statements = getAlterStatements(table, oldDefinition, newDefinition);
		if (statements.size() == 0) return;

		Statement stmt = null;
		Savepoint sp = null;
		try
		{
			stmt = dbConn.createStatement();
			if (dbSettings.useSavePointForDDL())
			{
				sp = dbConn.setSavepoint();
			}

			for (String sql : statements)
			{
				LogMgr.logDebug("ColumnChanger.alterColumns()", "Running SQL: " + sql);
				stmt.execute(sql);
			}
			dbConn.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			if (sp != null)
			{
				dbConn.rollback(sp);
			}
			else if (dbSettings.ddlNeedsCommit())
			{
				dbConn.rollback();
			}
			throw e;
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	public List<String> getAlterStatements(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		List<String> result = CollectionUtil.arrayList();
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
		}
		else
		{
			sql = sql.replace(PARAM_DEFAULT_VALUE, defaultValue);
		}
		return sql;
	}

	protected String changeDataType(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		String sql = dbSettings.getAlterColumnDataTypeSql();
		if (StringUtil.isBlank(sql)) return null;

		String oldType = oldDefinition.getDbmsType();
		String newType = newDefinition.getDbmsType();
		if (oldType.trim().equalsIgnoreCase(newType.trim())) return null;
		sql = sql.replace(PARAM_COL_NAME, oldDefinition.getColumnName(dbConn));
		sql = sql.replace(PARAM_TABLE_NAME, table.getTableExpression(dbConn));
		sql = sql.replace(PARAM_NEW_DATATYPE, newDefinition.getDbmsType());

		sql = changeCommonPlaceholders(sql, newDefinition);
		return sql;
	}

	protected String renameColumn(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		String sql = dbSettings.getRenameColumnSql();
		if (StringUtil.isBlank(sql)) return null;

		String oldName = oldDefinition.getColumnName();
		String newName = newDefinition.getColumnName();
		if (oldName.trim().equalsIgnoreCase(newName.trim())) return null;

		sql = sql.replace(PARAM_COL_NAME, oldDefinition.getColumnName(dbConn));
		sql = sql.replace(PARAM_TABLE_NAME, table.getTableExpression(dbConn));
		sql = sql.replace(PARAM_NEW_COL_NAME, newDefinition.getColumnName());

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
			sql = sql.replace(PARAM_COL_NAME, oldDefinition.getColumnName(dbConn));
			sql = sql.replace(PARAM_DATATYPE, oldDefinition.getDbmsType());
		}
		sql = changeCommonPlaceholders(sql, newDefinition);
		return sql;
	}

	private String changeRemarks(TableIdentifier table, ColumnIdentifier oldDefinition, ColumnIdentifier newDefinition)
	{
		CommentSqlManager mgr = new CommentSqlManager(this.dbSettings.getDbId());
		String sql = mgr.getCommentSqlTemplate("column");
		if (StringUtil.isBlank(sql)) return null;

		String oldRemarks = oldDefinition.getComment();
		String newRemarks = newDefinition.getComment();
		if (StringUtil.equalStringOrEmpty(oldRemarks, newRemarks)) return null;
		if (StringUtil.isBlank(newRemarks)) newRemarks = "";

		sql = sql.replace(CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, table.getTableExpression(dbConn));
		sql = sql.replace(CommentSqlManager.COMMENT_COLUMN_PLACEHOLDER, oldDefinition.getColumnName(dbConn));
		sql = sql.replace(CommentSqlManager.COMMENT_PLACEHOLDER, newRemarks.replace("'", "''"));
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
			sql = sql.replace(PARAM_COL_NAME, oldDefinition.getColumnName(dbConn));
			sql = sql.replace(PARAM_DATATYPE, oldDefinition.getDbmsType());
		}
		return sql;
	}

	private String nullableSql(boolean flag)
	{
		if (flag)
		{
			return "NULL";
		}
		else
		{
			return "NOT NULL";
		}
	}
}
