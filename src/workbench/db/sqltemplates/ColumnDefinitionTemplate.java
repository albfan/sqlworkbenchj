/*
 * ColumnDefinitionTemplate.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.sqltemplates;

import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;

import workbench.util.SqlUtil;
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
	 * Will be replaced only if the column is not null
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

	public static final String PARAM_COLLATION_NAME = "%collation%";

	public static final String PARAM_GENERATED = "%generated%";

	private String dbid;
	private String template;
	private boolean fixDefaultExpression;

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
		boolean isGenerator = StringUtil.isNonBlank(column.getGeneratorExpression());
		String sql = getTemplate(isComputed, column.isAutoincrement(), isGenerator);

		String type = StringUtil.padRight(dataTypeOverride == null ? column.getDbmsType() : dataTypeOverride, typeLength);

		sql = replaceArg(sql, ColumnChanger.PARAM_DATATYPE, type);
		boolean isDefaultConstraint = false;
		if (StringUtil.isNonBlank(colConstraint))
		{
			isDefaultConstraint = colConstraint.indexOf("DEFAULT") > -1;
		}

		String def = getDefaultExpression(column);
		if (isDefaultConstraint)
		{
			sql = replaceArg(sql, ColumnChanger.PARAM_DEFAULT_VALUE, colConstraint);
		}
		else if (StringUtil.isNonBlank(def))
		{
			sql = replaceArg(sql, ColumnChanger.PARAM_DEFAULT_VALUE, column.getDefaultClause() + " " + def);
		}
		else
		{
			sql = replaceArg(sql, ColumnChanger.PARAM_DEFAULT_VALUE, "");
		}
		sql = replaceNullable(sql, dbid, column.isNullable(), colConstraint);

		if (isDefaultConstraint)
		{
			sql = replaceArg(sql, PARAM_COL_CONSTRAINTS, "");
		}
		else
		{
			sql = replaceArg(sql, PARAM_COL_CONSTRAINTS, colConstraint);
		}
		sql = replaceArg(sql, PARAM_EXPRESSION, expr);
		sql = replaceArg(sql, PARAM_COLLATION_NAME, column.getCollationExpression());
		sql = replaceArg(sql, PARAM_GENERATED, column.getGeneratorExpression());
		return sql.trim();
	}

	public void setFixDefaultValues(boolean flag)
	{
		this.fixDefaultExpression = flag;
	}

	/**
	 * Fix potential problems with default values for some JDBC drivers.
	 *
	 * Some drivers seem to return default values that are not "real" expressions but e.g. an
	 * unquoted string even though the column is a character column.
	 *
	 * This method will fix this for character columns.
	 * If the DBMS is MySQL duplicated brackets at the start and end of the expression are also replaced by
	 * single brackets.
	 *
	 * @param column the column
	 * @return a valid expression for the column default.
	 */
	public String getDefaultExpression(ColumnIdentifier column)
	{
		String value = column.getDefaultValue();

		if (value == null) return null;

		boolean isMySQL = dbid != null && dbid.equals("mysql");

		// If the ColumnDefinition was retrieved from a different DBMS (e.g. SQL Server) it might be that
		// the default value is defined as ((42)).
		// MySQL's SQL parser is not smart enough to accept that as a valid default expression, so we need to fix it here.
		if (isMySQL && SqlUtil.isNumberType(column.getDataType()))
		{
			value = value.replaceAll("^\\(+", "");
			value = value.replaceAll("\\)+$", "");
		}

		if (!fixDefaultExpression) return value;

		boolean addQuotes = false;
		if (SqlUtil.isCharacterType(column.getDataType()))
		{
      // don't quote NULL for character data
      if ("NULL".equalsIgnoreCase(value)) return "NULL";

			if (!value.startsWith("'") && !value.startsWith("N'") && !value.startsWith("E'") && !value.startsWith("U&'"))
			{
				addQuotes = true;
			}
		}

		value = value.trim();
		if (isMySQL && SqlUtil.isDateType(column.getDataType()) && !value.startsWith("'") && !value.equalsIgnoreCase("CURRENT_TIMESTAMP"))
		{
			addQuotes = true;
		}

		if (addQuotes)
		{
			return "'" + value + "'";
		}

		return value;
	}

	public static String replaceNullable(String sql, String dbIdentifier, boolean isNullable, String columnConstraint)
	{
		boolean isNotNullConstraint = columnConstraint != null && columnConstraint.toUpperCase().trim().endsWith("NOT NULL");
		if (isNotNullConstraint)
		{
			// the column constraint seems to be a NOT NULL constraint
			// do not retrieve the not null condition in that case.
			sql = replaceArg(sql, ColumnChanger.PARAM_NULLABLE, "");
			sql = replaceArg(sql, PARAM_NOT_NULL, "");
			return sql;
		}

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

	private String getTemplate(boolean computedColumn, boolean isAutoincrement, boolean isGenerator)
	{
		if (template != null) return template;

		String sql;
		if (computedColumn)
		{
			sql = getProperty("coldef.computed", PARAM_EXPRESSION);
			if (isAutoincrement)
			{
				sql = getProperty("coldef.computed.autoinc", ColumnChanger.PARAM_DATATYPE + " " + PARAM_EXPRESSION);
			}
		}
		else if (isGenerator)
		{
			sql = getProperty("coldef.generator", ColumnChanger.PARAM_DATATYPE + " " + PARAM_GENERATED);
		}
		else if (isAutoincrement)
		{
			sql = getProperty("coldef.autoinc", ColumnChanger.PARAM_DATATYPE + " " + ColumnChanger.PARAM_DEFAULT_VALUE + " " + PARAM_NOT_NULL + " " + PARAM_COL_CONSTRAINTS);
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
