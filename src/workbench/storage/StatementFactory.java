/*
 * StatementFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.storage;

import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;

import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 *
 * @author  info@sql-workbench.net
 */
public class StatementFactory
{
	private ResultInfo resultInfo;
	private String tableToUse;

	public StatementFactory(ResultInfo metaData)
	{
		this.resultInfo = metaData;
	}

	public DmlStatement createUpdateStatement(RowData aRow)
	{
		return this.createUpdateStatement(aRow, false, "\n");
	}

	public DmlStatement createUpdateStatement(RowData aRow, boolean ignoreStatus)
	{
		return this.createUpdateStatement(aRow, ignoreStatus, "\n");
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
		
		boolean newLineAfterColumn = (cols > 5);

		DmlStatement dml;

		if (!ignoreStatus && !aRow.isModified()) return null;
		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer("UPDATE ");

		sql.append(getTableNameToUse());
		sql.append("\n   SET ");
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
				String colName = SqlUtil.quoteObjectname(this.resultInfo.getColumnName(col));
				sql.append(colName);
				Object value = aRow.getValue(col);
				if (value instanceof NullValue)
				{
					sql.append(" = NULL");
				}
				else
				{
					sql.append(" = ?");
					if ("LONG".equals(this.resultInfo.getDbmsTypeName(col)))
					{
						values.add(new OracleLongType(value.toString()));
					}
					else
					{
						values.add(value);
					}
				}
			}
		}
		sql.append("\n WHERE ");
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
			sql.append(SqlUtil.quoteObjectname(this.resultInfo.getColumnName(j)));
			Object value = aRow.getOriginalValue(j);
			if (value instanceof NullValue)
			{
				sql.append(" IS NULL");
			}
			else
			{
				sql.append(" = ?");
				values.add(value);
			}
		}
		try
		{
			dml = new DmlStatement(sql.toString(), values);
		}
		catch (Exception e)
		{
			dml = null;
			LogMgr.logError(this, "Error creating DmlStatement for " + sql.toString(), e);
		}
		return dml;
	}

	public DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus)
	{
		return this.createInsertStatement(aRow, ignoreStatus, "\n", null);
	}

	public DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus, String lineEnd)
	{
		return this.createInsertStatement(aRow, ignoreStatus, lineEnd);
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
		
		int columnThresholdForNewline = Settings.getInstance().getIntProperty("workbench.sql.generate.insert.newlinethreshold",5);
		boolean newLineAfterColumn = (cols > columnThresholdForNewline);
		
		int colsPerLine = Settings.getInstance().getIntProperty("workbench.sql.generate.insert.colsperline",1);
		
		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer(250);
    sql.append("INSERT INTO ");
		StringBuffer valuePart = new StringBuffer(250);

		sql.append(getTableNameToUse());
		sql.append(lineEnd);
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
		int includedColumns = 0;
		int colsInThisLine = 0;
		
		for (int col=0; col < cols; col ++)
		{
			if (columns != null)
			{
				if (!columns.contains(this.resultInfo.getColumn(col))) continue;
			}
			if (ignoreStatus || aRow.isColumnModified(col))
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
							sql.append(',');
							valuePart.append(',');
							sql.append(lineEnd);
							valuePart.append(lineEnd);
						}
						colsInThisLine = 0;
					}
					else
					{
						sql.append(',');
						valuePart.append(',');
					}
				}
				else
				{
					first = false;
				}

				colName = SqlUtil.quoteObjectname(this.resultInfo.getColumnName(col));
				sql.append(colName);
				valuePart.append('?');

				values.add(aRow.getValue(col));
			}
			colsInThisLine ++;
		}
		if (newLineAfterColumn)
		{
			sql.append(lineEnd);
			valuePart.append(lineEnd);
		}

		sql.append(')');
		sql.append(lineEnd);
		sql.append("VALUES");
		sql.append(lineEnd);
		sql.append('(');
		sql.append(valuePart);
		sql.append(')');
		try
		{
			dml = new DmlStatement(sql.toString(), values);
		}
		catch (Exception e)
		{
			dml = null;
			LogMgr.logError(this, "Error creating DmlStatement for " + sql.toString(), e);
		}
		return dml;
	}

	public DmlStatement createDeleteStatement(RowData aRow)
	{
		if (aRow == null) return null;
		if (aRow.isNew()) return null;

		boolean first = true;
		DmlStatement dml;

		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer(250);
    sql.append("DELETE FROM ");
		sql.append(getTableNameToUse());
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
			String colName = SqlUtil.quoteObjectname(this.resultInfo.getColumnName(j));
			sql.append(colName);

			Object value = aRow.getOriginalValue(j);
			if (value instanceof NullValue)
			{
				sql.append(" IS NULL");
			}
			else
			{
				sql.append(" = ?");
				values.add(value);
			}
		}
		try
		{
			dml = new DmlStatement(sql.toString(), values);
		}
		catch (Exception e)
		{
			dml = null;
			LogMgr.logError(this, "Error creating DELETE Statement for " + sql.toString(), e);
		}
		return dml;
	}

	private String getTableNameToUse()
	{
		String name = null;
		TableIdentifier updateTable = this.resultInfo.getUpdateTable();
		if (this.tableToUse != null || updateTable == null )
		{
			name = this.tableToUse;
		}
		else
		{
			name = updateTable.getTableExpression();
		}
		return SqlUtil.quoteObjectname(name);
	}

	/**
	 * Getter for property tableToUse.
	 * @return Value of property tableToUse.
	 */
	public java.lang.String getTableToUse()
	{
		return tableToUse;
	}

	/**
	 * Setter for property tableToUse.
	 * @param tableToUse New value of property tableToUse.
	 */
	public void setTableToUse(java.lang.String tableToUse)
	{
		this.tableToUse = tableToUse;
	}

}
