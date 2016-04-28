/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.components;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.AutoCompletionAction;
import workbench.gui.actions.EscAction;
import workbench.gui.sql.*;

import workbench.sql.BatchRunner;
import workbench.sql.ErrorDescriptor;
import workbench.sql.ErrorReportLevel;
import workbench.sql.ExecutionStatus;

import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public abstract class ExecuteSqlPanel
  extends JPanel
  implements ActionListener, WindowListener
{
  private JDialog window;
  private PlainEditor errorDisplay;
  protected EditorPanel sqlEditor;
  protected BatchRunner runner;
  protected ErrorDescriptor lastError;
  private EscAction escAction;
  private JSplitPane splitPane;
  private AutoCompletionAction autoComplete;
  protected int choice = JOptionPane.CANCEL_OPTION;
  private WbStatusLabel statusBar;
  private String sqlToUse;
  private boolean initialized;
  private WbThread runThread;
  private ExecutionStatus execStatus;

  protected ExecuteSqlPanel(WbConnection conn)
  {
    this(conn, null);
  }

  protected ExecuteSqlPanel(WbConnection con, String sql)
  {
    super(new GridBagLayout());
    sqlToUse = sql;
    runner = new BatchRunner();
    runner.setConnection(con);
  }

  protected void showLastError()
  {
    showError(lastError);
  }

  public void showError(ErrorDescriptor descriptor)
  {
    int caret = 0;
    if (descriptor != null)
    {
      errorDisplay.setText(descriptor.getErrorMessage());
      if (descriptor.getErrorPosition() > -1)
      {
        caret = descriptor.getErrorPosition();
      }
    }
    sqlEditor.setCaretPosition(caret);
  }

  protected abstract JPanel getToolPanel();
  protected abstract JLabel getLabel();

  protected void closeDialog(int dialogResult)
  {
    choice = dialogResult;
    closeWindow();
  }

  protected void closeWindow()
  {
    if (window == null) return;
    EventQueue.invokeLater(() ->
    {
        window.setVisible(false);
        window.dispose();
    });
  }

  public int getChoice()
  {
    return choice;
  }

  public ErrorDescriptor getError()
  {
    return lastError;
  }

  protected void initUI()
  {
    if (initialized) return;

    errorDisplay = new PlainEditor(WbSwingUtilities.PROP_ERROR_MSG_WRAP, false, false);
    sqlEditor = EditorPanel.createSqlEditor();
    statusBar = new WbStatusLabel();
    splitPane = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, true, sqlEditor, errorDisplay);

    autoComplete = new AutoCompletionAction(sqlEditor, statusBar);

    sqlEditor.setDatabaseConnection(runner.getConnection());
    autoComplete.setConnection(runner.getConnection());

    if (sqlToUse != null)
    {
      sqlEditor.setText(sqlToUse);
    }

    JLabel lbl = getLabel();
    int borderWidth = 8;

    Font font = null;
    FontMetrics fm = null;
    if (lbl == null)
    {
      font = UIManager.getDefaults().getFont("Label.font");
    }
    else
    {
      font = lbl.getFont();
    }

    fm = statusBar.getFontMetrics(font);
    if (fm != null)
    {
      borderWidth = (int)(fm.getHeight() / 2);
    }

    int gap = IconMgr.getInstance().getSizeForLabel();

    JPanel toolPanel = getToolPanel();

    int vgap = gap / 5;
    GridBagConstraints gc = new GridBagConstraints();

    gc.gridx = 0;
    gc.gridy = 0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.FIRST_LINE_START;
    gc.insets = new Insets(vgap, borderWidth, vgap, borderWidth);
    gc.weightx = 1.0;
    gc.weighty = 0.0;
    if (lbl != null)
    {
      add(lbl, gc);
    }

    gc.gridy ++;
    gc.weightx = 1.0;
    gc.weighty = 0.9;
    gc.insets = new Insets(lbl == null ? borderWidth : 0, borderWidth, vgap, borderWidth);
    gc.fill = GridBagConstraints.BOTH;
    add(splitPane, gc);

    gc.gridy ++;
    gc.weightx = 1.0;
    gc.weighty = 0.0;
    gc.insets = new Insets(0, borderWidth, gap, borderWidth);
    gc.fill = GridBagConstraints.HORIZONTAL;
    add(statusBar, gc);

    gc.gridy ++;
    gc.insets = new Insets(0, borderWidth, gap / 3, borderWidth);
    add(toolPanel, gc);

    initialized = true;
  }

  public boolean isSuccess()
  {
    return execStatus == ExecutionStatus.Success;
  }

  public void dispose()
  {
    if (autoComplete != null) autoComplete.dispose();
    sqlEditor.dispose();
  }

  protected void setButtonsEnabled(boolean flag)
  {
    autoComplete.setEnabled(flag);
  }

  protected void startSQL()
  {
    runThread = new WbThread("SqlExecThread")
    {
      @Override
      public void run()
      {
        runSQL();
      }
    };
    runThread.start();
  }

  protected void runSQL()
  {
    if (runner.getConnection() == null) return;
    if (runner.isBusy()) return;

    try
    {
			runner.setRowMonitor(statusBar.getMonitor());
			runner.setAbortOnError(true);
			runner.setStoreErrors(false);
      runner.setPrintStatements(false);
      runner.setShowProgress(false);
      runner.setShowTiming(false);
      runner.setStoreErrors(false);
      runner.setShowStatementWithResult(false);
      runner.setShowStatementSummary(false);
      runner.setErrorStatementLogging(ErrorReportLevel.none);

			// Make sure nothing is written to system.out
			runner.setResultLogger(ResultLogger.DEV_NULL_LOGGER);
      runner.getConnection().setBusy(true);

      setButtonsEnabled(false);
      statusBar.setStatusMessage(ResourceMgr.getString("MsgExecutingSql"));

      execStatus = runner.runScript(sqlEditor.getText());

      lastError = runner.getLastError();

      if (isSuccess())
      {
        scriptSuccess();
      }
      else
      {
        scriptError();
      }
    }
    catch (Exception ex)
    {
      // this should not happen
      LogMgr.logUserSqlError("ExecuteSqlPanel.runSQL()", "Could not run SQL script", ex);
      WbSwingUtilities.showErrorMessage(ex.getMessage());
    }
    finally
    {
      runner.getConnection().setBusy(false);
      statusBar.clearStatusMessage();
      runner.done();
      setButtonsEnabled(true);
      runThread = null;
    }
  }

  protected void cancel()
  {
    showStatusMessage(ResourceMgr.getString("MsgCancellingStmt"));

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
    showStatusMessage("");
  }

  protected void showStatusMessage(String msg)
  {
    statusBar.setStatusMessage(msg);
  }

  protected void scriptError()
  {
    EventQueue.invokeLater(() ->
    {
      showLastError();
      sqlEditor.requestFocusInWindow();
    });
  }

  protected void scriptSuccess()
  {
    WbSwingUtilities.invoke(this::clearError);
  }

  protected void clearError()
  {
    errorDisplay.setText("");
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == escAction)
    {
      closeDialog(JOptionPane.CANCEL_OPTION);
    }
  }

  @Override
  public void windowOpened(WindowEvent e)
  {
    // setting the divider location only works after the window has been displayed
    splitPane.setDividerLocation(0.8);
  }

  @Override
  public void windowClosing(WindowEvent e)
  {
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


  protected abstract String getWindowSettingsKey();

  protected void restoreSettings()
  {
    if (window != null && !Settings.getInstance().restoreWindowSize(window, getWindowSettingsKey()))
    {
      window.setSize(640, 480);
    }
  }

  protected void saveSettings()
  {
    if (window != null)
    {
      Settings.getInstance().storeWindowSize(window, getWindowSettingsKey());
    }
  }

  public void showDialog(Window owner, String windowTitle, boolean isModal)
  {
    if (isModal)
    {
      window = new JDialog(owner, windowTitle, Dialog.ModalityType.APPLICATION_MODAL);
    }
    else
    {
      window = new JDialog(owner, windowTitle, Dialog.ModalityType.MODELESS);
    }

    initUI();

		// creating the action will add it to the input map of the dialog
		// which will enable the key
		escAction = new EscAction(window, this);

    // the editor is using its own shortcut handling, so the ESC action
    // also needs to be registered there
    sqlEditor.addKeyBinding(escAction);

    window.getContentPane().add(this);
    restoreSettings();

    WbSwingUtilities.center(window, owner);
    WbSwingUtilities.requestComponentFocus(window, sqlEditor);
    window.addWindowListener(this);
    window.setVisible(true);
    saveSettings();
  }
}
