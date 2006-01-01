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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.ErrorHandler;
import workbench.db.ColumnIdentifier;
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
import workbench.util.ValueConverter;
/**
 * A RowDataReceiver
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
	
	public void setImportOptions(String file, int type, ImportOptions generalOptions, TextImportOptions textOptions, XmlImportOptions xmlOptions)
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
			data.setValue(i, row[i]);
		}
		target.addRow(data);
		currentRowNumber ++;
		this.rowMonitor.setCurrentRow(currentRowNumber, -1);		
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
			errorHandler.fatalError(ResourceMgr.getString("ErrorImportInvalidColumnStructure"));
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
