/*
 * ViewLogfileAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.LogFileViewer;
import workbench.log.LogMgr;
import workbench.util.ExceptionUtil;
import workbench.util.WbFile;

/**
 * @author Thomas Kellerer
 */
public class ViewLogfileAction
	extends WbAction
{
	private static ViewLogfileAction instance = new ViewLogfileAction();
	private JFrame viewer = null;

	public static ViewLogfileAction getInstance()
	{
		return instance;
	}

	private ViewLogfileAction()
	{
		super();
		this.initMenuDefinition("MnuTxtViewLogfile");
		this.removeIcon();
		WbFile logFile = LogMgr.getLogfile();
		this.setEnabled(logFile != null);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		final WbFile logFile = LogMgr.getLogfile();
		if (logFile == null) return;

		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (viewer == null)
				{
					try
					{
						LogFileViewer lview = new LogFileViewer(WbManager.getInstance().getCurrentWindow());
						viewer = lview;
						viewer.setVisible(true);
						lview.setText("Loading...");
						lview.showFile(logFile);
					}
					catch (Exception e)
					{
						LogMgr.logError("ViewLogFileAction.executeAction()", "Error displaying the log file", e);
						WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
					}
				}
				else
				{
					viewer.setVisible(true);
					viewer.toFront();
				}
			}
		});
	}
}
