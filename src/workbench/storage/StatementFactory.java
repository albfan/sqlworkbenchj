/*
 * StatementFactory.java
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
package workbench.storage;

import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionProfile;
import workbench.db.DbSettings;
import workbench.db.DefaultExpressionBuilder;
import workbench.db.DmlExpressionBuilder;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.formatter.FormatterUtil;

import workbench.util.SqlUtil;

/**
 * A class to generate DELETE, INSERT or UPDATE statements based
 * on the data in a {@link workbench.storage.RowData} object.
 *
 * @author  Thomas Kellerer
 */
public class StatementFactory
{
	private ResultInfo resultInfo;
	private TableIdentifier tableToUse;
	private boolean includeTableOwner = true;
	private WbConnection dbConnection;
	private boolean emptyStringIsNull;
	private boolean includeNullInInsert = true;
	private boolean useColumnLabel;
	private boolean includeReadOnlyColumns;
	private boolean includeIdentityColumns;
	private DmlExpressionBuilder expressionBuilder;

	/**
	 * @param metaData the description of the resultSet for which the statements are generated
	 * @param conn the database connection for which the statements are generated
	 */
	public StatementFactory(ResultInfo metaData, WbConnection conn)
	{
		this.resultInfo = metaData;
		this.setCurrentConnection(conn);
		expressionBuilder = DmlExpressionBuilder.Factory.getBuilder(conn);
		includeIdentityColumns = !Settings.getInstance().getGenerateInsertIgnoreIdentity();
	}

	public void setIncludeIdentiyColumns(boolean flag)
	{
		this.includeIdentityColumns = flag;
	}

	public void setIncludeReadOnlyColumns(boolean flag)
	{
		this.includeReadOnlyColumns = flag;
	}

	/**
	 * Controls if the column name or the column's display name should be used
	 * to generate the SQL statement.
	 */
	public void setUseColumnLabel(boolean flag)
	{
		this.useColumnLabel = flag;
	}

	public DmlStatement createUpdateStatement(RowData aRow, boolean ignoreStatus, String lineEnd)
	{
		return createUpdateStatement(aRow, ignoreStatus, lineEnd, null);
	}

	/**
	 *	Create an UPDATE Statement based on the data provided
	 *
	 *	@param aRow						the RowData that should be used for the UPDATE statement
	 *	@param ignoreStatus		if set to true all columns will be included (otherwise only modified columns)
	 *	@param lineEnd				the character sequence to be used as the line ending
	 *	@param columns				a list of columns to be included. If this is null all columns are included
	 */
	public DmlStatement createUpdateStatement(RowData aRow, boolean ignoreStatus, String lineEnd, List<ColumnIdentifier> columns)
	{
		if (aRow == null) return null;
		boolean first = true;
		int cols = this.resultInfo.getColumnCount();

		if (!resultInfo.hasPkColumns()) throw new IllegalArgumentException("Cannot proceed without a primary key");

		DmlStatement dml = null;

		if (!ignoreStatus && !aRow.isModified()) return null;
		ArrayList<ColumnData> values = new ArrayList<>(cols);
		StringBuilder sql = new StringBuilder(50);
		appendKeyword(sql, "UPDATE ");
		sql.append(getTableNameToUse());
		appendKeyword(sql, " SET ");
		first = true;

		for (int col=0; col < cols; col ++)
		{
			if (columns != null)
			{
				if (!columns.contains(this.resultInfo.getColumn(col))) continue;
			}

			if (aRow.isColumnModified(col) || (ignoreStatus && !this.resultInfo.isPkColumn(col)))
			{
				if (first)
				{
					first = false;
				}
				else
				{
					sql.append(", ");
				}

				String colName = getColumnName(col);

				sql.append(colName);
				Object value = aRow.getValue(col);
				if (isNull(value))
				{
					appendKeyword(sql, " = NULL");
				}
				else
				{
					sql.append(" = ");
					sql.append(getDmlExpression(this.resultInfo.getColumn(col)));
					values.add(new ColumnData(value, this.resultInfo.getColumn(col)));
				}
			}
		}
		appendKeyword(sql, " WHERE ");
		first = true;
		int count = this.resultInfo.getColumnCount();
		for (int j=0; j < count; j++)
		{
			if (!this.resultInfo.isPkColumn(j)) continue;
			if (first)
			{
				first = false;
			}
			else
			{
				appendKeyword(sql, " AND ");
			}
			String colName = getColumnName(j);
			sql.append(colName);

			Object value = aRow.getOriginalValue(j);
			if (value == null)
			{
				appendKeyword(sql, " IS NULL");
			}
			else
			{
				sql.append(" = ");
				sql.append(getDmlExpression(resultInfo.getColumn(j)));
				values.add(new ColumnData(value, resultInfo.getColumn(j)));
			}
		}

		dml = new DmlStatement(sql.toString(), values);
		return dml;
	}

