/*
 * TableExporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.gui.dbobjects;

import java.awt.Frame;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.ProgressReporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.ExportType;

import workbench.gui.WbSwingUtilities;
import workbench.gui.dialogs.export.ExportFileDialog;

import workbench.storage.RowActionMonitor;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class TableExporter
	implements DbExecutionListener
{
	private DataExporter exporter;
	private ProgressDialog progress;
	private List<TableIdentifier> toExport;
	private String outputDirectory;
	private String extension;

	public TableExporter(WbConnection conn)
	{
		exporter = new DataExporter(conn);
		exporter.setReportInterval(ProgressReporter.DEFAULT_PROGRESS_INTERVAL);
	}

	public DataExporter getExporter()
	{
		return exporter;
	}

	public boolean selectTables(List<? extends DbObject> tables, Frame caller)
	{
		if (CollectionUtil.isEmpty(tables)) return false;

		ExportFileDialog dialog = new ExportFileDialog(caller);
		dialog.setIncludeSqlUpdate(false);
		dialog.setSelectDirectoryOnly(true);
		dialog.restoreSettings();

		String title = ResourceMgr.getString("LblSelectDirTitle");
		WbConnection dbConnection = exporter.getConnection();
		DbMetadata meta = dbConnection.getMetadata();
		boolean answer = dialog.selectOutput(title);
		if (answer)
		{
			this.outputDirectory = dialog.getSelectedFilename();

			dialog.setExporterOptions(exporter);

			ExportType type = dialog.getExportType();
			this.extension = type.getDefaultFileExtension();

			this.toExport = new ArrayList<>(tables.size());

			for (DbObject dbo : tables)
			{
				String ttype = dbo.getObjectType();
				if (ttype == null) continue;
				if (!meta.objectTypeCanContainData(ttype)) continue;
				if (!(dbo instanceof TableIdentifier)) continue;
				this.toExport.add((TableIdentifier)dbo);
			}
		}
		return answer;
	}

	public void startExport(final Frame parent)
	{
    if (toExport == null) return;

		progress = new ProgressDialog(ResourceMgr.getString("MsgSpoolWindowTitle"), parent, exporter);
		exporter.setRowMonitor(progress.getMonitor());
		progress.showProgress();

		progress.getInfoPanel().setMonitorType(RowActionMonitor.MONITOR_PLAIN);
		progress.getInfoPanel().setCurrentObject(ResourceMgr.getString("MsgDiffRetrieveDbInfo"), -1, -1);

		// Creating the tableExportJobs should be done in a background thread
		// as this can potentially take some time (especially with Oracle) as for
		// each table that should be exported, the definition needs to be retrieved.

		WbThread th = new WbThread("Init export")
		{
			@Override
			public void run()
			{
        for (TableIdentifier tbl : toExport)
        {
          String fname = StringUtil.makeFilename(tbl.getObjectName());
          WbFile f = new WbFile(outputDirectory, fname + extension);
          try
          {
            exporter.addTableExportJob(f, tbl);
          }
          catch (SQLException e)
          {
            LogMgr.logError("TableListPanel.exportTables()", "Error adding ExportJob", e);
            WbSwingUtilities.showMessage(parent, e.getMessage());
          }
        }
				progress.getInfoPanel().setMonitorType(RowActionMonitor.MONITOR_EXPORT);
				exporter.addExecutionListener(TableExporter.this);
				exporter.startBackgroundExport();
			}
		};
		th.start();
	}

	@Override
	public void executionStart(WbConnection conn, Object source)
	{
	}

	@Override
	public void executionEnd(WbConnection conn, Object source)
	{
		if (progress != null)
		{
			progress.finished();
			progress.dispose();
		}
		if (exporter != null && !exporter.isSuccess())
		{
			CharSequence msg = exporter.getErrors();
			WbSwingUtilities.showErrorMessage(msg.toString());
		}
	}

}
