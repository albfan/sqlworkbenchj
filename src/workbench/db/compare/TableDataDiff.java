/*
 * TableDataDiff.java
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.BlobMode;
import workbench.interfaces.ErrorReporter;
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
import workbench.util.CollectionUtil;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A class to compare the data of two tables and generate approriate INSERT or UPDATE
 * statements in order to sync the tables.
 *
 * The table that should be synchronized needs to exist in both the target and
 * the reference database and it is expected that both tables have the same primary
 * key definition.
 *
 * To improve performance (a bit), the rows are retrieved in chunks from the
 * target table by dynamically constructing a WHERE clause for the rows
 * that were retrieved from the reference table. The chunk size
 * can be controlled using the property workbench.sql.sync.chunksize
 * The chunk size defaults to 25. This is a conservative setting to avoid
 * problems with long SQL statements when processing tables that have
 * a PK with multiple columns.
 *
 * @see workbench.resource.Settings#getSyncChunkSize()
 *
 * @author Thomas Kellerer
 */
public class TableDataDiff
	implements ProgressReporter, ErrorReporter
{
	private WbConnection toSync;
	private WbConnection reference;
	private TableIdentifier referenceTable;
	private TableIdentifier tableToSync;
	private TableDefinition toSyncDef;

	private int chunkSize = 15;

	private Statement checkStatement;
	private RowActionMonitor monitor;
	private Writer updateWriter;
	private Writer insertWriter;

	private boolean firstUpdate;
	private boolean firstInsert;

	private SqlLiteralFormatter formatter;
	private List<ColumnIdentifier> pkColumns;
	private String lineEnding = "\n";
	private String encoding = "UTF-8";

	private boolean cancelExecution;
	private int progressInterval = 10;

	private Set<String> columnsToIgnore;
	private RowDataComparer comparer;

	private MessageBuffer warnings = new MessageBuffer();
	private MessageBuffer errors = new MessageBuffer();
	private long currentRowNumber;

	public TableDataDiff(WbConnection original, WbConnection compareTo)
		throws SQLException
	{
		toSync = compareTo;
		reference = original;
		formatter = new SqlLiteralFormatter(toSync);
		formatter.setDateLiteralType("jdbc");
		chunkSize = Settings.getInstance().getSyncChunkSize();
		comparer = new RowDataComparer();
		comparer.setConnection(toSync);
		comparer.setTypeSql();
	}

	public void setTypeXml(boolean useCDATA)
	{
		comparer.setTypeXml(useCDATA);
	}

	public void setTypeSql()
	{
		comparer.setTypeSql();
	}

	public void setRowMonitor(RowActionMonitor rowMonitor)
	{
		this.monitor = rowMonitor;
	}

	public void addWarning(String msg)
	{
		this.warnings.append(msg);
		this.warnings.appendNewLine();
	}

	public void addError(String msg)
	{
		this.errors.append(msg);
		this.errors.appendNewLine();
	}

	/**
	 * Define how blobs should be handled during export.
	 *
	 * @param type the blob mode to be used.
	 *        null means no special treatment (toString() will be called)
	 */
	public void setBlobMode(String type)
	{
		BlobMode mode = BlobMode.getMode(type);
		if (mode == null)
		{
			String msg = ResourceMgr.getString("ErrExpInvalidBlobType");
			msg = StringUtil.replace(msg, "%paramvalue%", type);
			this.addWarning(msg);
		}
		else
		{
			comparer.setSqlBlobMode(mode);
		}
	}

	/**
	 * Define a list of column names which should not considered when
	 * checking for differences (e.g. a "MODIFIED" column)
	 *
	 * @param columnNames
	 */
	public void setColumnsToIgnore(List<String> columnNames)
	{
		if (columnNames == null)
		{
			this.columnsToIgnore = null;
			return;
		}
		this.columnsToIgnore = CollectionUtil.caseInsensitiveSet();
		this.columnsToIgnore.addAll(columnNames);
	}

	public void setReportInterval(int interval)
	{
		this.progressInterval = interval;
	}

	/**
	 * Define the literal type of the date literals.
	 * This is simply delegated to the instance of the
	 * SqlRowDataConverte} that is used internally.
	 *
	 * @param type
	 */
	public void setSqlDateLiteralType(String type)
	{
		comparer.setSqlDateLiteralType(type);
	}

	public void setBaseDir(WbFile dir)
	{
		comparer.setBaseDir(dir);
	}

	/**
	 * Set the Writers to write the generated UPDATE and INSERT statements.
	 *
	 * @param updates the Writer to write UPDATEs to
	 * @param inserts the Writer to write INSERTs to
	 * @param lineEnd the line end character(s) to be used when writing the text files
	 * @param encoding the encoding used by the writers (this will be written into the XML files)
	 */
	public void setOutputWriters(Writer updates, Writer inserts, String lineEnd, String encoding)
	{
		this.updateWriter = updates;
		this.insertWriter = inserts;
		this.lineEnding = (lineEnd == null ? "\n" : lineEnd);
		this.encoding = encoding;
	}

	/**
	 * Define the tables to be compared.
	 *
	 * @param refTable the table with the "reference" data
	 * @param tableToVerify the table from which obsolete rows should be deleted
	 * @throws java.sql.SQLException if the refTable does not have a primary key
	 * or the tableToVerify is not found
	 */
	public boolean setTableName(TableIdentifier refTable, TableIdentifier tableToVerify)
		throws SQLException
	{
		firstUpdate = true;
		firstInsert = true;
		referenceTable = this.reference.getMetadata().findSelectableObject(refTable);
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

		if (CollectionUtil.isEmpty(pkColumns))
		{
			throw new SQLException("No primary key found for table " + referenceTable);
		}

		tableToSync = this.toSync.getMetadata().findTable(tableToVerify, false);
		if (tableToSync == null)
		{
			throw new SQLException("Target table " + tableToVerify.getTableName() + " not found!");
		}
		else
		{
			toSyncDef = this.toSync.getMetadata().getTableDefinition(tableToSync);
			tableToSync = toSyncDef.getTable();
		}

		boolean columnMatch = true;
		for (ColumnIdentifier col : cols)
		{
			if (findTargetColumn(col) == null)
			{
				columnMatch = false;
				LogMgr.logError("TableDataDiff.setTableName()", "Reference column " + col.getColumnName() + " not found in target table!", null);
			}
		}
		return columnMatch;
	}

	public void cancel()
	{
		this.cancelExecution = true;
	}

	/**
	 * This starts the actual creation of the necessary update and inserts
	 * statements.
	 *
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 */
	public void doSync()
		throws SQLException, IOException
	{
		String retrieve = "SELECT * FROM " + this.referenceTable.getTableExpression(this.reference);

		LogMgr.logDebug("TableDataDiff.doSync()", "Using " + retrieve + " to retrieve rows from reference database");

		checkStatement = toSync.createStatementForQuery();

		cancelExecution = false;

		ResultSet rs = null;
		Statement stmt = null;
		currentRowNumber = 0;
		try
		{
			// Process all rows from the reference table to be synchronized
			stmt = this.reference.createStatementForQuery();
			rs = stmt.executeQuery(retrieve);
			ResultInfo info = new ResultInfo(rs.getMetaData(), this.reference);

			if (this.monitor != null)
			{
				this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
				String msg = ResourceMgr.getFormattedString("MsgDataDiffProcessUpd", this.tableToSync.getTableName());
				this.monitor.setCurrentObject(msg, -1, -1);
			}

			int cols = info.getColumnCount();
			List<RowData> packetRows = new ArrayList<RowData>(chunkSize);

			while (rs.next())
			{
				if (cancelExecution) break;

				RowData row = new RowData(cols);
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

			if (!firstUpdate) writeEnd(updateWriter);
			if (!firstInsert) writeEnd(insertWriter);
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

			if (currentRowNumber == 0) comparer.setResultInfo(ri);

			while (rs.next())
			{
				RowData r = RowDataFactory.createRowData(ri, toSync);
				r.read(rs, ri);
				checkRows.add(r);
				if (cancelExecution) break;
			}

			for (RowData toInsert : referenceRows)
			{
				if (cancelExecution) break;

				int i = findRowByPk(checkRows, info, toInsert, ri);

				currentRowNumber ++;
				if (this.monitor != null && (currentRowNumber % progressInterval == 0))
				{
					monitor.setCurrentRow(currentRowNumber, -1);
				}

				Writer writerToUse = null;
				comparer.setRows(toInsert, i > -1 ? checkRows.get(i) : null);
				comparer.ignoreColumns(columnsToIgnore, ri);

				String output = comparer.getMigration(currentRowNumber);

				if (output != null)
				{
					if (i > -1)
					{
						// Row is present, check for modifications
						if (firstUpdate)
						{
							firstUpdate = false;
							writeHeader(updateWriter);
						}
						writerToUse = updateWriter;
					}
					else
					{
						if (firstInsert)
						{
							firstInsert = false;
							writeHeader(insertWriter);
						}
						writerToUse = insertWriter;
					}

					writerToUse.write(output);
					if (comparer.isTypeXml())
					{
						writerToUse.write(lineEnding);
					}
					else
					{
						writerToUse.write(lineEnding + lineEnding);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("TableDataDiff.checkRows()", "Error when running check SQL " + sql, e);
			throw e;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
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
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT ");
		for (int i=0; i < info.getColumnCount(); i++)
		{
			ColumnIdentifier targetCol = findTargetColumn(info.getColumn(i));
			if (targetCol == null) continue;
			if (i > 0) sql.append(',');
			sql.append(targetCol.getColumnName(toSync));
		}
		sql.append(" FROM ");
		sql.append(this.tableToSync.getTableExpression(toSync));
		sql.append(" WHERE ");

		for (int row=0; row < rows.size(); row++)
		{
			if (row > 0) sql.append (" OR ");
			sql.append('(');
			int pkCount = 0;
			for (int c=0; c < info.getColumnCount(); c++)
			{
				ColumnIdentifier column = info.getColumn(c);
				if (pkColumns.contains(column))
				{
					if (pkCount > 0) sql.append(" AND ");
					ColumnIdentifier targetCol = findTargetColumn(column);
					sql.append(targetCol.getColumnName(toSync));
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

	private ColumnIdentifier findTargetColumn(ColumnIdentifier toFind)
	{
		String cname = StringUtil.trimQuotes(toFind.getColumnName());
		for (ColumnIdentifier col : toSyncDef.getColumns())
		{
			if (StringUtil.trimQuotes(col.getColumnName()).equalsIgnoreCase(cname))
			{
				return col;
			}
		}
		return null;
	}

	private void writeHeader(Writer out)
		throws IOException
	{
		String genInfo = "Generated by " + ResourceMgr.TXT_PRODUCT_NAME + " at: " + StringUtil.getCurrentTimestampWithTZString();
		if (comparer.isTypeXml())
		{
			out.write("<?xml version=\"1.0\" encoding=\"" + this.encoding + "\"?>");
			out.write(lineEnding);
			out.write("<!-- ");
			out.write(genInfo);
			out.write(" -->");
			out.write(lineEnding);
			out.write("<table-data-diff name=\"");
			out.write(tableToSync.getTableExpression(toSync));
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

	private void writeEnd(Writer out)
		throws IOException
	{
		if (comparer.isTypeXml())
		{
			out.write("</table-data-diff>");
			out.write(lineEnding);
		}
	}
}
