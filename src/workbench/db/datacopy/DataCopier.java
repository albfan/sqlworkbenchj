/*
 * DataCopier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.datacopy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableCreator;
import workbench.db.TableDropper;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.DataImporter;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.RowDataReceiver;
import workbench.db.importer.TableStatements;
import workbench.interfaces.BatchCommitter;
import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.ProgressReporter;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.WbThread;

/**
 *
 * @author  support@sql-workbench.net
 */
public class DataCopier
	implements BatchCommitter, ProgressReporter
{
	private WbConnection sourceConnection;
	private WbConnection targetConnection;

	private RowDataProducer sourceData;

	private TableIdentifier sourceTable;
	private TableIdentifier targetTable;

	private DataImporter importer;

	// the columnMap will contain elements of type ColumnIdentifier
	private HashMap<ColumnIdentifier, ColumnIdentifier> columnMap;

	private ColumnIdentifier[] targetColumnsForQuery;
	private MessageBuffer messages = null;
	private MessageBuffer errors = null;
	
	public DataCopier()
	{
		this.importer = new DataImporter();
	}

	public void reset()
	{
		sourceData = null;
		sourceTable = null;
		sourceConnection = null;
		targetTable = null;
		targetColumnsForQuery = null;
		targetTable = null;
		targetConnection = null;
		columnMap = null;
		importer.clearMessages();
		messages = new MessageBuffer();
		errors = new MessageBuffer();
	}

	public void setTransactionControl(boolean flag)
	{
		this.importer.setTransactionControl(flag);
	}
	
	public void beginMultiTableCopy()
	{
		this.importer.beginMultiTable();
	}
	
	public void endMultiTableCopy()
	{
		this.importer.endMultiTable();
	}
	
	public RowDataProducer getSource()
	{
		return this.sourceData;
	}
	
	public RowDataReceiver getReceiver()
	{
		return this.importer;
	}
	
	public void setKeyColumns(List<ColumnIdentifier> keys)
	{
		this.importer.setKeyColumns(keys);
	}

	public void setKeyColumns(String keys)
	{
		this.importer.setKeyColumns(keys);
	}

	public void setPerTableStatements(TableStatements stmt)
	{
		this.importer.setPerTableStatements(stmt);
	}
	
	/**
	 *	Forwards the setMode() call to the DataImporter.
	 *	@see workbench.db.importer.DataImporter#setMode(String)
	 */
	public boolean setMode(String mode)
	{
		return this.importer.setMode(mode);
	}

	public void setReportInterval(int value)
	{
		this.importer.setReportInterval(value);
	}

	public void setProducer(RowDataProducer source, WbConnection target, TableIdentifier targetTbl)
	{
		this.sourceConnection = null;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.targetTable = targetTbl;
		this.targetColumnsForQuery = null;
		this.sourceData = source;
		this.importer.setProducer(source);
	}

	/**
	 *	Define the source table, the target table and the column mapping
	 *	for the copy process.
	 *	If the columnMapping is null, the matching columns from both tables are used.
	 *	It is expected that the mapping contains String objects. The key is the name of the
	 *	source column, the mapped value is the name of the target column
	 */
	public void copyFromTable(WbConnection source, 
														WbConnection target, 
														TableIdentifier aSourceTable, 
														TableIdentifier aTargetTable, 
														Map<String, String> columnMapping, 
														String additionalWhere, 
														boolean createTable, 
														boolean dropTable)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = aSourceTable;
		this.targetTable = aTargetTable;
		this.targetColumnsForQuery = null;

		if (!this.sourceConnection.getMetadata().objectExists(aSourceTable, (String)null))
		{
			this.addError(ResourceMgr.getFormattedString("ErrCopySourceTableNotFound", aSourceTable.getQualifiedName()));
			throw new SQLException("Table " + aTargetTable.getTableName() + " not found in source connection");
		}

		this.initColumnMapping(columnMapping, createTable);
			
		if (createTable) 
		{
			createTable(this.columnMap.values(), dropTable);
		}
		
		this.initImporterForTable(additionalWhere);
	}

	private void createTable(Collection<ColumnIdentifier> columns, boolean dropIfExists)
		throws SQLException
	{
		if (dropIfExists)
		{
			boolean exists = this.targetConnection.getMetadata().tableExists(targetTable);
			if (exists)
			{
				try
				{
					TableDropper dropper = new TableDropper(this.targetConnection);
					dropper.dropTable(targetTable);
					this.addMessage(ResourceMgr.getFormattedString("MsgCopyTableDropped", targetTable.getQualifiedName()));
				}
				catch (SQLException e)
				{
					this.addError(ResourceMgr.getFormattedString("MsgCopyErrorCreatTable",targetTable.getTableExpression(this.targetConnection),ExceptionUtil.getDisplay(e)));
					throw e;
				}
			}
		}

		try
		{
			TableCreator creator = new TableCreator(this.targetConnection, this.targetTable, columns);
			creator.useDbmsDataType(this.sourceConnection.getDatabaseProductName().equals(this.targetConnection.getDatabaseProductName()));
			creator.createTable();

			// no need to delete rows from a newly created table
			this.setDeleteTarget(false);
			this.addMessage(ResourceMgr.getFormattedString("MsgCopyTableCreated", this.targetTable.getTableExpression(this.targetConnection)) + "\n");
		}
		catch (SQLException e)
		{
			//LogMgr.logError("DataCopier.copyFromTable()", "Error when creating target table", e);
			this.addError(ResourceMgr.getFormattedString("MsgCopyErrorCreatTable",targetTable.getTableExpression(this.targetConnection),ExceptionUtil.getDisplay(e)));
			throw e;
		}
	}
		
	/**
	 *	Copy data from a SQL SELECT result to the given target table.
	 */
	public void copyFromQuery(WbConnection source, 
														WbConnection target, 
														String aSourceQuery, 
														TableIdentifier aTargetTable, 
														ColumnIdentifier[] queryColumns,
														boolean createTarget,
														boolean dropTarget)
		throws SQLException
	{
		if (queryColumns == null || queryColumns.length == 0)
		{
			throw new IllegalArgumentException("Source and target column identifiers must be specified when using a SQL query!");
		}
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = null;
		this.targetTable = aTargetTable;
		this.targetColumnsForQuery = queryColumns;

		if (createTarget)
		{
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
			for (ColumnIdentifier col : queryColumns)
			{
				cols.add(col);
			}
			createTable(cols, dropTarget);
		}
		this.initImporterForQuery(aSourceQuery);
	}

	public void setRowActionMonitor(RowActionMonitor rowMonitor)
	{
		if (rowMonitor != null)
		{
			this.importer.setRowActionMonitor(rowMonitor);
		}
	}

	public void setDeleteTarget(boolean delete)
	{
		this.importer.setDeleteTarget(delete);
	}

	public void setContinueOnError(boolean cont)
	{
		this.importer.setContinueOnError(cont);
	}

	public void setUseBatch(boolean flag)
	{
		this.importer.setUseBatch(flag);
	}

	public void setCommitBatch(boolean flag)
	{
		this.importer.setCommitBatch(flag);
	}
	
	public void setBatchSize(int size) 
	{
		this.importer.setBatchSize(size);
	}
	
	public void commitNothing()
	{
		this.importer.commitNothing();
	}
	
	public void setCommitEvery(int interval)
	{
		this.importer.setCommitEvery(interval);
	}

	public void startBackgroundCopy()
	{
		Thread t = new WbThread("DataCopier Thread")
		{
			public void run()
			{
				try
				{
					startCopy();
				}
				catch (Throwable th)
				{
				}
			}
		};
		t.start();
	}

	public long getAffectedRows()
	{
		return this.importer.getAffectedRows();
	}

	public boolean isSuccess()
	{
		return this.importer.isSuccess();
	}

	public String getErrorMessage()
	{
		if (this.errors == null) return null;
		return this.errors.toString();
	}

	public void startCopy()
		throws Exception
	{
		try
		{
			// this will call start() or sourceData.start()
			// depending on which source we set for the importer
			this.sourceData.setAbortOnError(!this.importer.getContinueOnError());
			this.importer.startImport();
			//LogMgr.logDebug("DataCopier.startCopy()", "Copying of data finished. " + this.importer.getInsertedRows() + " total row(s) inserted. " + this.importer.getUpdatedRows() + " total row(s) updated.");
		}
		catch (Exception e)
		{
			LogMgr.logError("DataCopier.start()", "Error when copying data", e);
			this.importer.tableImportError();
			throw e;
		}
	}

	public String getRowsUpdatedMessage()
	{
		String msg = null;
		long rows = this.importer.getUpdatedRows();
		if (rows > 0)
		{
			msg = rows + " " + ResourceMgr.getString("MsgCopyNumRowsUpdated");
		}
		return msg;
	}

	public String getRowsInsertedMessage()
	{
		long rows = this.importer.getInsertedRows();
		String msg = null;

		if (rows > 0)
		{
			msg = rows + " " + ResourceMgr.getString("MsgCopyNumRowsInserted");
		}
		return msg;
	}

	public void cancel()
	{
		this.sourceData.cancel();
	}

	private void initImporterForQuery(String query)
		throws SQLException
	{
		if (this.targetColumnsForQuery == null) return;
		this.importer.setTargetTable(this.targetTable, this.targetColumnsForQuery);
		initQuerySource(query);
	}

	/**
	 *	Send the definition of the target table to the DataImporter, and creates
	 *	the approriate SELECT statement to retrieve the data from the source
	 */
	private void initImporterForTable(String addWhere)
		throws SQLException
	{
		if (this.columnMap == null || this.columnMap.size() == 0)  
		{
			throw new SQLException("No columns defined");
		}
		int count = this.columnMap.size();
		ColumnIdentifier[] cols = new ColumnIdentifier[count];

		int col = 0;

		StringBuilder sql = new StringBuilder(count * 25 + 30);
		sql.append("SELECT ");

		for (Map.Entry<ColumnIdentifier, ColumnIdentifier> entry : this.columnMap.entrySet())
		{
			ColumnIdentifier sid = entry.getKey();
			ColumnIdentifier tid = entry.getValue();
			if (col > 0)
			{
				sql.append(", ");
			}
			sql.append(sid.getColumnName());
			cols[col] = tid;
			col ++;
		}
		
		sql.append(" FROM ");
		sql.append(this.sourceTable.getTableExpression(this.sourceConnection));

		if (addWhere != null && addWhere.trim().length() > 0)
		{
			sql.append(' ');
			String first = SqlUtil.getSqlVerb(addWhere);
			if (!first.equals("WHERE"))
			{
				sql.append(" WHERE ");
			}
			sql.append(addWhere);
		}
		initQuerySource(sql.toString());
		
		try
		{
			this.importer.setTargetTable(this.targetTable, cols);
		}
		catch (SQLException e)
		{
			String msg = ResourceMgr.getFormattedString("ErrCopyTargetTableNotFound", this.targetTable.getTableExpression());
			this.addMessage(msg);
			throw e;
		}
	}

	private void initQuerySource(String sql)
	{
		QueryCopySource source = new QueryCopySource(this.sourceConnection, sql);
		this.sourceData = source;
		this.importer.setProducer(source);
	}

	private List<ColumnIdentifier> getSourceColumns()
		throws SQLException
	{
		List<ColumnIdentifier> sourceCols = null;
		if (this.sourceTable != null)
		{
			DbMetadata sourceMeta = this.sourceConnection.getMetadata();
			TableIdentifier tbl = sourceMeta.resolveSynonym(this.sourceTable);
			sourceCols = sourceMeta.getTableColumns(tbl);
		}
		else if (this.targetColumnsForQuery != null)
		{
			sourceCols = new ArrayList<ColumnIdentifier>(this.targetColumnsForQuery.length);
			for (ColumnIdentifier col : targetColumnsForQuery)
			{
				sourceCols.add(col);
			}
		}
		return sourceCols;
	}
	
	/**
	 *	Initialize the column mapping between source and target table.
	 *	If a mapping is provided, it is used (after checking that the columns
	 *	exist in both tables).
	 *	If no mapping is provided, all matching columns from both tables are copied
	 */
	private void initColumnMapping(Map<String, String> columnMapping, boolean createNew)
		throws SQLException
	{
		List<ColumnIdentifier> sourceCols = getSourceColumns();
		List<ColumnIdentifier> targetCols = (createNew ? null : this.targetConnection.getMetadata().getTableColumns(this.targetTable));

		this.columnMap = new HashMap<ColumnIdentifier, ColumnIdentifier>(sourceCols.size());
		
		if (columnMapping != null)
		{
			int colPos = 0;
			for (Map.Entry<String, String> entry : columnMapping.entrySet())
			{
				ColumnIdentifier scol = new ColumnIdentifier(entry.getKey());
				int index = sourceCols.indexOf(scol);
				if (index > -1)
				{
					ColumnIdentifier sourceCol = sourceCols.get(index);
					ColumnIdentifier targetCol = null;
					
					if (createNew)
					{
						// If no target column specified (e.g. using -columns=col1,col2,col3)
						// then simply use the name from the source table
						targetCol = sourceCol.createCopy();
						if (entry.getValue() != null)
						{
							// Mapping specified, change the name of the column to the specified value
							targetCol.setColumnName(entry.getValue());
						}
						
						// Make sure the order of the tables is preserved
						// when creating the table later, the columns will 
						// be sorted by the position before generating the SQL
						targetCol.setPosition(colPos);
						colPos++;
					}
					else
					{
						// Find the mapped column name in the columns of the target table
						targetCol = new ColumnIdentifier(entry.getValue());
						if (targetCols.indexOf(targetCol) == -1)
						{
							targetCol = null;
							LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + targetCol + " not found in table " + this.targetTable + ". Ignoring mapping!");
							String msg = ResourceMgr.getFormattedString("ErrCopyTargetColumnNotFound", targetCol.getColumnName());
							this.addMessage(msg);
						}
					}
					if (targetCol != null) this.columnMap.put(sourceCol, targetCol);
				}
				else
				{
					LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + scol + " not found in table " + this.sourceTable + ". Ignoring mapping!");
					String msg = ResourceMgr.getFormattedString("ErrCopySourceColumnNotFound", scol.getColumnName());
					this.addMessage(msg);
				}
			}
		}
		else
		{
			// Use all columns from the source table
			for (ColumnIdentifier scol : sourceCols)
			{
				columnMap.put(scol, scol.createCopy());
			}
		}
	}

	private void addError(String msg)
	{
		if (this.errors == null) this.errors = new MessageBuffer();
		if (this.errors.getLength() > 0) this.errors.appendNewLine();
		this.errors.append(msg);
	}


	private void addMessage(String msg)
	{
		if (this.messages == null) this.messages = new MessageBuffer();
		if (this.messages.getLength() > 0) this.messages.appendNewLine();
		this.messages.append(msg);
	}

	public boolean hasWarnings()
	{
		return this.importer.hasWarnings();
	}

	public MessageBuffer getMessageBuffer()
	{
		MessageBuffer buf = new MessageBuffer();
		buf.append(this.messages);
		this.messages.clear();
		importer.copyMessages(buf);
		buf.append(this.errors);
		return buf;
	}
	
	public CharSequence getAllMessages()
	{
		StringBuilder log = new StringBuilder(2000);

		if (this.messages != null) 
		{
			log.append(messages.getBuffer());
			log.append('\n');
		}
		log.append(this.importer.getMessages());
		if (this.errors != null) log.append(this.errors.getBuffer());

		return log;
	}

	public void setErrorHandler(JobErrorHandler handler)
	{
	}
}
