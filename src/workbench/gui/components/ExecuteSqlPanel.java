/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

import workbench.log.LogMgr;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.AutoCompletionAction;
import workbench.gui.actions.EscAction;
import workbench.gui.sql.*;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ErrorDescriptor;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.parser.ScriptParser;

import workbench.util.StringUtil;
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
  protected StatementRunner stmtRunner;
  private ErrorDescriptor error;
  private EscAction escAction;
  private JSplitPane splitPane;
  private AutoCompletionAction autoComplete;
  protected int choice = JOptionPane.CANCEL_OPTION;
  private WbStatusLabel statusBar;
  private String sqlToUse;
  private boolean initialized;
  protected WbThread runThread;

  protected ExecuteSqlPanel(WbConnection connection, String sql)
  {
    super(new GridBagLayout());
    stmtRunner = new StatementRunner();
    stmtRunner.setConnection(connection);
    sqlToUse = sql;
  }

  protected ExecuteSqlPanel(ErrorDescriptor errorDescriptor, StatementRunner runner)
  {
    super(new GridBagLayout());
    stmtRunner = runner;
    error = errorDescriptor;
  }

  public void setStatement(ScriptParser parser, int cmdIndex)
  {
    initUI();
    if (parser == null) return;

    String sql = parser.getCommand(cmdIndex);
    // if the alternate delimiter was used when parsing the script
    // we need to add this again otherwise "Retry" might not work
    // if that e.g. is a PL/SQL statement
    DelimiterDefinition delimiterUsed = parser.getDelimiterUsed(cmdIndex);
    if (delimiterUsed.isNonStandard())
    {
      sql += "\n" + delimiterUsed.getDelimiter();
    }
    setStatement(sql);
    showError(error);
  }

  public void setStatement(String sql)
  {
    initUI();
    sqlEditor.setText(sql);
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

  public String getStatement()
  {
    initUI();
    ScriptParser parser = ScriptParser.createScriptParser(stmtRunner.getConnection());
    parser.setScript(sqlEditor.getText());
    if (parser.getSize() < 1)
    {
      return "";
    }
    return parser.getCommand(0);
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

  private void initUI()
  {
    if (initialized) return;

    errorDisplay = new PlainEditor(WbSwingUtilities.PROP_ERROR_MSG_WRAP, false, false);
    sqlEditor = EditorPanel.createSqlEditor();
    if (stmtRunner != null)
    {
      sqlEditor.setDatabaseConnection(stmtRunner.getConnection());
    }

    if (error != null && error.getOriginalStatement() != null)
    {
      sqlEditor.setText(error.getOriginalStatement());
    }
    else if (sqlToUse != null)
    {
      sqlEditor.setText(sqlToUse);
    }

    statusBar = new WbStatusLabel();

    autoComplete = new AutoCompletionAction(sqlEditor, statusBar);
    autoComplete.setConnection(stmtRunner.getConnection());

    splitPane = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, true, sqlEditor, errorDisplay);

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
    final String command = getStatement();
    if (StringUtil.isEmptyString(command))
    {
      LogMgr.logWarning("ExecuteSqlPanel.runSQL()", "No SQL statement!");
      return;
    }

    StatementRunnerResult result = null;
    try
    {
      setButtonsEnabled(false);
      statusBar.setStatusMessage(ResourceMgr.getString("MsgExecutingSql"));

      stmtRunner.getConnection().setBusy(true);
      stmtRunner.runStatement(command);
      result = stmtRunner.getResult();
      final ErrorDescriptor descriptor = result.getErrorDescriptor();

      if (result.isSuccess())
      {
        statementFinished();
      }
      else
      {
        EventQueue.invokeLater(() ->
        {
          showError(descriptor);
          sqlEditor.requestFocusInWindow();
        });
      }
    }
    catch (Exception ex)
    {
      LogMgr.logUserSqlError("ExecuteSqlPanel.runSQL()", command, ex);
      WbSwingUtilities.showErrorMessage(ex.getMessage());
    }
    finally
    {
      result.clear();
      result.clearResultData();
      stmtRunner.getConnection().setBusy(false);
      statusBar.clearStatusMessage();
      stmtRunner.statementDone();
      setButtonsEnabled(true);
      runThread = null;
    }
  }

  protected void showStatusMessage(String msg)
  {
    statusBar.setStatusMessage(msg);
  }

  protected abstract void statementFinished();

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

  public void showDialog(Window owner, String windowTitle)
  {
    window = new JDialog(owner, windowTitle, Dialog.ModalityType.APPLICATION_MODAL);

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
