/*
 * TableDeleteSync.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.compare;

import java.io.IOException;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.XmlRowDataConverter;
import workbench.interfaces.ProgressReporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.ColumnData;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;
import workbench.storage.RowDataFactory;
import workbench.storage.SqlLiteralFormatter;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * A class to delete rows in a target table that do not exist in a
 * reference table.
 *
 * The table that should be synchronized needs to exist in both the target and
 * the reference database and it is expected that both tables have the same primary
 * key definition.
 *
 * The presence of the primary keys in the source table are not checked. The column names
 * of the PK of the target table are used to retrieve the data from the source table.
 *
 * To improve performance (a bit), the rows are retrieved in chunks from the
 * target table by dynamically constructing a WHERE clause for the rows
 * that were retrieved from the reference table. The chunk size
 * can be controlled using the property workbench.sql.sync.chunksize
 * The chunk size defaults to 25. This is a conservative setting to avoid
 * problems with long SQL statements when processing tables that have
 * a PK with multiple columns.
 *
 * @author Thomas Kellerer
 */
public class TableDeleteSync
	implements ProgressReporter
{
	private WbConnection toDelete;
	private WbConnection reference;
	private TableIdentifier checkTable;
	private TableIdentifier deleteTable;
	private BatchedStatement deleteStatement;
	private int chunkSize = 50;
	private int batchSize = 50;

	private Statement checkStatement;
	private Map<ColumnIdentifier, Integer> columnMap = new HashMap<ColumnIdentifier, Integer>();
	private RowActionMonitor monitor;
	private Writer outputWriter;
	private String lineEnding = "\n";
	private String encoding;
	private SqlLiteralFormatter formatter;
	private long deletedRows;
	private boolean firstDelete;
	private XmlRowDataConverter xmlConverter;
	private boolean cancelExecution;
	private int progressInterval = 10;

	public TableDeleteSync(WbConnection deleteFrom, WbConnection compareTo)
		throws SQLException
	{
		toDelete = deleteFrom;
		reference = compareTo;
		formatter = new SqlLiteralFormatter(toDelete);
		chunkSize = Settings.getInstance().getSyncChunkSize();
	}

	public void setTypeSql()
	{
		xmlConverter = null;
	}

	public void setTypeXml(boolean useCDATA)
	{
		xmlConverter = new XmlRowDataConverter();
		xmlConverter.setUseDiffFormat(true);
		xmlConverter.setUseVerboseFormat(false);
		xmlConverter.setWriteClobToFile(false);
		xmlConverter.setUseCDATA(useCDATA);
		xmlConverter.setOriginalConnection(toDelete);
		xmlConverter.setWriteBlobToFile(false);
	}

	public void setBatchSize(int size)
	{
		if (size > 0) this.batchSize = size;
	}

	public void setRowMonitor(RowActionMonitor rowMonitor)
	{
		this.monitor = rowMonitor;
	}

	public void cancel()
	{
		this.cancelExecution = true;
	}

	/**
	 * Set a Writer to write the generated statements to.
	 * If an outputwriter is defined, the statements will <b>not</b>
	 * be executed against the target database.
	 *
	 * @param out
	 * @param lineEnd the line ending to be used
	 * @param encoding the encoding used by the write. This is written into the header of the XML output
	 */
	public void setOutputWriter(Writer out, String lineEnd, String encoding)
	{
		this.outputWriter = out;
		if (lineEnd != null)
		{
			lineEnding = lineEnd;
		}
		else
		{
			lineEnding = "\n";
		}
		this.encoding = encoding;
	}

	@Override
	public void setReportInterval(int interval)
	{
		this.progressInterval = interval;
	}

	public long getDeletedRows()
	{
		return this.deletedRows;
	}

	/**
	 * Define the table to be checked.
	 *
	 * @param tableToCheck the table with the "reference" data
	 * @param tableToDelete the table from which obsolete rows should be deleted
	 * @throws java.sql.SQLException
	 */
	public void setTableName(TableIdentifier tableToCheck, TableIdentifier tableToDelete)
		throws SQLException
	{
		if (tableToCheck == null) throw new IllegalArgumentException("Source table may not be null!");
		if (tableToDelete == null) throw new IllegalArgumentException("Target table (for source: " + tableToCheck.getTableName() + ") may not be null!");

		this.checkTable = this.reference.getMetadata().findSelectableObject(tableToCheck);
		this.deleteTable = this.toDelete.getMetadata().findTable(tableToDelete, false);

		if (deleteTable == null) throw new SQLException("Table " + tableToDelete.getTableName() + " not found in target database");
		firstDelete = true;
		this.columnMap.clear();

		List<ColumnIdentifier> columns = this.toDelete.getMetadata().getTableColumns(deleteTable);
		if (columns == null || columns.isEmpty()) throw new SQLException("Table " + deleteTable.getTableName() + " not found in target database");
		String where = " WHERE ";
		int colIndex = 1;

		for (ColumnIdentifier col : columns)
		{
			if (col.isPkColumn())
			{
				if (colIndex > 1)
				{
					where += " AND ";
				}
				String colname = this.toDelete.getMetadata().quoteObjectname(col.getColumnName());
				where += colname + " = ?";
				columnMap.put(col, colIndex);
				colIndex ++;
			}
		}

		if (columnMap.isEmpty())
		{
			throw new SQLException("No primary key found to delete rows from target table " + tableToDelete.getTableName());
		}

		if (outputWriter == null)
		{
			// Directly delete the rows, without an intermediate script file...
			String deleteSql = "DELETE FROM " + this.deleteTable.getTableExpression(this.toDelete) + where;
			PreparedStatement deleteStmt = toDelete.getSqlConnection().prepareStatement(deleteSql);
			this.deleteStatement = new BatchedStatement(deleteStmt, toDelete, batchSize);
			LogMgr.logDebug("SyncDeleter.setTable()", "Using " + deleteSql + " to delete rows from target database");
		}
	}

	public void doSync()
		throws SQLException, IOException
	{
		List<ColumnIdentifier> columns = this.toDelete.getMetadata().getTableColumns(this.deleteTable);
		boolean first = true;
		String selectColumns = "";
		for (ColumnIdentifier col : columns)
		{
			if (col.isPkColumn())
			{
				if (!first)
				{
					selectColumns += ", ";
				}
				else first = false;
				selectColumns += " " + this.toDelete.getMetadata().quoteObjectname(col.getColumnName());
			}
		}
		String retrieve = "SELECT " + selectColumns + " FROM " + this.deleteTable.getTableExpression(this.toDelete);

		LogMgr.logDebug("SyncDeleter.deleteTarget()", "Using " + retrieve + " to retrieve rows from reference database");

		deletedRows = 0;
		cancelExecution = false;

		checkStatement = reference.createStatement();

		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			// Process all rows from the table to be synchronized
			stmt = this.toDelete.createStatementForQuery();
			rs = stmt.executeQuery(retrieve);
			ResultInfo info = new ResultInfo(rs.getMetaData(), this.toDelete);
			if (xmlConverter != null)
			{
				for (int i=0; i < info.getColumnCount(); i++)
				{
					info.getColumn(i).setIsPkColumn(true);
				}
				xmlConverter.setResultInfo(info);
			}

			long rowNumber = 0;
			if (this.monitor != null)
			{
				if (this.outputWriter == null)
				{
					// If output writer is null, we are executing the statements directly.
					this.monitor.setMonitorType(RowActionMonitor.MONITOR_DELETE);
					this.monitor.setCurrentObject(this.deleteTable.getTableName(), -1, -1);
				}
				else
				{
					this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
					String msg = ResourceMgr.getFormattedString("MsgDeleteSyncProcess", this.deleteTable.getTableName());
					this.monitor.setCurrentObject(msg, -1, -1);
				}
			}
			List<RowData> packetRows = new ArrayList<RowData>(chunkSize);

			while (rs.next())
			{
				if (cancelExecution) break;

				rowNumber ++;
				RowData row = RowDataFactory.createRowData(info, toDelete);
				if (this.monitor != null && (rowNumber % progressInterval == 0))
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

			if (packetRows.size() > 0 && !cancelExecution)
			{
				checkRows(packetRows, info);
			}

			if (outputWriter == null)
			{
				this.deletedRows += this.deleteStatement.flush();

				if (!toDelete.getAutoCommit())
				{
					toDelete.commit();
				}
			}
			else
			{
				writeEnd(outputWriter);
			}
		}
		catch (SQLException e)
		{
			if (!toDelete.getAutoCommit())
			{
				try { toDelete.rollback(); } catch (Throwable th) {}
			}
			throw e;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
			if (deleteStatement != null) this.deleteStatement.close();
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
			ResultInfo ri = new ResultInfo(rs.getMetaData(), reference);
			while (rs.next())
			{
				if (cancelExecution) break;
				RowData r = RowDataFactory.createRowData(ri, reference);
				r.read(rs, ri);
				checkRows.add(r);
			}

			// Same number of rows --> no row is missing
			if (checkRows.size() == referenceRows.size()) return;
			if (cancelExecution) return;

			for (RowData doomed : referenceRows)
			{
				if (!checkRows.contains(doomed))
				{
					deleteRow(doomed, ri);
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
	}

	private void deleteRow(RowData row, ResultInfo info)
		throws SQLException
	{
		if (this.outputWriter == null)
		{
			for (int i=0; i < row.getColumnCount(); i++)
			{
				Integer index = this.columnMap.get(info.getColumn(i));
				this.deleteStatement.setObject(index, row.getValue(i));
			}
			long rows = deleteStatement.executeUpdate();
			this.deletedRows += rows;
		}
		else
		{
			try
			{
				if (firstDelete)
				{
					firstDelete = false;
					writeHeader(outputWriter);
				}
				if (xmlConverter == null)
				{
					this.outputWriter.write("DELETE FROM " + deleteTable.getTableName() + " WHERE ") ;
					for (int i=0; i < row.getColumnCount(); i++)
					{
						Object value = row.getValue(i);
						ColumnIdentifier col = info.getColumn(i);
						if (i > 0) this.outputWriter.write(" AND ");
						outputWriter.write(col.getColumnName());
						outputWriter.write(" = ");
						outputWriter.write(formatter.getDefaultLiteral(new ColumnData(value, col)).toString());
					}
					outputWriter.write(';');
				}
				else
				{
					StrBuffer rowData = xmlConverter.convertRowData(row, deletedRows);
					if (rowData != null)
					{
						outputWriter.write("<delete>");
						rowData.writeTo(outputWriter);
						outputWriter.write("</delete>");
					}
				}
				outputWriter.write(lineEnding);
			}
			catch (IOException e)
			{
				LogMgr.logError("TableSync.deleteRow()", "Error writing DELETE statement", e);
			}
		}
	}

	private String buildCheckSql(List<RowData> rows, ResultInfo info)
	{
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT ");
		for (int i=0; i < info.getColumnCount(); i++)
		{
			if (i > 0) sql.append(',');
			sql.append(info.getColumnName(i));
		}
		sql.append(" FROM ");
		sql.append(this.checkTable.getTableExpression(reference));
		sql.append(" WHERE ");

		for (int row=0; row < rows.size(); row++)
		{
			if (row > 0) sql.append (" OR ");
			sql.append('(');
			for (int c=0; c < info.getColumnCount(); c++)
			{
				if (c > 0) sql.append(" AND ");
				sql.append(info.getColumnName(c));
				sql.append(" = ");
				Object value = rows.get(row).getValue(c);
				ColumnIdentifier col = info.getColumn(c);
				ColumnData data = new ColumnData(value,col);
				sql.append(formatter.getDefaultLiteral(data));
			}
			sql.append(") ");
		}
		return sql.toString();
	}

	private void writeEnd(Writer out)
		throws IOException
	{
		if (xmlConverter != null && !firstDelete)
		{
			out.write("</table-data-diff>");
			out.write(lineEnding);
		}
	}

	private void writeHeader(Writer out)
		throws IOException
	{
		String genInfo = "Generated by " + ResourceMgr.TXT_PRODUCT_NAME + " at: " + StringUtil.getCurrentTimestampWithTZString();
		if (xmlConverter != null)
		{
			out.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
			out.write(lineEnding);
			out.write("<!-- ");
			out.write(genInfo);
			out.write(" -->");
			out.write(lineEnding);
			out.write("<table-data-diff name=\"");
			out.write(deleteTable.getTableExpression(toDelete));
			out.write("\">");
			out.write(lineEnding);
		}
		else
		{
			out.write("------------------------------------------------------------------");
			out.write(lineEnding);
			out.write("-- ");
			out.write(genInfo);
			out.write(lineEnding);
			out.write("------------------------------------------------------------------");
			out.write(lineEnding);
		}
	}

}
