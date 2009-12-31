/*
 * DataStoreExporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import java.awt.Component;
import workbench.db.exporter.DataExporter;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.WbFile;

/**
 * @author  Thomas Kellerer
 */
public class DataStoreExporter
{
	private DataStore source;
	private Component caller;
	private ExportFileDialog dialog;
	private WbFile output;
	
	public DataStoreExporter(DataStore source, Component caller)
	{
		this.caller = caller;
		this.source = source;
	}

	public void saveAs()
	{
		this.dialog = new ExportFileDialog(this.caller, source);
		this.dialog.setSelectDirectoryOnly(false);
		this.output = null;
		boolean selected = dialog.selectOutput();
		if (selected)
		{
			this.output = new WbFile(dialog.getSelectedFilename());
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
		if (this.output == null)
		{
			throw new NullPointerException("No outputfile defined");
		}
		DataExporter exporter = new DataExporter(this.source.getOriginalConnection());
		exporter.setColumnsToExport(this.dialog.getColumnsToExport());
		dialog.setExporterOptions(exporter);
		
		try
		{
			exporter.startExport(output, this.source);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStoreExporter.writeFile()", "Error writing export file", e);
		}
	}
	
}
