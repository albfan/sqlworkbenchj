/*
 * StatementFactory.java
 *
 * Created on September 10, 2004, 10:02 PM
 */

package workbench.storage;

import java.util.ArrayList;
import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
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
		return this.createUpdateStatement(aRow, false, StringUtil.LINE_TERMINATOR);
	}

	public DmlStatement createUpdateStatement(RowData aRow, boolean ignoreStatus)
	{
		return this.createUpdateStatement(aRow, ignoreStatus, StringUtil.LINE_TERMINATOR);
	}
	
	public DmlStatement createUpdateStatement(RowData aRow, boolean ignoreStatus, String lineEnd)
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
					if (this.resultInfo.getColumnType(col) == SqlUtil.LONG_TYPE)
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
		return this.createInsertStatement(aRow, ignoreStatus, StringUtil.LINE_TERMINATOR);
	}

	/**
	 *	Generate an insert statement for the given row
	 *	When creating a script for the DataStore the ignoreStatus
	 *	will be passed as true, thus ignoring the row status and
	 *	some basic formatting will be applied to the SQL Statement
	 */
	public DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus, String lineEnd)
	{
		boolean first = true;
		DmlStatement dml;

		if (!ignoreStatus && !aRow.isModified()) return null;
		
		int cols = this.resultInfo.getColumnCount();
		boolean newLineAfterColumn = (cols > 5);

		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer(250);
    sql.append("INSERT INTO ");
		StringBuffer valuePart = new StringBuffer(250);

		sql.append(getTableNameToUse());
		if (ignoreStatus) sql.append(lineEnd);
		sql.append('(');
		if (newLineAfterColumn)
		{
			sql.append(lineEnd);
			sql.append("  ");
			valuePart.append(lineEnd);
			valuePart.append("  ");
		}

		first = true;
    String colName = null;
		int includedColumns = 0;

		for (int col=0; col < cols; col ++)
		{
			if (ignoreStatus || aRow.isColumnModified(col))
			{
				if (first)
				{
					first = false;
				}
				else
				{
					if (newLineAfterColumn)
					{
						sql.append("  , ");
						valuePart.append("  , ");
					}
					else
					{
						sql.append(',');
						valuePart.append(',');
					}
				}

				colName = SqlUtil.quoteObjectname(this.resultInfo.getColumnName(col));
				sql.append(colName);
				valuePart.append('?');

				if (ignoreStatus && newLineAfterColumn)
				{
					sql.append(lineEnd);
					valuePart.append(lineEnd);
				}
				values.add(aRow.getValue(col));
			}
		}
		sql.append(')');
		if (ignoreStatus)
		{
			sql.append(lineEnd);
			sql.append("VALUES");
			sql.append(lineEnd);
			sql.append('(');
		}
		else
		{
			sql.append(" VALUES (");
		}
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