	private String getDmlExpression(ColumnIdentifier column)
	{
		return expressionBuilder.getDmlExpression(column);
	}

	private String getColumnName(int column)
	{
		String name = this.useColumnLabel ? this.resultInfo.getColumnDisplayName(column) : this.resultInfo.getColumnName(column);
		return adjustColumnName(name);
	}

	/**
	 * Set a different DbSettings configuration.
	 *
	 * This is only intended for testing purposes
	 *
	 */
	void setTestSettings(DbSettings settings)
	{
		if (this.expressionBuilder instanceof DefaultExpressionBuilder)
		{
			((DefaultExpressionBuilder)expressionBuilder).setDbSettings(settings);
		}
	}

	public DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus, String lineEnd)
	{
		return this.createInsertStatement(aRow, ignoreStatus, lineEnd, null);
	}

	private boolean includeInsertColumn(ColumnIdentifier colId)
	{
		if (colId.isAutoGenerated())
		{
			if (!includeIdentityColumns)
			{
				LogMgr.logDebug("StatementFactory.includeInsertColumn()", "Ignoring column " + getTableNameToUse() + "." + colId.getColumnName() + " because it is an auto-increment/identity column");
				return false;
			}
		}
		else
		{
			if (includeReadOnlyColumns) return true;

			if (colId.getComputedColumnExpression() != null)
			{
				LogMgr.logDebug("StatementFactory.includeInsertColumn()", "Ignoring column " + getTableNameToUse() + "." + colId.getColumnName() + " because it is a computed column");
				return false;
			}

			if (!colId.isUpdateable() || colId.isReadonly())
			{
				LogMgr.logDebug("StatementFactory.includeInsertColumn()", "Ignoring column " + getTableNameToUse() + "." + colId.getColumnName() + " because it is marked as not updateable");
				return false;
			}
		}
		return true;
	}

	/**
	 *	Generate an insert statement for the given row
	 *	When creating a script for the DataStore the ignoreStatus
	 *	will be passed as true, thus ignoring the row status and
	 *	some basic formatting will be applied to the SQL Statement
	 *
	 *	@param aRow the RowData that should be used for the insert statement
	 *	@param ignoreStatus if set to true all columns will be included (otherwise only modified columns)
	 *	@param lineEnd the character sequence to be used as the line ending
	 *	@param columns  a list of columns to be included. If this is null all columns are included
	 */
	public DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus, String lineEnd, List<ColumnIdentifier> columns)
	{
		if (!ignoreStatus && !aRow.isModified()) return null;

		DmlStatement dml;
		int cols = this.resultInfo.getColumnCount();

		ArrayList<ColumnData> values = new ArrayList<>(cols);

		StringBuilder sql = new StringBuilder(250);
    appendKeyword(sql, "INSERT INTO ");
		sql.append(getTableNameToUse());
		sql.append(" (");

		StringBuilder valuePart = new StringBuilder(250);

		boolean first = true;

		int realColcount = 0;

		for (int col=0; col < cols; col ++)
		{
			ColumnIdentifier colId = this.resultInfo.getColumn(col);

			boolean isSelectedColumn = false;

			if (columns == null)
			{
				// only check for identity columns and non-updateable columns if they weren't explicitely selected
				// this happens when the user selects specific columns when e.g. copying the data as SQL insert
				// from the result set
				if (!includeInsertColumn(colId)) continue;
			}
			else
			{
				isSelectedColumn = ColumnIdentifier.containsColumn(columns, colId);
				if (!isSelectedColumn) continue;
			}

			realColcount ++;
			Object value = aRow.getValue(col);
			boolean isNull = isNull(value);

			boolean includeCol = (ignoreStatus || aRow.isColumnModified(col));

			if (isNull)
			{
				includeCol = includeCol || this.includeNullInInsert;
			}

			if (includeCol)
			{
				if (first)
				{
					first = false;
				}
				else
				{
					sql.append(',');
					valuePart.append(',');
				}
				sql.append(getColumnName(col));

				// getDmlExpression() will return a parameter placeholder including any cast (or function call)
				// to convert the input value for the JDBC driver
				// see DmlExpressionBuilder.getDmlExpression()
				valuePart.append(getDmlExpression(colId));
				values.add(new ColumnData(value,colId));
			}
		}
		appendKeyword(sql, ") VALUES (");
		sql.append(valuePart);
		sql.append(')');
		dml = new DmlStatement(sql.toString(), values);

		if (realColcount == 0)
		{
			LogMgr.logError("StatementFactory.createInsertStatement()", "No columns included in the INSERT statement for the table " + getTableNameToUse() + " Please turn on DEBUG logging to see why the columns where excluded", null);
		}
		return dml;
	}

	public DmlStatement createDeleteStatement(RowData aRow)
	{
		return createDeleteStatement(aRow, false);
	}

	/**
	 * Generate a DELETE statement that will delete the row from the database.
	 *
	 * @param row the row to be deleted
	 * @param ignoreStatus if false, the row will only be deleted if it's not "new"
	 * @return a DELETE statement or null, if the row was new and does not need to be deleted
	 * @see workbench.storage.RowData#isNew()
	 */
	public DmlStatement createDeleteStatement(RowData row, boolean ignoreStatus)
	{
		if (row == null) return null;
		if (!ignoreStatus && row.isNew()) return null;

		boolean first = true;
		DmlStatement dml;
		int count = this.resultInfo.getColumnCount();

		ArrayList<ColumnData> values = new ArrayList<>(count);
		StringBuilder sql = new StringBuilder(250);
    appendKeyword(sql, "DELETE FROM ");
		sql.append(getTableNameToUse());
		appendKeyword(sql, " WHERE ");
		first = true;

		for (int j=0; j < count; j++)
		{
			if (!this.resultInfo.isPkColumn(j)) continue;
			if (first)
			{
				first = false;
			}
			else
			{
				appendKeyword(sql, " AND ");
			}
			sql.append(getColumnName(j));

			Object value = row.getOriginalValue(j);
			if (isNull(value)) value = null;
			if (value == null)
			{
				appendKeyword(sql, " IS NULL");
			}
			else
			{
				sql.append(" = ");
				sql.append(getDmlExpression(resultInfo.getColumn(j)));
				values.add(new ColumnData(value, resultInfo.getColumn(j)));
			}
		}

		dml = new DmlStatement(sql.toString(), values);
		return dml;
	}

	/**
	 * Defines an alternative table to be used when generating the SQL statements.
	 * By default the table defined through the ResultInfo from the constructor is used.
	 *
	 * @param table The table to be used
	 */
	public void setTableToUse(TableIdentifier table)
	{
		this.tableToUse = table;
	}

	/**
	 * Control the usage of table owner/catalog/schema in the generated statements.
	 * If this is set to false, the owner/catalog/schema will <b>never</b> included
	 * in the generated SQL. If this is set to true (which is the default), the
	 * owner/catalog/schema is included in the table name if necessary.
	 *
	 * @param flag turn the usage of the table owner on or off
	 * @see workbench.db.DbMetadata#needCatalogInDML(workbench.db.TableIdentifier)
	 * @see workbench.db.DbMetadata#needSchemaInDML(workbench.db.TableIdentifier)
	 * @see workbench.db.TableIdentifier#getTableExpression(workbench.db.WbConnection)
	 */
	public void setIncludeTableOwner(boolean flag)
	{
		this.includeTableOwner = flag;
	}

	public void setEmptyStringIsNull(boolean flag)
	{
		this.emptyStringIsNull = flag;
	}

	public void setIncludeNullInInsert(boolean flag)
	{
		this.includeNullInInsert = flag;
	}

	public final void setCurrentConnection(WbConnection conn)
	{
		this.dbConnection = conn;
		if (this.dbConnection != null)
		{
			ConnectionProfile prof = dbConnection.getProfile();
			emptyStringIsNull = (prof == null ? true : prof.getEmptyStringIsNull());
			includeNullInInsert = (prof == null ? true : prof.getIncludeNullInInsert());
		}
	}

	private String adjustColumnName(String colName)
	{
		if (colName == null) return null;
		String name = null;

		if (dbConnection == null)
		{
			name = SqlUtil.quoteObjectname(colName, false, true, '"');
		}
		else
		{
			name = dbConnection.getMetadata().quoteObjectname(colName);
		}
		return FormatterUtil.getIdentifier(name);
	}

	private TableIdentifier getUpdateTable()
	{
		return tableToUse != null ? tableToUse : resultInfo.getUpdateTable();
	}

	private String getTableNameToUse()
	{
		String tname = null;

		TableIdentifier updateTable = getUpdateTable();
		if (updateTable == null) throw new IllegalArgumentException("Cannot proceed without update table defined");

		TableIdentifier toUse = updateTable.createCopy();
		toUse.adjustCase(dbConnection);

		if (includeTableOwner)
		{
			tname = toUse.getTableExpression(this.dbConnection);
		}
		else
		{
			if (dbConnection == null)
			{
				tname = SqlUtil.quoteObjectname(toUse.getTableName(), false, true, '"');
			}
			else
			{
				tname = dbConnection.getMetadata().quoteObjectname(toUse.getTableName());
			}
		}
		return FormatterUtil.getIdentifier(tname);
	}

	private void appendKeyword(StringBuilder text, String toAppend)
	{
    text.append(FormatterUtil.getKeyword(toAppend));
	}

	private boolean isNull(Object value)
	{
		if (value == null) return true;
		String s = value.toString();
		return emptyStringIsNull && s.isEmpty();
	}

}
