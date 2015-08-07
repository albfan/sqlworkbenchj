/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.gui.sql;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import workbench.gui.WbSwingUtilities;
import workbench.sql.ErrorDescriptor;
import workbench.sql.StatementRunner;
import workbench.sql.parser.ScriptParser;

/**
 *
 * @author Thomas Kellerer
 */
public class ErrorRetryPanel
  extends JPanel
{
  private JComponent errorDisplay;
  private EditorPanel sqlEditor;
  private StatementRunner stmtRunner;
  private ErrorDescriptor error;
  private String errorMessage;
  private ScriptParser scriptParser;

  private int commandIndex;
  private int sqlStartPos;
  private int sqlEndPos;

  public ErrorRetryPanel(ErrorDescriptor errorDescriptor, String errorMsg, StatementRunner runner, int cmdIndex, int sqlStart, int sqlEnd)
  {
    super(new GridBagLayout());
    this.stmtRunner = runner;
    this.commandIndex = cmdIndex;
    this.sqlStartPos = sqlStart;
    this.sqlEndPos = sqlEnd;
    error = errorDescriptor;
    errorMessage = errorMsg;
  }

  public void setScriptParser(ScriptParser parser)
  {
    scriptParser = parser;
  }

  private void initComponents()
  {
    String msg = errorMessage;
    if (error != null && error.getErrorMessage() != null)
    {
      msg = error.getErrorMessage();
    }
    errorDisplay = WbSwingUtilities.createErrorMessagePanel(msg, WbSwingUtilities.PROP_ERROR_MSG_WRAP);
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.LINE_START;
    gc.gridx = 0;
    gc.gridy = 0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 1.0;
    add(errorDisplay, gc);

    sqlEditor = EditorPanel.createSqlEditor();
    if (stmtRunner != null)
    {
      sqlEditor.setDatabaseConnection(stmtRunner.getConnection());
    }
    gc.fill = GridBagConstraints.BOTH;
    gc.gridy ++;
    add(sqlEditor, gc);
  }

}
