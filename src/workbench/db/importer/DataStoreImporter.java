/*
 * DataStoreImporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.gui.dialogs.dataimport.ImportOptions;
import workbench.gui.dialogs.dataimport.TextImportOptions;
import workbench.gui.dialogs.dataimport.XmlImportOptions;
import workbench.interfaces.Interruptable;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;
import workbench.util.ClipboardFile;

/**
 * A RowDataReceiver to import text files (either from a file or from a String)
 * into a DataStore.
 * @author support@sql-workbench.net
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
	
	public void importString(String contents)
	{
		importString(contents, "\t", "\"");
	}
	
	public void importString(String contents, String delimiter, String quoteChar)
	{
		ClipboardFile file = new ClipboardFile(contents);
		setImportOptions(file, ProducerFactory.IMPORT_TEXT, createDefaultImportOptions(), createDefaultTextOptions(delimiter, quoteChar), null);
	}

	protected TextImportOptions createDefaultTextOptions(final String delimiter, final String quoteChar)
	{
		TextImportOptions textOptions = new TextImportOptions()
		{
			public String getTextDelimiter() { return delimiter; }
			public boolean getContainsHeader() {  return true; }
			public String getTextQuoteChar() { return quoteChar;	}
			public boolean getDecode() { return false; }
			public String getDecimalChar() { return Settings.getInstance().getDecimalSymbol();	}

			public void setTextDelimiter(String delim) { 	}
			public void setContainsHeader(boolean flag) { }
			public void setTextQuoteChar(String quote) { }
			public void setDecode(boolean flag) { }
			public void setDecimalChar(String s) { }
		};
		return textOptions;
	}
	
	protected ImportOptions createDefaultImportOptions()
	{
		ImportOptions options = new ImportOptions()
		{
			public String getEncoding() {  return "UTF-8"; }
			public String getDateFormat() {	return Settings.getInstance().getDefaultDateFormat(); }
			public String getTimestampFormat() { return Settings.getInstance().getDefaultTimestampFormat(); }

			public void setEncoding(String enc) {	}
			public void setDateFormat(String format) { }
			public void setTimestampFormat(String format) {	}
			public void setMode(String mode) { }
			public String getMode() { return "insert"; }
		};
		
		return options;
	}
	
	public void importString(String content, ImportOptions options, TextImportOptions textOptions)
	{
		ClipboardFile file = new ClipboardFile(content);
		setImportOptions(file, ProducerFactory.IMPORT_TEXT, options, textOptions, null);
	}
	
	public void setImportOptions(File file, int type, ImportOptions generalOptions, TextImportOptions textOptions, XmlImportOptions xmlOptions)
	{
		ProducerFactory factory = new ProducerFactory(file);
		factory.setTextOptions(textOptions);
		factory.setGeneralOptions(generalOptions);
		factory.setXmlOptions(xmlOptions);
		factory.setType(type);
		ResultInfo info = this.target.getResultInfo();
		
		List cols = new ArrayList(info.getColumnCount());
		for (int i = 0; i < info.getColumnCount(); i++)
		{
			cols.add(info.getColumn(i));
		}
		try 
		{
			factory.setImportColumns(cols);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStoreImporter.setImportOptions()", "Error setting import columns", e);
		}
		this.source = factory.getProducer();
		this.source.setReceiver(this);
		this.source.setAbortOnError(false);
		this.source.setErrorHandler(this.errorHandler);
	}
	
	public String getMessage()
	{
		return this.source.getMessages();
	}
	
	public void processRow(Object[] row) throws SQLException
	{
		RowData data = new RowData(row.length);
		for (int i = 0; i < row.length; i++)
		{
			if (row[i] == null)
			{
				data.setNull(i,this.target.getColumnType(i));
			}
			else
			{
				data.setValue(i, row[i]);
			}
		}
		target.addRow(data);
		currentRowNumber ++;
		if (this.rowMonitor != null) this.rowMonitor.setCurrentRow(currentRowNumber, -1);		
	}
	
	public void setTableCount(int total)
	{
	}
	
	public void setCurrentTable(int current)
	{
	}
	
	public void setTargetTable(String tableName, ColumnIdentifier[] columns) 
		throws SQLException
	{
		if (columns.length != this.target.getColumnCount())
		{
			if (errorHandler != null) errorHandler.fatalError(ResourceMgr.getString("ErrImportInvalidColumnStructure"));
			throw new SQLException("Invalid column count");
		}
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
