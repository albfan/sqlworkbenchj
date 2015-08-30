/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import workbench.log.LogMgr;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.AutoCompletionAction;
import workbench.gui.actions.EscAction;
import workbench.gui.components.PlainEditor;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbCheckBox;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbStatusLabel;

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
public class ErrorRetryPanel
  extends JPanel
  implements ActionListener, WindowListener
{
  public static final String PROP_REPLACE_ERROR_STATEMENT = "workbench.gui.retry_error.replace.statement";

  private JDialog window;
  private PlainEditor errorDisplay;
  private EditorPanel sqlEditor;
  private StatementRunner stmtRunner;
  private ErrorDescriptor error;
  private JButton retryButton;
  private JButton cancelButton;
  private JButton ignoreOneButton;
  private JButton ignoreAllButton;
  private EscAction escAction;
  private JSplitPane splitPane;
  private AutoCompletionAction autoComplete;
  private int choice = JOptionPane.NO_OPTION;
  private WbStatusLabel statusBar;
  private JCheckBox replaceStatement;
  private boolean enableReplace = true;

  public ErrorRetryPanel(ErrorDescriptor errorDescriptor, StatementRunner runner)
  {
    super(new BorderLayout(5,0));
    stmtRunner = runner;
    error = errorDescriptor;
  }

  public void setEnableReplace(boolean flag)
  {
    enableReplace = flag;
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

    sqlEditor.setText(sql);
    setCaretToError();
  }

  private void setCaretToError()
  {
    int caret = 0;
    if (error != null && error.getErrorPosition() > -1)
    {
      caret = error.getErrorPosition();
    }
    sqlEditor.setCaretPosition(caret);
  }

  public int getChoice()
  {
    return choice;
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

  private void initUI()
  {
    if (errorDisplay != null) return;

    String msg = null;
    if (error != null && error.getErrorMessage() != null)
    {
      msg = error.getErrorMessage();
    }
    errorDisplay = new PlainEditor(WbSwingUtilities.PROP_ERROR_MSG_WRAP, false, false);
    errorDisplay.setText(msg);

    splitPane = new WbSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPane.setContinuousLayout(true);
    splitPane.setOneTouchExpandable(true);

    sqlEditor = EditorPanel.createSqlEditor();
    if (stmtRunner != null)
    {
      sqlEditor.setDatabaseConnection(stmtRunner.getConnection());
    }
    
    if (error != null && error.getOriginalStatement() != null)
    {
      sqlEditor.setText(error.getOriginalStatement());
      setCaretToError();
    }

    statusBar = new WbStatusLabel();

    autoComplete = new AutoCompletionAction(sqlEditor, statusBar);
    autoComplete.setConnection(stmtRunner.getConnection());

    splitPane.setTopComponent(sqlEditor);
    splitPane.setBottomComponent(errorDisplay);

    JLabel lbl = new JLabel(ResourceMgr.getString("MsgExecuteError"));

    Font font = lbl.getFont();
    int borderWith = 8;
    if (font != null)
    {
      FontMetrics fm = lbl.getFontMetrics(font);
      if (fm != null)
      {
        borderWith = (int)(fm.getHeight() / 2);
      }
    }
    EmptyBorder border = new EmptyBorder(borderWith, borderWith, 0, borderWith);
    lbl.setBorder(border);
    splitPane.setBorder(border);

    int gap = IconMgr.getInstance().getSizeForLabel();

    JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.setBorder(new EmptyBorder(borderWith/4, borderWith, 0, borderWith));

    JPanel toolPanel = new JPanel(new BorderLayout(0,0));
    replaceStatement = new WbCheckBox(ResourceMgr.getString("LblReplaceOrgSql"));
    replaceStatement.setToolTipText(ResourceMgr.getDescription("LblReplaceOrgSql"));

    if (enableReplace)
    {
      replaceStatement.setEnabled(true);
      replaceStatement.setSelected(Settings.getInstance().getBoolProperty(PROP_REPLACE_ERROR_STATEMENT, false));
    }
    else
    {
      replaceStatement.setEnabled(false);
    }

    toolPanel.add(replaceStatement, BorderLayout.LINE_START);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    buttonPanel.setBorder(new EmptyBorder(borderWith, 0, borderWith, 0));
    retryButton = new WbButton(ResourceMgr.getString("LblRetry"));
    retryButton.addActionListener(this);
    buttonPanel.add(retryButton);
    buttonPanel.add(Box.createHorizontalStrut(gap * 2));

    ignoreOneButton = new WbButton(ResourceMgr.getString("LblIgnore"));
    ignoreOneButton.addActionListener(this);
    buttonPanel.add(ignoreOneButton);
    buttonPanel.add(Box.createHorizontalStrut(gap / 3));

    ignoreAllButton = new WbButton(ResourceMgr.getString("LblIgnoreAll"));
    ignoreAllButton.addActionListener(this);
    buttonPanel.add(ignoreAllButton);
    buttonPanel.add(Box.createHorizontalStrut(gap / 3));

    cancelButton = new WbButton(ResourceMgr.getString("LblCancel"));
    cancelButton.addActionListener(this);
    buttonPanel.add(cancelButton);
    toolPanel.add(buttonPanel, BorderLayout.CENTER);
    bottomPanel.add(toolPanel, BorderLayout.PAGE_END);
    bottomPanel.add(statusBar, BorderLayout.PAGE_START);

    add(lbl, BorderLayout.PAGE_START);
    add(splitPane, BorderLayout.CENTER);
    add(bottomPanel, BorderLayout.PAGE_END);
  }


  private void closeDialog(int value)
  {
    choice = value;
    if (window == null) return;

    EventQueue.invokeLater(() ->
    {
      window.setVisible(false);
      window.dispose();
    });
  }

  public boolean shouldReplaceOriginalStatement()
  {
    return replaceStatement.isSelected();
  }

  public void dispose()
  {
    if (autoComplete != null) autoComplete.dispose();
    sqlEditor.dispose();
  }

  private void setButtonsEnabled(boolean flag)
  {
    autoComplete.setEnabled(flag);
    cancelButton.setEnabled(flag);
    retryButton.setEnabled(flag);
    ignoreAllButton.setEnabled(flag);
    ignoreOneButton.setEnabled(flag);
  }

  private void startSQL()
  {
    WbThread sqlThread = new WbThread("SqlRetryThread")
    {

      @Override
      public void run()
      {
        runSQL();
      }
    };
    sqlThread.start();
  }

  private void runSQL()
  {
    final String command = getStatement();
    if (StringUtil.isEmptyString(command))
    {
      LogMgr.logWarning("ErrorRetryPanel.runSQL", "No SQL statement!");
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
        closeDialog(WbSwingUtilities.CONTINUE_OPTION);
      }
      else
      {
        EventQueue.invokeLater(() ->
        {
          if (descriptor != null && descriptor.getErrorMessage() != null)
          {
            errorDisplay.setText(descriptor.getErrorMessage());
            if (descriptor.getErrorPosition() > -1)
            {
              sqlEditor.setCaretPosition(descriptor.getErrorPosition());
            }
          }
          sqlEditor.requestFocusInWindow();
        });
      }
    }
    catch (Exception ex)
    {
      LogMgr.logUserSqlError("ErrorRetryPanel.runSQL", command, ex);
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
    }
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == cancelButton || e.getSource() == escAction)
    {
      closeDialog(JOptionPane.CANCEL_OPTION);
    }
    if (e.getSource() == retryButton)
    {
      startSQL();
    }
    if (e.getSource() == ignoreAllButton)
    {
      closeDialog(WbSwingUtilities.IGNORE_ALL);
    }
    if (e.getSource() == ignoreOneButton)
    {
      closeDialog(WbSwingUtilities.IGNORE_ONE);
    }
  }

  @Override
  public void windowOpened(WindowEvent e)
  {
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


  public void showDialog(Frame owner)
  {
    window = new JDialog(owner, true);
    window.setTitle(ResourceMgr.getString("TxtWindowTitleErrorRetry"));

    initUI();

		// creating the action will add it to the input map of the dialog
		// which will enable the key
		escAction = new EscAction(window, this);
    escAction.addToInputMap(sqlEditor);

    window.getContentPane().add(this);
    if (!Settings.getInstance().restoreWindowSize(window, "workbench.gui.sql.retrywindow"))
    {
      window.setSize(640, 480);
    }

    WbSwingUtilities.center(window, owner);
    WbSwingUtilities.requestComponentFocus(window, sqlEditor);
    window.addWindowListener(this);
    window.setVisible(true);

    Settings.getInstance().storeWindowSize(window, "workbench.gui.sql.retrywindow");
    if (enableReplace)
    {
      Settings.getInstance().setProperty(PROP_REPLACE_ERROR_STATEMENT, shouldReplaceOriginalStatement());
    }
  }
}
