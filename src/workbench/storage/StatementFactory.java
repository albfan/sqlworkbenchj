/*
 * StatementFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import workbench.db.DbMetadata;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to generate DELETE, INSERT or UPDATE statements based
 * on the data in a {@link workbench.storage.RowData} object.
 *
 * @author  support@sql-workbench.net
 */
public class StatementFactory
{
	private ResultInfo resultInfo;
	private TableIdentifier tableToUse;
	private boolean includeTableOwner = true;
	private WbConnection dbConnection;
	private boolean emptyStringIsNull = false;
	private boolean includeNullInInsert = true;

	private static final int CASE_NO_CHANGE = 1;
	private static final int CASE_UPPER = 2;
	private static final int CASE_LOWER = 4;
	private int identifierCase = CASE_NO_CHANGE;

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
		ArrayList values = new ArrayList(cols);
		StringBuffer sql = new StringBuffer("UPDATE ");

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
					sql.append(" = ?");
					values.add(new ColumnData(value,this.resultInfo.getColumn(col)));
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
			if (value instanceof NullValue)
			{
				sql.append(" IS NULL");
			}
			else
			{
				sql.append(" = ?");
				values.add(new ColumnData(value,this.resultInfo.getColumn(j)));
			}
		}

		dml = new DmlStatement(sql.toString(), values);
		return dml;
	}

	public DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus)
	{
		return this.createInsertStatement(aRow, ignoreStatus, "\n", null);
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

		ArrayList values = new ArrayList(cols);
		StringBuffer sql = new StringBuffer(250);
    sql.append("INSERT INTO ");
		StringBuffer valuePart = new StringBuffer(250);

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
				valuePart.append('?');

				values.add(new ColumnData(value,this.resultInfo.getColumn(col)));
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

		dml = new DmlStatement(sql.toString(), values);
		return dml;
	}

	public DmlStatement createDeleteStatement(RowData aRow)
	{
		return createDeleteStatement(aRow, false);
	}

	public DmlStatement createDeleteStatement(RowData aRow, boolean ignoreStatus)
	{
		if (aRow == null) return null;
		if (!ignoreStatus && aRow.isNew()) return null;

		boolean first = true;
		DmlStatement dml;
		int count = this.resultInfo.getColumnCount();

		ArrayList values = new ArrayList(count);
		StringBuffer sql = new StringBuffer(250);
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

			Object value = aRow.getOriginalValue(j);
			if (isNull(value)) value = null;
			if (value == null)
			{
				sql.append(" IS NULL");
			}
			else
			{
				sql.append(" = ?");
				values.add(new ColumnData(value, resultInfo.getColumn(j)));
			}
		}

		dml = new DmlStatement(sql.toString(), values);
		return dml;
	}
	
	/**
	 * Setter for property tableToUse.
	 * @param tableToUse New value of property tableToUse.
	 */
	public void setTableToUse(TableIdentifier tableToUse)
	{
		this.tableToUse = tableToUse;
	}

	public void setIncludeTableOwner(boolean flag) { this.includeTableOwner = flag; }
	public boolean getIncludeTableOwner() { return this.includeTableOwner; }

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

	private String adjustIdentifierCase(String value)
	{
		if (value == null) return null;
		if (value.startsWith("\"")) return value;
		
		// If the table name is not in the same case the server stores it
		// and the case may not be changed at all, then we need to quote the table name.
		
		// setNeverAdjustCase() will only be set for TableIdentifiers that have
		// been "retrieved" from the database (e.g. in the DbExplorer)
		// For table names that the user entered, neverAdjustCase() will be false
		TableIdentifier updateTable = this.resultInfo.getUpdateTable();
		
		boolean neverAdjust = (updateTable == null ? false : updateTable.getNeverAdjustCase());
		
		if (neverAdjust && dbConnection != null && !dbConnection.getMetadata().isDefaultCase(value))
		{
			return dbConnection.getMetadata().quoteObjectname(value);
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
		String name = null;
		TableIdentifier updateTable = this.resultInfo.getUpdateTable();
		if (updateTable == null && this.tableToUse == null) throw new IllegalArgumentException("Cannot proceed without update table defined");
		
		if (this.tableToUse != null)
		{
			if (!includeTableOwner)
			{
				name = tableToUse.getTableName();
			}
			else
			{
				name = tableToUse.getTableExpression(this.dbConnection);
			}
		}
		else
		{
			name = (includeTableOwner ? updateTable.getTableExpression(this.dbConnection) : updateTable.getTableName());
		}
		name = adjustIdentifierCase(name);
		return name;
	}

	private boolean isNull(Object value)
	{
		if (value == null) return true;
		if (value instanceof NullValue) return true;
		String s = value.toString();
		if (emptyStringIsNull && s.length() == 0) return true;
		return false;
	}
	
}
