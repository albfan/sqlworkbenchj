/*
 * DataStoreExporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
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
 * @author  info@sql-workbench.net
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
		boolean sql = (this.source != null && this.source.canSaveAsSqlInsert());
		boolean update = (source != null && source.hasPkColumns());
		this.dialog = new ExportFileDialog(this.caller);
		this.dialog.setIncludeSqlInsert(sql);
		this.dialog.setIncludeSqlUpdate(update);
		this.dialog.setSelectDirectoryOnly(false);
		
		boolean selected = dialog.selectOutput();
		if (selected)
		{
			writeFile();
			/*
			Thread t = new WbThread("Export Thread")
			{
				public void run()
				{
					writeFile();
				}
			};
			t.start();
			*/
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
		int type = dialog.getExportType();
		DataExporter exporter = new DataExporter();
		exporter.setConnection(this.source.getOriginalConnection());
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
