/*
 * SchemaReportAction.java
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
package workbench.gui.actions;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.SwingUtilities;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.db.report.SchemaReporter;

import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ProgressDialog;

import workbench.storage.RowActionMonitor;

import workbench.util.ExceptionUtil;
import workbench.util.FileDialogUtil;
import workbench.util.WbThread;
/**
 * @author Thomas Kellerer
 */
public class SchemaReportAction
	extends WbAction
{
	private DbObjectList client;

	public SchemaReportAction(DbObjectList list)
	{
		super();
		initMenuDefinition("MnuTxtSchemaReport");
		client = list;
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		saveReport();
	}

	protected void saveReport()
	{
		if (client == null) return;

		final WbConnection dbConnection = client.getConnection();
		final Component caller = client.getComponent();

		if (!WbSwingUtilities.isConnectionIdle(caller, dbConnection)) return;
		List<? extends DbObject> objects = client.getSelectedObjects();
		if (objects == null) return;

		FileDialogUtil dialog = new FileDialogUtil();

		String filename = dialog.getXmlReportFilename(client.getComponent());
		if (filename == null) return;

		final SchemaReporter reporter = new SchemaReporter(client.getConnection());
		reporter.setObjectList(objects);
		reporter.setOutputFilename(filename);
		reporter.setIncludePartitions(true);

		Frame f = (Frame)SwingUtilities.getWindowAncestor(caller);
		final ProgressDialog progress = new ProgressDialog(ResourceMgr.getString("MsgReportWindowTitle"), f, reporter);
		progress.getInfoPanel().setObject(filename);
		progress.getInfoPanel().setMonitorType(RowActionMonitor.MONITOR_PLAIN);
		reporter.setProgressMonitor(progress.getInfoPanel());
		progress.showProgressWindow();

		Thread t = new WbThread("Schema Report")
		{
			@Override
			public void run()
			{
				try
				{
					dbConnection.setBusy(true);
					reporter.writeXml();
				}
				catch (Throwable e)
				{
					LogMgr.logError("TableListPanel.saveReport()", "Error writing schema report", e);
					final String msg = ExceptionUtil.getDisplay(e);
					EventQueue.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							WbSwingUtilities.showErrorMessage(caller, msg);
						}
					});
				}
				finally
				{
					dbConnection.setBusy(false);
					progress.finished();
				}
			}
		};
		t.start();
	}

}
