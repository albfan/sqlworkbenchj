/*
 * TableDataDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.compare;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.ColumnData;
import workbench.storage.DmlStatement;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;
import workbench.storage.StatementFactory;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to compare the data of two tables and generate approriate INSERT or UPDATE 
 * statements in order to sync the tables.
 * 
 * The table that should be synchronized needs to exist in both the target and 
 * the reference database and it is expected that both tables have the same primary 
 * key definition.
 * 
 * The presence of the primary keys in the source table are not checked. The column names
 * of the PK of the target table are used to retrieve the data from the source table.
 * 
 * @author support@sql-workbench.net
 */
public class TableDataDiff
{
	private WbConnection toSync;
	private WbConnection reference;
	private TableIdentifier referenceTable;
	private TableIdentifier tableToSync;
	private int chunkSize = 15;
	
	private Statement checkStatement;
	private RowActionMonitor monitor;
	private Writer updateWriter;
	private Writer insertWriter;
	
	private boolean firstUpdate;
	private boolean firstInsert;
		
	private SqlLiteralFormatter formatter;
	private List<ColumnIdentifier> pkColumns = null;
	private String lineEnding = "\n";
	
	public TableDataDiff(WbConnection original, WbConnection compareTo)
		throws SQLException
	{
		this.toSync = compareTo;
		this.reference = original;
		formatter = new SqlLiteralFormatter(toSync);
		chunkSize = Settings.getInstance().getSyncChunkSize();
	}
	
	public void setRowMonitor(RowActionMonitor rowMonitor)
	{
		this.monitor = rowMonitor;
	}
	
	public void setSqlDateLiteralType(String type)
	{
		formatter.setDateLiteralType(type);
	}
	
	/**
	 * Set a Writer to write the generated UPDATE statements to. 
	 * 
	 * @param out
	 */
	public void setOutputWriters(Writer updates, Writer inserts, String lineEnd)
	{
		this.updateWriter = updates;
		this.insertWriter = inserts;
		this.lineEnding = (lineEnd == null ? "\n" : lineEnd);
	}


	/**
	 * Define the table to be checked. 
	 * 
	 * @param tableToCheck the table with the "reference" data
	 * @param tableToDelete the table from which obsolete rows should be deleted
	 * @throws java.sql.SQLException
	 */
	public void setTableName(TableIdentifier refTable, TableIdentifier tableToVerify)
		throws SQLException
	{
		firstUpdate = true;
		firstInsert = true;
		this.referenceTable = this.reference.getMetadata().findSelectableObject(refTable);
		if (referenceTable == null)
		{
			throw new SQLException("Reference table " + refTable.getTableName() + " not found!");
		}
		List<ColumnIdentifier> cols = this.reference.getMetadata().getTableColumns(referenceTable);
		this.pkColumns = new ArrayList<ColumnIdentifier>();
		for (ColumnIdentifier col : cols)
		{
			if (col.isPkColumn())
			{
				pkColumns.add(col);
			}
		}
		
		if (pkColumns.size() == 0)
		{
			throw new SQLException("No primary key found for table " + referenceTable);
		}
		
		this.tableToSync = this.toSync.getMetadata().findTable(tableToVerify);
		if (tableToSync == null)
		{
			throw new SQLException("Target table " + tableToVerify.getTableName() + " not found!");
		}
	}

