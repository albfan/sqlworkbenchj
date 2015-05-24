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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;

import workbench.sql.BatchRunner;
import workbench.sql.ErrorDescriptor;
import workbench.sql.ErrorReportLevel;

import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ScriptExecutionFeedback
  extends JPanel
  implements ActionListener, WindowListener
{
  private WbConnection dbConn;
  private JDialog window;
  private BatchRunner runner;
  private Thread runThread;
  private String sqlScript;
  private EscAction escAction;
  private boolean success;

  public ScriptExecutionFeedback(WbConnection con, String script)
  {
    initComponents();
    editor.setFont(Settings.getInstance().getEditorFont());
    dbConn = con;
    sqlScript = script;
    cancelButton.addActionListener(this);
    closeButton.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == closeButton || e.getSource() == escAction)
    {
      closeWindow();
    }
    else if (e.getSource() == cancelButton)
    {
      cancel();
    }
  }

  public ErrorDescriptor getLastError()
  {
    if (runner == null) return null;
    return runner.getLastError();
  }

  public int getLastErrorIndex()
  {
    if (runner == null) return -1;
    return runner.getLastErrorStatementIndex();
  }

  public void openWindow(Component comp, String title)
  {
    Frame parent = null;
    Window w = SwingUtilities.getWindowAncestor(comp);
    if (w instanceof Frame)
    {
      parent = (Frame)w;
    }
    openWindow(parent, title);
  }

  public void openWindow(final Frame owner, final String title)
  {
    WbSwingUtilities.invoke(new Runnable()
    {
      @Override
      public void run()
      {
        window = new JDialog(owner, title, true);
        ResourceMgr.setWindowIcons(window, "script");
        escAction = new EscAction(window, ScriptExecutionFeedback.this);

        window.getContentPane().add(ScriptExecutionFeedback.this);

        if (!Settings.getInstance().restoreWindowSize(window, "workbench.gui.scriptfeedback.window"))
        {
          window.setSize(640, 480);
        }

        WbSwingUtilities.center(window, owner);
        window.addWindowListener(ScriptExecutionFeedback.this);

        cancelButton.setEnabled(true);
        closeButton.setEnabled(false);
        escAction.setEnabled(false);
        window.setVisible(true);
      }
    });
  }

  protected void closeWindow()
  {
    Settings.getInstance().storeWindowSize(window, "workbench.gui.scriptfeedback.window");
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
    runner.setAbortOnError(true);
    runner.setPrintStatements(false);
    runner.setShowProgress(false);
    runner.setShowTiming(false);
    runner.setStoreErrors(false);
    runner.setShowStatementWithResult(false);
    runner.setShowStatementSummary(false);
    runner.setErrorStatementLogging(ErrorReportLevel.none);

    success = false;

    try
    {
      runner.setAbortOnError(true);
      runner.setStoreErrors(true);

      runner.setResultLogger(new ResultLogger()
      {
        @Override
        public void clearLog()
        {
          editor.setText("");
        }

        @Override
        public void appendToLog(String msg)
        {
          if (StringUtil.isBlank(msg) && StringUtil.isEmptyString(editor.getText())) return;
          editor.append(msg);
        }

        @Override
        public void showLogMessage(String msg)
        {
          editor.setText(msg);
        }
      });

      success = !runner.runScript(sqlScript);
    }
    catch (Exception e)
    {
      LogMgr.logError("RunScriptPanel.runScript()", "Error when running script", e);
      final String error = ExceptionUtil.getDisplay(e);
      WbSwingUtilities.invoke(new Runnable()
      {
        @Override
        public void run()
        {
          editor.append("\n");
          editor.append(error);
        }
      });
    }
    finally
    {
      cancelButton.setEnabled(false);
      escAction.setEnabled(true);
      closeButton.setEnabled(true);
    }
  }

  public boolean isSuccess()
  {
    return success;
  }

  @Override
  public void windowOpened(WindowEvent e)
  {
    startScript();
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

    jScrollPane1 = new JScrollPane();
    editor = new JTextArea();
    cancelButton = new JButton();
    closeButton = new JButton();

    setLayout(new GridBagLayout());

    jScrollPane1.setViewportBorder(BorderFactory.createEtchedBorder());

    editor.setColumns(20);
    editor.setRows(5);
    jScrollPane1.setViewportView(editor);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(5, 5, 5, 5);
    add(jScrollPane1, gridBagConstraints);

    cancelButton.setText(ResourceMgr.getString("LblCancelPlain")); // NOI18N
    cancelButton.setEnabled(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 5, 5, 5);
    add(cancelButton, gridBagConstraints);

    closeButton.setText(ResourceMgr.getString("LblClose")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new Insets(0, 0, 5, 5);
    add(closeButton, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JButton cancelButton;
  private JButton closeButton;
  private JTextArea editor;
  private JScrollPane jScrollPane1;
  // End of variables declaration//GEN-END:variables
}
