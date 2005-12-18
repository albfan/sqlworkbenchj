/*
 * DataStoreExporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import java.awt.Component;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.WbThread;

/**
 *
 * @author  support@sql-workbench.net
 *
 */
public class DataStoreExporter
{
	private DataStore source;
	private Component caller;
	private ExportFileDialog dialog;
	
	public DataStoreExporter(DataStore source, Component caller)
	{
		this.caller = caller;
		this.source = source;
	}

	public void saveAs()
	{
		boolean insert = (this.source != null && this.source.canSaveAsSqlInsert());
		boolean update = (source != null && source.hasPkColumns());
		this.dialog = new ExportFileDialog(this.caller, source.getResultInfo());
		this.dialog.setIncludeSqlInsert(insert);
		this.dialog.setIncludeSqlUpdate(update);
		this.dialog.setIncludeSqlDeleteInsert(insert && update);
		this.dialog.setSelectDirectoryOnly(false);
		
		boolean selected = dialog.selectOutput();
		if (selected)
		{
			writeFile();
		}
	}
	
	public DataStore getSource()
	{
		return source;
	}

	public void setSource(DataStore source)
	{
		this.source = source;
	}
	
	private void writeFile()
	{
		if (this.source == null) return;
		DataExporter exporter = new DataExporter(this.source.getOriginalConnection());
		exporter.setColumnsToExport(this.dialog.getColumnsToExport());
		dialog.setExporterOptions(exporter);
		try
		{
			exporter.startExport(this.source);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStoreExporter.writeFile()", "Error writing export file", e);
		}
	}
	
}
