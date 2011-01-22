/*
 * DataStoreImporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.gui.dialogs.dataimport.ImportOptions;
import workbench.gui.dialogs.dataimport.TextImportOptions;
import workbench.gui.dialogs.dataimport.XmlImportOptions;
import workbench.interfaces.Interruptable;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;
import workbench.util.ClipboardFile;
import workbench.util.MessageBuffer;

/**
 * A RowDataReceiver to import text files (either from a file or from a String)
 * into a DataStore.
 * @author Thomas Kellerer
 */
public class DataStoreImporter
	implements RowDataReceiver, Interruptable
{
	private DataStore target;
	private RowDataProducer source;
	private RowActionMonitor rowMonitor;
	private JobErrorHandler errorHandler;
	private int currentRowNumber;

	public DataStoreImporter(DataStore data, RowActionMonitor monitor, JobErrorHandler handler)
	{
		this.target = data;
		this.rowMonitor = monitor;
		if (this.rowMonitor != null)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_INSERT);
		}
		this.errorHandler = handler;
	}

	public void startImport()
	{
		try
		{
			this.currentRowNumber = 0;
			this.source.start();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStoreImporter.startImport()", "Error ocurred during import", e);
		}
	}

	public void setTableList(List<TableIdentifier> targetTables)
	{
		// Nothing to do as only one table can be imported
	}

	public void deleteTargetTables()
		throws SQLException
	{
		// Nothing to do as only one table can be imported
	}

	public void beginMultiTable()
		throws SQLException
	{
		// Nothing to do as only one table can be imported
	}

	public void endMultiTable()
	{
		// Nothing to do as only one table can be imported
	}

	public boolean getCreateTarget()
	{
		return false;
	}

	public boolean shouldProcessNextRow()
	{
		return true;
	}

	public void nextRowSkipped()
	{
	}

	public void importString(String contents)
	{
		importString(contents, "\t", "\"");
	}

	public void importString(String contents, String delimiter, String quoteChar)
	{
		ClipboardFile file = new ClipboardFile(contents);
		setImportOptions(file, ProducerFactory.ImportType.Text, new DefaultImportOptions(), new DefaultTextImportOptions(delimiter, quoteChar), null);
	}

	public void importString(String content, ImportOptions options, TextImportOptions textOptions)
	{
		ClipboardFile file = new ClipboardFile(content);
		setImportOptions(file, ProducerFactory.ImportType.Text, options, textOptions, null);
	}

	public void setImportOptions(File file, ProducerFactory.ImportType type, ImportOptions generalOptions, TextImportOptions textOptions, XmlImportOptions xmlOptions)
	{
		ProducerFactory factory = new ProducerFactory(file);
		factory.setTextOptions(textOptions);
		factory.setGeneralOptions(generalOptions);
		factory.setXmlOptions(xmlOptions);
		factory.setType(type);

		this.source = factory.getProducer();

		ResultInfo info = target.getResultInfo();

		try
		{
			List<ColumnIdentifier> targetColumns = Arrays.asList(info.getColumns());
			List<ColumnIdentifier> fileColumns = factory.getFileColumns();
			factory.setColumnMap(fileColumns, targetColumns);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStoreImporter.setImportOptions()", "Error setting import columns", e);
		}
		this.source.setReceiver(this);
		this.source.setAbortOnError(false);
		this.source.setErrorHandler(this.errorHandler);
	}

	public MessageBuffer getMessage()
	{
		return this.source.getMessages();
	}

	public void processRow(Object[] row) throws SQLException
	{
		RowData data = new RowData(row.length);
		for (int i = 0; i < row.length; i++)
		{
			data.setValue(i, row[i]);
		}
		target.addRow(data);
		currentRowNumber ++;
		if (this.rowMonitor != null) this.rowMonitor.setCurrentRow(currentRowNumber, -1);
	}

	public void recordRejected(String record, long importRow, Throwable error)
	{

	}

	public void setTableCount(int total)
	{
	}

	public void setCurrentTable(int current)
	{
	}

	public void setTargetTable(TableIdentifier table, List<ColumnIdentifier> columns)
		throws SQLException
	{
		if (columns.size() != this.target.getColumnCount())
		{
			if (errorHandler != null) errorHandler.fatalError(ResourceMgr.getString("ErrImportInvalidColumnStructure"));
			throw new SQLException("Invalid column count");
		}
	}

	public void tableImportFinished()
		throws SQLException
	{

	}

	public void importFinished()
	{
	}

	public void importCancelled()
	{
	}

	public void tableImportError()
	{
	}

	public void cancelExecution()
	{
		this.source.cancel();
	}

	public boolean confirmCancel()
	{
		return true;
	}

}
