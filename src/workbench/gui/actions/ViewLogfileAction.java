/*
 * ViewLogfileAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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

	public void executeAction(ActionEvent e)
	{
		final WbFile logFile = LogMgr.getLogfile();
		if (logFile == null) return;

		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				if (viewer == null)
				{
					try
					{
						LogFileViewer lview = new LogFileViewer(WbManager.getInstance().getCurrentWindow());
						lview.showFile(logFile);
						viewer = lview;
						viewer.setVisible(true);
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
