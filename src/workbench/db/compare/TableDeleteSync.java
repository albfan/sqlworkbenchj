/*
 * TableDeleteSync.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
import java.util.Set;

import workbench.interfaces.ProgressReporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.XmlRowDataConverter;

import workbench.storage.ColumnData;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;
import workbench.storage.RowDataReader;
import workbench.storage.RowDataReaderFactory;
import workbench.storage.SqlLiteralFormatter;
import workbench.util.CollectionUtil;

import workbench.util.SqlUtil;
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
	private WbConnection targetConnection;
	private WbConnection referenceConnection;
	private TableIdentifier referenceTable;
	private TableDefinition tableToDeleteFrom;
	private BatchedStatement deleteStatement;
	private int chunkSize = 50;
	private int batchSize = 50;

	private Statement checkStatement;
	private final Map<ColumnIdentifier, Integer> columnMap = new HashMap<>();
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
	final private Set<String> alternatePKColumns = CollectionUtil.caseInsensitiveSet();;

	public TableDeleteSync(WbConnection deleteFrom, WbConnection compareTo)
		throws SQLException
	{
		targetConnection = deleteFrom;
		referenceConnection = compareTo;
		formatter = new SqlLiteralFormatter(targetConnection);
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
		xmlConverter.setOriginalConnection(targetConnection);
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
	public TableDiffStatus setTableName(TableIdentifier tableToCheck, TableIdentifier tableToDelete)
		throws SQLException
	{
		return setTableName(tableToCheck, tableToDelete, null);
	}

	/**
	 * Define the table to be checked.
	 *
	 * @param tableToCheck the table with the "reference" data
	 * @param tableToDelete the table from which obsolete rows should be deleted
	 * @param alternatePK alternate PK columns to be used
	 * @throws java.sql.SQLException
	 */
	public TableDiffStatus setTableName(TableIdentifier tableToCheck, TableIdentifier tableToDelete, Set<String> alternatePK)
		throws SQLException
	{
		if (tableToCheck == null) throw new IllegalArgumentException("Source table may not be null!");
		if (tableToDelete == null) throw new IllegalArgumentException("Target table (for source: " + tableToCheck.getTableName() + ") may not be null!");

		this.referenceTable = this.referenceConnection.getMetadata().findSelectableObject(tableToCheck);
    TableIdentifier toDelete = this.targetConnection.getMetadata().findTable(tableToDelete);
    this.tableToDeleteFrom = this.targetConnection.getMetadata().getTableDefinition(toDelete, true);

		if (tableToDeleteFrom == null) throw new SQLException("Table " + tableToDelete.getTableName() + " not found in target database");
		firstDelete = true;
		columnMap.clear();
    alternatePKColumns.clear();

    if (alternatePK != null)
    {
      alternatePKColumns.addAll(alternatePK);
    }

		List<ColumnIdentifier> columns = tableToDeleteFrom.getColumns();
		if (columns == null || columns.isEmpty()) throw new SQLException("Table " + tableToDeleteFrom.getTable().getTableName() + " not found in target database");

		String where = " WHERE ";
		int colIndex = 1;

		for (ColumnIdentifier col : columns)
		{
			if (isPkColumn(col))
			{
				if (colIndex > 1)
				{
					where += " AND ";
				}
				String colname = this.targetConnection.getMetadata().quoteObjectname(col.getColumnName());
				where += colname + " = ?";
				columnMap.put(col, colIndex);
				colIndex ++;
			}
		}

		if (columnMap.isEmpty())
		{
			LogMgr.logWarning("TableDeleteSync.setTableName()", "No primary key found to delete rows from target table " + tableToDelete.getTableName(), null);
			return TableDiffStatus.NoPK;
		}

		if (outputWriter == null)
		{
			// Directly delete the rows, without an intermediate script file...
			String deleteSql = "DELETE FROM " + this.tableToDeleteFrom.getTable().getFullyQualifiedName(this.targetConnection) + where;
			PreparedStatement deleteStmt = targetConnection.getSqlConnection().prepareStatement(deleteSql);
			this.deleteStatement = new BatchedStatement(deleteStmt, targetConnection, batchSize);
			LogMgr.logInfo("TableDeleteSync.setTable()", "Using " + deleteSql + " to delete rows from target database");
		}
		return TableDiffStatus.OK;
	}

  private boolean isPkColumn(ColumnIdentifier col)
  {
    if (col == null) return false;
    if (alternatePKColumns.isEmpty()) return col.isPkColumn();
    return alternatePKColumns.contains(col.getColumnName());
  }

	public void doSync()
		throws SQLException, IOException
	{
		List<ColumnIdentifier> columns = this.tableToDeleteFrom.getColumns();
		String selectColumns = "";
    int columnsInSelect = 0;

    for (ColumnIdentifier col : columns)
    {
      if (isPkColumn(col))
      {
        if (columnsInSelect > 0) selectColumns += ", ";
        selectColumns += this.targetConnection.getMetadata().quoteObjectname(col.getColumnName());
        columnsInSelect ++;
      }
    }
    TableIdentifier deleteTable = this.tableToDeleteFrom.getTable();
		String retrieve = "SELECT " + selectColumns + " FROM " + deleteTable.getFullyQualifiedName(targetConnection);

		LogMgr.logInfo("TableDeleteSync.doSync()", "Using " + retrieve + " to retrieve rows from the reference table" );

		deletedRows = 0;
		cancelExecution = false;

		checkStatement = referenceConnection.createStatement();

		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			// Process all rows from the table to be synchronized
			stmt = this.targetConnection.createStatementForQuery();
			rs = stmt.executeQuery(retrieve);
			ResultInfo info = new ResultInfo(rs.getMetaData(), this.targetConnection);
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
					this.monitor.setCurrentObject(deleteTable.getTableName(), -1, -1);
				}
				else
				{
					this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
					String msg = ResourceMgr.getFormattedString("MsgDeleteSyncProcess",deleteTable.getTableName());
					this.monitor.setCurrentObject(msg, -1, -1);
				}
			}
			List<RowData> packetRows = new ArrayList<>(chunkSize);

			RowDataReader reader = RowDataReaderFactory.createReader(info, targetConnection);

			while (rs.next())
			{
				if (cancelExecution) break;

				rowNumber ++;
				if (this.monitor != null && (rowNumber % progressInterval == 0))
				{
					monitor.setCurrentRow(rowNumber, -1);
				}
				RowData row = reader.read(rs, false);
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

				if (!targetConnection.getAutoCommit())
				{
					targetConnection.commit();
				}
			}
			else
			{
				writeEnd(outputWriter);
			}
		}
		catch (SQLException e)
		{
			if (!targetConnection.getAutoCommit())
			{
				try { targetConnection.rollback(); } catch (Throwable th) {}
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
			List<RowData> checkRows = new ArrayList<>(referenceRows.size());
			ResultInfo ri = new ResultInfo(rs.getMetaData(), referenceConnection);
			RowDataReader reader = RowDataReaderFactory.createReader(ri, referenceConnection);
			while (rs.next())
			{
				if (cancelExecution) break;
				RowData r = reader.read(rs, false);
				checkRows.add(r);
			}
			reader.closeStreams();

			// Same number of rows --> no row is missing
			if (checkRows.size() == referenceRows.size()) return;
			if (cancelExecution) return;

			for (RowData doomed : referenceRows)
			{
				if (!checkRows.contains(doomed))
				{
					processRowToDelete(doomed, ri);
				}
        if (cancelExecution) return;
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("TableDeleteSync.checkRows()", "Error when running check SQL " + sql, e);
			throw e;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
	}

  /**
   * This will either delete the row directly in the target database,
   * or write the approriate delete statement to the output file.
   *
   * @param row   the row to be deleted
   * @param info  the result set info from the select statement
   *
   * @throws SQLException
   */
	private void processRowToDelete(RowData row, ResultInfo info)
		throws SQLException
	{
		if (this.outputWriter == null)
		{
      // delete the rows directly
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
					this.outputWriter.write("DELETE FROM " + tableToDeleteFrom.getTable().getTableExpression(targetConnection) + " WHERE ") ;
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
					StringBuilder rowData = xmlConverter.convertRowData(row, deletedRows);
					if (rowData != null)
					{
						outputWriter.write("<delete>");
						outputWriter.write(rowData.toString());
						outputWriter.write("</delete>");
					}
				}
				outputWriter.write(lineEnding);
			}
			catch (IOException e)
			{
				LogMgr.logError("TableDeleteSync.deleteRow()", "Error writing DELETE statement", e);
			}
		}
	}

  /**
   * Create a SELECT statement to retrieve the (potentially) corresponding rows
   * from the reference table based on the passed rows from the target table.
   *
   * @param rows  the rows from the target table
   * @param info  the result set information from the target table
   * @return the SELECT statement to use
   */
	private String buildCheckSql(List<RowData> rows, ResultInfo info)
	{
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT ");
		for (int i=0; i < info.getColumnCount(); i++)
		{
			if (i > 0) sql.append(',');
			sql.append(referenceConnection.getMetadata().quoteObjectname(info.getColumnName(i)));
		}
		sql.append(" FROM ");
		sql.append(this.referenceTable.getTableExpression(referenceConnection));
		sql.append(" WHERE ");

		for (int row=0; row < rows.size(); row++)
		{
			if (row > 0) sql.append (" OR ");
			sql.append('(');
			for (int c=0; c < info.getColumnCount(); c++)
			{
				if (c > 0) sql.append(" AND ");
				sql.append(referenceConnection.getMetadata().quoteObjectname(info.getColumnName(c)));
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
      TableDataDiff.writeTableNameTag(out, "table-data-diff", tableToDeleteFrom.getTable());
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