	public void doSync()
		throws SQLException, IOException
	{
		String retrieve = "SELECT * FROM " + this.referenceTable.getTableExpression(this.reference);
		
		LogMgr.logDebug("SyncDeleter.deleteTarget()", "Using " + retrieve + " to retrieve rows from reference database");
	
		checkStatement = toSync.createStatement();
		
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			// Process all rows from the reference table to be synchronized
			stmt = this.reference.createStatementForQuery();
			rs = stmt.executeQuery(retrieve);
			ResultInfo info = new ResultInfo(rs.getMetaData(), this.reference);
			
			long rowNumber = 0;
			if (this.monitor != null)
			{
				this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
				this.monitor.setCurrentObject(this.tableToSync.getTableExpression(this.toSync), -1, -1);
			}
			int cols = info.getColumnCount();
			List<RowData> packetRows = new ArrayList<RowData>(chunkSize);
			
			while (rs.next())
			{
				rowNumber ++;
				RowData row = new RowData(cols);
				if (this.monitor != null)
				{
					monitor.setCurrentRow(rowNumber, -1);
				}
				row.read(rs, info);
				packetRows.add(row);
				
				if (packetRows.size() == chunkSize)
				{
					checkRows(packetRows, info);
					packetRows.clear();
				}
			}
			
			if (packetRows.size() > 0)
			{
				checkRows(packetRows, info);
			}
			
			if (this.monitor != null)
			{
				this.monitor.jobFinished();
			}
			
		}
		finally
		{
			SqlUtil.closeResult(rs);
			SqlUtil.closeStatement(stmt);
			SqlUtil.closeStatement(this.checkStatement);
		}
	}

	private void checkRows(List<RowData> referenceRows, ResultInfo info)
		throws SQLException, IOException
	{
		String sql = buildCheckSql(referenceRows, info);
		ResultSet rs = null;
		try
		{
			rs = checkStatement.executeQuery(sql);
			List<RowData> checkRows = new ArrayList<RowData>(referenceRows.size());
			ResultInfo ri = new ResultInfo(rs.getMetaData(), toSync);
			ri.setPKColumns(this.pkColumns);
			ri.setUpdateTable(this.tableToSync);
			
			StatementFactory factory = new StatementFactory(ri, toSync);
			while (rs.next())
			{
				RowData r = new RowData(ri);
				r.read(rs, ri);
				checkRows.add(r);
			}
			
			for (RowData toInsert : referenceRows)
			{
				int i = findRowByPk(checkRows, info, toInsert, ri);
				RowDataComparer comp;
				if (i > -1)
				{
					comp = new RowDataComparer(toInsert, checkRows.get(i));
				}
				else
				{
					comp = new RowDataComparer(toInsert, null);
				}
				DmlStatement dml = comp.getMigrationSql(factory);
				
				// dml == null means no difference
				if (dml != null)
				{
					Writer writerToUse = null;
					
					String migrateSql = dml.getExecutableStatement(formatter).toString();
					if (migrateSql.startsWith("UPDATE"))
					{
						if (firstUpdate)
						{
							firstUpdate = false;
							writeGenerationInfo(updateWriter);
						}
						writerToUse = updateWriter;
					}
					else
					{
						if (firstInsert)
						{
							firstInsert = false;
							writeGenerationInfo(insertWriter);
						}
						writerToUse = insertWriter;
					}
					writerToUse.write(migrateSql);
					writerToUse.write(";" + lineEnding + lineEnding);
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("TableSync.checkRows()", "Error when running check SQL " + sql, e);
			throw e;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return;
	}

	protected int findRowByPk(List<RowData> reference, ResultInfo refInfo, RowData toFind, ResultInfo findInfo)
	{
		int index = 0;
		for (RowData refRow : reference)
		{
			int equalCount = 0;
			for (ColumnIdentifier col : pkColumns)
			{
				int fIndex = findInfo.findColumn(col.getColumnName());
				int rIndex = refInfo.findColumn(col.getColumnName());
				Object ref = refRow.getValue(rIndex);
				Object find = toFind.getValue(fIndex);
				if (RowData.objectsAreEqual(ref, find)) equalCount ++;
			}
			if (equalCount == pkColumns.size()) return index;
			index ++;
		}
		return -1;
	}
	/**
	 * Creates the Statement to retrieve the corresponding rows of the target
	 * table based on the data retrieved from the reference table
	 * 
	 * @param rows the data from the reference table
	 * @param info the result set definition of the reference table
	 * @return
	 */
	private String buildCheckSql(List<RowData> rows, ResultInfo info)
	{
		StringBuffer sql = new StringBuffer(150);
		sql.append("SELECT ");
		for (int i=0; i < info.getColumnCount(); i++)
		{
			if (i > 0) sql.append(',');
			sql.append(info.getColumnName(i));
		}
		sql.append(" FROM ");
		sql.append(this.tableToSync.getTableExpression(toSync));
		sql.append(" WHERE ");
		
		for (int row=0; row < rows.size(); row++)
		{
			if (row > 0) sql.append (" OR ");
			sql.append("(");
			int pkCount = 0;
			for (int c=0; c < info.getColumnCount(); c++)
			{
				ColumnIdentifier column = info.getColumn(c);
				if (pkColumns.contains(column))
				{
					if (pkCount > 0) sql.append(" AND ");
					sql.append(SqlUtil.quoteObjectname(column.getColumnName()));
					sql.append(" = ");
					Object value = rows.get(row).getValue(c);
					ColumnData data = new ColumnData(value, column);
					sql.append(formatter.getDefaultLiteral(data));
					pkCount++;
				}
			}
			sql.append(") ");
		}
		return sql.toString();
	}
	
	private void writeGenerationInfo(Writer out)
		throws IOException
	{
		out.write("------------------------------------------------------------------");
		out.write(lineEnding);
		out.write("-- Generated by " + ResourceMgr.TXT_PRODUCT_NAME + " at: " + StringUtil.getCurrentTimestampWithTZString());
		out.write(lineEnding);
		out.write("------------------------------------------------------------------");
		out.write(lineEnding);
	}
}
