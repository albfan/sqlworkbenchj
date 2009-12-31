/*
 * StatementFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionProfile;
import workbench.db.DbSettings;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.util.StringUtil;

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

	private static final int CASE_NO_CHANGE = 1;
	private static final int CASE_UPPER = 2;
	private static final int CASE_LOWER = 4;
	private int identifierCase = CASE_NO_CHANGE;

	// DbSettings used by the Unit tests
	private DbSettings testSettings;

	/**
	 * @param metaData the description of the resultSet for which the statements are generated
	 * @param conn the database connection for which the statements are generated
	 */
	public StatementFactory(ResultInfo metaData, WbConnection conn)
	{
		this.resultInfo = metaData;
		this.setCurrentConnection(conn);
		String s = Settings.getInstance().getGeneratedSqlTableCase();
		if (!StringUtil.isEmptyString(s))
		{
			if (s.equals("lower")) identifierCase = CASE_LOWER;
			else if (s.equals("upper")) identifierCase = CASE_UPPER;
		}
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
	public DmlStatement createUpdateStatement(RowData aRow, boolean ignoreStatus, String lineEnd, List columns)
	{
		if (aRow == null) return null;
		boolean first = true;
		int cols = this.resultInfo.getColumnCount();

		boolean doFormatting = Settings.getInstance().getDoFormatUpdates();
		int columnThresholdForNewline = Settings.getInstance().getFormatUpdateColumnThreshold();

		boolean newLineAfterColumn = doFormatting && (cols > columnThresholdForNewline);

		if (!resultInfo.hasPkColumns()) throw new IllegalArgumentException("Cannot proceed without a primary key");
		
		DmlStatement dml = null;

		if (!ignoreStatus && !aRow.isModified()) return null;
		ArrayList<ColumnData> values = new ArrayList<ColumnData>(cols);
		StringBuilder sql = new StringBuilder("UPDATE ");

		sql.append(getTableNameToUse());
		if (doFormatting) sql.append("\n  ");
		sql.append(" SET ");
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
					if (newLineAfterColumn) sql.append("\n       ");
				}
				String colName = adjustColumnName(this.resultInfo.getColumnName(col));

				sql.append(colName);
				Object value = aRow.getValue(col);
				if (isNull(value))
				{
					sql.append(" = NULL");
				}
				else
				{
					String literal = getTemplateValue(resultInfo.getDbmsTypeName(col), value);
					if (literal != null)
					{
						sql.append(" = ");
						sql.append(literal);
					}
					else
					{
						sql.append(" = ?");
						values.add(new ColumnData(value, this.resultInfo.getColumn(col)));
					}
				}
			}
		}
		if (doFormatting) sql.append("\n ");
		sql.append(" WHERE ");
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
				sql.append(" AND ");
			}
			String colName = adjustColumnName(this.resultInfo.getColumnName(j));
			sql.append(colName);

			Object value = aRow.getOriginalValue(j);
			if (value == null)
			{
				sql.append(" IS NULL");
			}
			else
			{
				sql.append(" = ");
				String literal = getTemplateValue(resultInfo.getDbmsTypeName(j), value);
				if (literal != null)
				{
					sql.append(literal);
				}
				else
				{
					sql.append("?");
					values.add(new ColumnData(value, resultInfo.getColumn(j)));
				}
			}
		}

		dml = new DmlStatement(sql, values);
		return dml;
	}

	void setTestSettings(DbSettings settings)
	{
		testSettings = settings;
	}
	
	protected DbSettings getDbSettings()
	{
		if (testSettings != null) return testSettings;
		if (dbConnection == null) return null;
		return dbConnection.getDbSettings();
	}

	protected String getTemplateValue(String dbmsType, Object value)
	{
		if (value == null) return null;
		if (this.getDbSettings() == null) return null;
		String template = getDbSettings().getValueTemplate(dbmsType);
		if (template == null) return null;
		return template.replace("%value%", value.toString());
	}
	
	public DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus, String lineEnd)
	{
		return this.createInsertStatement(aRow, ignoreStatus, lineEnd, null);
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
	public DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus, String lineEnd, List columns)
	{
		boolean first = true;
		DmlStatement dml;

		if (!ignoreStatus && !aRow.isModified()) return null;

		int cols = this.resultInfo.getColumnCount();

		boolean doFormatting = Settings.getInstance().getDoFormatInserts();
		int columnThresholdForNewline = Settings.getInstance().getIntProperty("workbench.sql.generate.insert.newlinethreshold",5);
		boolean newLineAfterColumn = doFormatting && (cols > columnThresholdForNewline);
		boolean skipIdentityCols = Settings.getInstance().getFormatInsertIgnoreIdentity();
		int colsPerLine = Settings.getInstance().getFormatInsertColsPerLine();

		ArrayList<ColumnData> values = new ArrayList<ColumnData>(cols);
		StringBuilder sql = new StringBuilder(250);
    sql.append("INSERT INTO ");
		StringBuilder valuePart = new StringBuilder(250);

		sql.append(getTableNameToUse());
		if (doFormatting) sql.append(lineEnd);
		else sql.append(' ');

		sql.append('(');
		if (newLineAfterColumn)
		{
			sql.append(lineEnd);
			sql.append("  ");
			valuePart.append(lineEnd);
			valuePart.append("  ");
			if (colsPerLine == 1)
			{
				sql.append("  ");
				valuePart.append("  ");
			}
		}

		first = true;
    String colName = null;
		int colsInThisLine = 0;

		for (int col=0; col < cols; col ++)
		{
			ColumnIdentifier colId = this.resultInfo.getColumn(col);
			if (columns != null)
			{
				if (!columns.contains(colId)) continue;
			}

			if (skipIdentityCols && colId.isIdentityColumn()) continue;

			Object value = aRow.getValue(col);
			boolean isNull = isNull(value);

			boolean includeCol = (ignoreStatus || aRow.isColumnModified(col));

			if (includeCol)
			{
				if (isNull)
				{
					includeCol = this.includeNullInInsert;
				}
			}

			if (includeCol)
			{
				if (!first)
				{
					if (newLineAfterColumn && colsInThisLine >= colsPerLine)
					{
						if (colsPerLine == 1)
						{
							sql.append(lineEnd);
							valuePart.append(lineEnd);
							sql.append("  , ");
							valuePart.append("  , ");
						}
						else
						{
							sql.append(", ");
							valuePart.append(", ");
							sql.append(lineEnd);
							valuePart.append(lineEnd);
						}
						colsInThisLine = 0;
					}
					else
					{
						sql.append(", ");
						valuePart.append(", ");
					}
				}
				else
				{
					first = false;
				}

				colName = adjustColumnName(this.resultInfo.getColumnName(col));

				sql.append(colName);
				String literal = getTemplateValue(resultInfo.getDbmsTypeName(col), value);
				if (literal != null)
				{
					valuePart.append(literal);
				}
				else
				{
					valuePart.append('?');
					values.add(new ColumnData(value,this.resultInfo.getColumn(col)));
				}
			}
			colsInThisLine ++;
		}
		if (newLineAfterColumn)
		{
			sql.append(lineEnd);
			valuePart.append(lineEnd);
		}

		sql.append(')');
		if (doFormatting)
		{
			sql.append(lineEnd);
			sql.append("VALUES");
			sql.append(lineEnd);
		}
		else
		{
			sql.append(" VALUES ");
		}
		sql.append('(');
		sql.append(valuePart);
		sql.append(')');

		dml = new DmlStatement(sql, values);
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

		ArrayList<ColumnData> values = new ArrayList<ColumnData>(count);
		StringBuilder sql = new StringBuilder(250);
    sql.append("DELETE FROM ");
		sql.append(getTableNameToUse());
		sql.append(" WHERE ");
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
				sql.append(" AND ");
			}
			String colName = adjustColumnName(this.resultInfo.getColumnName(j));
			sql.append(colName);

			Object value = row.getOriginalValue(j);
			if (isNull(value)) value = null;
			if (value == null)
			{
				sql.append(" IS NULL");
			}
			else
			{
				String literal = getTemplateValue(resultInfo.getDbmsTypeName(j), value);
				if (literal != null)
				{
					sql.append(" = ");
					sql.append(literal);
				}
				else
				{
					sql.append(" = ?");
					values.add(new ColumnData(value, resultInfo.getColumn(j)));
				}
			}
		}

		dml = new DmlStatement(sql, values);
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
	
	public void setCurrentConnection(WbConnection conn)
	{
		this.dbConnection = conn;
		if (this.dbConnection != null)
		{
			ConnectionProfile prof = dbConnection.getProfile();
			emptyStringIsNull = (prof == null ? true : prof.getEmptyStringIsNull());
			includeNullInInsert = (prof == null ? true : prof.getIncludeNullInInsert());
		}
	}
	
	private String adjustColumnName(String value)
	{
		if (value == null) return null;
		if (value.startsWith("\"")) return value;
		if (dbConnection != null && !dbConnection.getMetadata().isDefaultCase(value))
		{
			return dbConnection.getMetadata().quoteObjectname(value);
		}
		return value;
	}

	private TableIdentifier getUpdateTable()
	{
		return tableToUse != null ? tableToUse : resultInfo.getUpdateTable();
	}
	
	private String adjustIdentifierCase(String value)
	{
		if (value == null) return null;
		if (value.startsWith("\"")) return value;
		
		// If the table name is not in the same case the server stores it
		// and the case may not be changed at all, then we need to quote the table name.
		
		TableIdentifier updateTable = getUpdateTable();
		
		// setNeverAdjustCase() will only be set for TableIdentifiers that have
		// been "retrieved" from the database (e.g. in the DbExplorer)
		// For table names that the user entered, neverAdjustCase() will be false
		boolean neverAdjust = (updateTable == null ? false : updateTable.getNeverAdjustCase());
		if (neverAdjust) return value;
		
		// If the table name should not be adjusted, we'll need to check 
		// if it needs quotes.
		if (neverAdjust && dbConnection != null)
		{
			boolean caseSensitive =  dbConnection.getMetadata().isCaseSensitive();
			boolean defaultCase = dbConnection.getMetadata().isDefaultCase(value);
			if (caseSensitive) return value;
			if (!defaultCase) return dbConnection.getMetadata().quoteObjectname(value);
		}
		
		if (this.identifierCase == CASE_UPPER)
		{
			return value.toUpperCase();
		}
		else if (this.identifierCase == CASE_LOWER)
		{
			return value.toLowerCase();
		}

		return value;
	}

	private String getTableNameToUse()
	{
		String expression = null;
		
		TableIdentifier updateTable = getUpdateTable();
		if (updateTable == null) throw new IllegalArgumentException("Cannot proceed without update table defined");
		
		if (includeTableOwner)
		{
			expression = updateTable.getTableExpression(this.dbConnection);
		}
		else
		{
			expression = updateTable.getTableName();
		}
		
		expression = adjustIdentifierCase(expression);
		return expression;
	}

	private boolean isNull(Object value)
	{
		if (value == null) return true;
		String s = value.toString();
		if (emptyStringIsNull && s.length() == 0) return true;
		return false;
	}
	
}
