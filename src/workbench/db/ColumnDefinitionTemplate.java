/*
 * ColumnDefinitionTemplate
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * A template class for column definitions, used when reconstructing the source code of a table.
 *
 * The template only deals with the column <b>definition</b> (i.e. data type and constraints) not the actual column name.
 *
 * @author Thomas Kellerer
 */
public class ColumnDefinitionTemplate
{
	public static enum ColumnType
	{
		regularColumn,
		computedColumn;
	}

	/**
	 * Will be replaced only if the column is not null, if the column
	 * is nullable no SQL will be generated
	 */
	public static final String PARAM_NOT_NULL = "%not_null%";

	/**
	 * The placeholder for any column constraint
	 */
	public static final String PARAM_COL_CONSTRAINTS = "%column_constraint%";

	/**
	 * The placeholder for the expression of a computed column
	 */
	public static final String PARAM_EXPRESSION = "%expression%";

	private String dbid;
	private String template;

	public ColumnDefinitionTemplate()
	{
	}

	public ColumnDefinitionTemplate(String id)
	{
		dbid = id;
	}

	/**
	 * Build the datatype part of a column definition.
	 *
	 * The result will not include the column's name.
	 *
	 * @param column  the column definition to use
	 * @param colConstraint column constraints if available (e.g. CHECK constraints)
	 * @param typeLength the datatype will be padded with spaces to fill this length
	 *
	 * @return the data definition part of a column in a CREATE TABLE statement
	 */
	public String getColumnDefinitionSQL(ColumnIdentifier column, String colConstraint, int typeLength)
	{
		return getColumnDefinitionSQL(column, colConstraint, typeLength, null);
	}

	/**
	 * Build the datatype part of a column definition.
	 *
	 * The result will not include the column's name.
	 *
	 * @param column  the column definition to use
	 * @param colConstraint column constraints if available (e.g. CHECK constraints)
	 * @param typeLength the datatype will be padded with spaces to fill this length
	 * @param dataTypeOverride an alternative data type, if this is non-null it will be used instead
	 *                        (otherwise the one stored in the column definition will be use)
	 *
	 * @return the data definition part of a column in a CREATE TABLE statement
	 *
	 * @see ColumnChanger#PARAM_DATATYPE
	 * @see ColumnChanger#PARAM_DEFAULT_VALUE
	 * @see ColumnChanger#PARAM_NULLABLE
	 */
	public String getColumnDefinitionSQL(ColumnIdentifier column, String colConstraint, int typeLength, String dataTypeOverride)
	{
		String expr = column.getComputedColumnExpression();
		boolean isComputed = StringUtil.isNonBlank(expr);
		String sql = getTemplate(isComputed, column.isAutoincrement());

		String type = StringUtil.padRight(dataTypeOverride == null ? column.getDbmsType() : dataTypeOverride, typeLength);

		sql = replaceArg(sql, ColumnChanger.PARAM_DATATYPE, type);
		String def = column.getDefaultValue();
		if (StringUtil.isNonBlank(def))
		{
			sql = replaceArg(sql, ColumnChanger.PARAM_DEFAULT_VALUE, "DEFAULT " + def);
		}
		else
		{
			sql = replaceArg(sql, ColumnChanger.PARAM_DEFAULT_VALUE, "");
		}
		sql = replaceNullable(sql, dbid, column.isNullable());
		sql = replaceArg(sql, PARAM_COL_CONSTRAINTS, colConstraint);
		sql = replaceArg(sql, PARAM_EXPRESSION, expr);
		return sql.trim();
	}

	public static String replaceNullable(String sql, String dbIdentifier, boolean isNullable)
	{
		String nullkeyword = Settings.getInstance().getProperty("workbench.db." + dbIdentifier + ".nullkeyword", "NULL");
		if (isNullable)
		{
			sql = replaceArg(sql, ColumnChanger.PARAM_NULLABLE, nullkeyword);
			sql = replaceArg(sql, PARAM_NOT_NULL, "");
		}
		else
		{
			// Only one of the placeholders should be there
			sql = replaceArg(sql, ColumnChanger.PARAM_NULLABLE, "NOT NULL");
			sql = replaceArg(sql, PARAM_NOT_NULL, "NOT NULL");
		}
		return sql;
	}

	public static String replaceArg(String template, String placeholder, String value)
	{
		if (StringUtil.isBlank(value))
		{
			return template.replaceFirst(placeholder + "\\s*|" + placeholder + "$", "");
		}
		return template.replace(placeholder, value);
	}

	private String getTemplate(boolean computedColumn, boolean isAutoincrement)
	{
		if (template != null) return template;

		String sql = null;
		if (computedColumn)
		{
			sql = getProperty("coldef.computed", PARAM_EXPRESSION);
			if (isAutoincrement)
			{
				sql = getProperty("coldef.computed.autoinc", ColumnChanger.PARAM_DATATYPE + " " + PARAM_EXPRESSION);
			}
		}
		else
		{

			sql = getProperty("coldef", ColumnChanger.PARAM_DATATYPE + " " + ColumnChanger.PARAM_DEFAULT_VALUE + " " + PARAM_NOT_NULL + " " + PARAM_COL_CONSTRAINTS);
		}
		return sql;
	}

	private String getProperty(String suffix, String defaultValue)
	{
		String sql = Settings.getInstance().getProperty("workbench.db.sql." + suffix, defaultValue);
		if (StringUtil.isNonBlank(dbid))
		{
			sql = Settings.getInstance().getProperty("workbench.db." + dbid + "." + suffix, sql);
		}
		return sql;
	}
	/**
	 * For unit testing only
	 */
	void setTemplate(String templateSql)
	{
		template = templateSql;
	}

}
