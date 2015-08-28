/*
 * RunScriptPanel.java
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbStatusLabel;
import workbench.gui.sql.SqlEditor;

import workbench.sql.BatchRunner;

import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class RunScriptPanel
	extends JPanel
	implements ActionListener, WindowListener
{
	private WbConnection dbConn;
	private JDialog window;
	private BatchRunner runner;
	private Thread runThread;
	private String sqlScript;
	private EscAction escAction;
	private boolean wasRun;
	private boolean success;

	public RunScriptPanel(WbConnection con, String script)
	{
		initComponents();
		statusbar.setText("");
		dbConn = con;
		editor.setDatabaseConnection(con);
		sqlScript = script;
		startButton.addActionListener(this);
		cancelButton.addActionListener(this);
		closeButton.addActionListener(this);
		saveAsButton.setAction(editor.getFileSaveAsAction());
		saveAsButton.setEnabled(true);
	}

	public boolean wasRun()
	{
		return wasRun;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == startButton)
		{
			startScript();
		}
		else if (e.getSource() == closeButton || e.getSource() == escAction)
		{
			closeWindow();
		}
		else if (e.getSource() == cancelButton)
		{
			cancel();
		}
	}

	public void openWindow(Component comp, String title)
	{
		openWindow(comp, title, null);
	}

	public void openWindow(Component comp, String title, String highlight)
	{
		Frame parent = null;
		Window w = SwingUtilities.getWindowAncestor(comp);
		if (w instanceof Frame)
		{
			parent = (Frame) w;
		}
		openWindow(parent, title, highlight);
	}

	public void openWindow(final Frame owner, final String title, final String highlight)
	{
		WbSwingUtilities.invoke(() ->
    {
      window = new JDialog(owner, title, true);
      ResourceMgr.setWindowIcons(window, "script");
      escAction = new EscAction(window, RunScriptPanel.this);

      window.getContentPane().add(RunScriptPanel.this);
      if (!Settings.getInstance().restoreWindowSize(window, "workbench.gui.runscript.window"))
      {
        window.setSize(600, 400);
      }
      WbSwingUtilities.center(window, owner);
      window.addWindowListener(RunScriptPanel.this);
      editor.setText(sqlScript);
      editor.setCaretPosition(0);

      if (highlight != null)
      {
        EventQueue.invokeLater(() ->
        {
          highlightText(highlight);
        });
      }
      window.setVisible(true);
    });
	}

	private void highlightText(String text)
	{
		if (StringUtil.isBlank(text)) return;
		int start = editor.getReplacer().findFirst(text, true, true, false);
		int end = start + text.length();
		if (start > -1)
		{
			editor.select(start, end);
		}
		editor.requestFocusInWindow();
	}

	protected void closeWindow()
	{
		if (runner != null) return;
		Settings.getInstance().storeWindowSize(window, "workbench.gui.runscript.window");
		window.setVisible(false);
		window.dispose();
	}

	protected void cancel()
	{
		if (runner != null)
		{
			runner.cancel();
		}
		try
		{
			if (runThread != null)
			{
				runThread.interrupt();
			}
			runThread = null;
		}
		catch (Exception e)
		{
			// ignore
		}
	}

	protected void startScript()
	{
		if (runner != null) return;

		runThread = new WbThread("RunScript")
		{
			@Override
			public void run()
			{
				runScript();
			}
		};
		runThread.start();
	}

	protected void runScript()
	{
		if (dbConn == null) return;
		if (dbConn.isBusy()) return;

		runner = new BatchRunner();
		runner.setConnection(dbConn);
		success = false;

		try
		{
			runner.setRowMonitor(((WbStatusLabel)statusbar).getMonitor());
			runner.setAbortOnError(true);
			runner.setStoreErrors(true);

			// Make sure nothing is written to system.out
			runner.setResultLogger(new ResultLogger() {
				@Override
				public void clearLog()
				{
				}

				@Override
				public void appendToLog(String msg)
				{
				}

				@Override
				public void showLogMessage(String msg)
				{
				}
			});
			startButton.setEnabled(false);
			cancelButton.setEnabled(true);
			closeButton.setEnabled(false);

			success = !runner.runScript(editor.getText());

			final String statusMsg;
			if (success)
			{
				statusMsg = ResourceMgr.getString("TxtScriptFinished");
			}
			else
			{
				statusMsg = ResourceMgr.getString("MsgBatchStatementError");
			}

			WbSwingUtilities.invoke(() ->
      {
        statusbar.setText(statusMsg);
      });

			if (!success)
			{
				String errors = runner.getMessages();
				if (errors != null)
				{
					WbSwingUtilities.showMultiLineError(this, ResourceMgr.TXT_PRODUCT_NAME, errors);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("RunScriptPanel.runScript()", "Error when running script", e);
			final String error = ExceptionUtil.getDisplay(e);
			WbSwingUtilities.invoke(() ->
      {
        statusbar.setText(error);
      });
			WbSwingUtilities.showMessage(this, error);
		}
		finally
		{
			wasRun = true;
			startButton.setEnabled(true);
			cancelButton.setEnabled(false);
			closeButton.setEnabled(true);
			runner = null;
		}
	}

	public boolean isSuccess()
	{
		return success;
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
		editor.invalidate();
		editor.validate();
		editor.doLayout();
		editor.requestFocusInWindow();
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		closeWindow();
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


	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    GridBagConstraints gridBagConstraints;

    editor = new SqlEditor();
    startButton = new JButton();
    cancelButton = new JButton();
    closeButton = new JButton();
    statusbar = new WbStatusLabel();
    saveAsButton = new JButton();

    setLayout(new GridBagLayout());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(6, 5, 10, 5);
    add(editor, gridBagConstraints);

    startButton.setText(ResourceMgr.getString("LblStartSql")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 6, 5, 0);
    add(startButton, gridBagConstraints);

    cancelButton.setText(ResourceMgr.getString("LblCancelPlain")); // NOI18N
    cancelButton.setEnabled(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 5, 5, 5);
    add(cancelButton, gridBagConstraints);

    closeButton.setText(ResourceMgr.getString("LblClose")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new Insets(0, 0, 5, 5);
    add(closeButton, gridBagConstraints);

    statusbar.setText("XXXX");
    statusbar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(1, 1, 1, 1)));
    statusbar.setPreferredSize(new Dimension(30, 24));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 6, 5, 6);
    add(statusbar, gridBagConstraints);

    saveAsButton.setText(ResourceMgr.getString("MnuTxtFileSave")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 6, 5, 0);
    add(saveAsButton, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JButton cancelButton;
  private JButton closeButton;
  private SqlEditor editor;
  private JButton saveAsButton;
  private JButton startButton;
  private JLabel statusbar;
  // End of variables declaration//GEN-END:variables
}
