/*
 * ProgressDialog.java
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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;

import workbench.interfaces.Interruptable;

import workbench.gui.WbSwingUtilities;

import workbench.storage.RowActionMonitor;

import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ProgressDialog
	extends JDialog
	implements WindowListener
{
	private ProgressPanel progressPanel;
	private Interruptable worker;
	private final Frame parentWindow;

	/**
	 *
	 * @param parent the window acting as the parent for the progress monitor
	 * @param workerThread
	 */
	public ProgressDialog(String title, Frame parent, final Interruptable workerThread)
  {
    this(title, parent, workerThread, true);
  }

	public ProgressDialog(String title, Frame parent, final Interruptable workerThread, boolean showFilename)
	{
		super(parent, true);
		setTitle(title);
		worker = workerThread;
		parentWindow = parent;
		progressPanel = new ProgressPanel(worker, showFilename);
		progressPanel.setParentDialog(this);
		getContentPane().add(progressPanel);
		pack();
		addWindowListener(this);
	}


	public ProgressPanel getInfoPanel()
	{
		return progressPanel;
	}

	public void finished()
	{
		removeWindowListener(this);
		setVisible(false);
	}

	public RowActionMonitor getMonitor()
	{
		return progressPanel;
	}

	/**
	 * Open the progress monitor window.
	 */
	public void showProgress()
	{
		WbThread t = new WbThread("ShowProgress")
		{
			@Override
			public void run()
			{
				WbSwingUtilities.invoke(new Runnable()
				{
					@Override
					public void run()
					{
						WbSwingUtilities.center(ProgressDialog.this, parentWindow);
						setVisible(true);
					}
				});
			}
		};
		t.start();
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		worker.cancelExecution();
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

}
