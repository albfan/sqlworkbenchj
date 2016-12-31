/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExecuteSqlPanel;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbCheckBox;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ErrorDescriptor;
import workbench.sql.parser.ScriptParser;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ErrorRetryPanel
  extends ExecuteSqlPanel
  implements ActionListener, WindowListener
{
  public static final String PROP_REPLACE_ERROR_STATEMENT = "workbench.gui.retry_error.replace.statement";

  private JPanel buttonPanel;
  private JButton retryButton;
  private JButton cancelButton;
  private JButton ignoreOneButton;
  private JButton ignoreAllButton;
  private JCheckBox replaceStatement;
  private boolean enableReplace = true;

  public ErrorRetryPanel(WbConnection conn)
  {
    super(conn);
  }

  public void setEnableReplace(boolean flag)
  {
    enableReplace = flag;
  }

  public void setStatement(ScriptParser parser, int cmdIndex, ErrorDescriptor error)
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

    String verb = SqlUtil.getSqlVerb(sql);

    if ("select".equalsIgnoreCase(verb) || "with".equalsIgnoreCase(verb))
    {
      showSelectHint();
    }

    showError(error);
  }

  public String getStatement()
  {
    if (sqlEditor == null) return "";

    ScriptParser parser = ScriptParser.createScriptParser(runner.getConnection());
    parser.setScript(sqlEditor.getText());
    if (parser.getSize() < 1)
    {
      return "";
    }
    return parser.getCommand(0);
  }

  @Override
  protected JPanel getToolPanel()
  {
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

    int gap = IconMgr.getInstance().getSizeForLabel();

    toolPanel.add(replaceStatement, BorderLayout.LINE_START);

    buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    retryButton = new WbButton(ResourceMgr.getString("LblRetry"));
    retryButton.addActionListener(this);
    buttonPanel.add(retryButton);
    buttonPanel.add(Box.createHorizontalStrut(gap * 2));

    ignoreOneButton = new WbButton(ResourceMgr.getString("LblIgnoreThis"));
    ignoreOneButton.addActionListener(this);
    buttonPanel.add(ignoreOneButton);
    buttonPanel.add(Box.createHorizontalStrut(gap / 3));

    ignoreAllButton = new WbButton(ResourceMgr.getString("LblIgnoreAllErr"));
    ignoreAllButton.addActionListener(this);
    buttonPanel.add(ignoreAllButton);
    buttonPanel.add(Box.createHorizontalStrut(gap / 3));

    cancelButton = new WbButton(ResourceMgr.getString("LblStopScript"));
    cancelButton.addActionListener(this);
    buttonPanel.add(cancelButton);
    toolPanel.add(buttonPanel, BorderLayout.CENTER);

    return toolPanel;
  }

  @Override
  protected JLabel getLabel()
  {
    return new JLabel(ResourceMgr.getString("MsgExecuteError"));
  }

  private void showSelectHint()
  {
    JLabel lbl = new JLabel("<html>Query results will <b>not</b> be displayed!</html>");
    lbl.setBorder(new EmptyBorder(0,0,0,IconMgr.getInstance().getSizeForLabel() / 3));
    buttonPanel.add(lbl, 0);
  }

  @Override
  protected void scriptSuccess()
  {
    super.scriptSuccess();
    closeDialog(WbSwingUtilities.CONTINUE_OPTION);
  }

  public boolean shouldReplaceOriginalStatement()
  {
    return replaceStatement.isSelected();
  }

  @Override
  protected void setButtonsEnabled(boolean flag)
  {
    super.setButtonsEnabled(flag);
    cancelButton.setEnabled(flag);
    retryButton.setEnabled(flag);
    ignoreAllButton.setEnabled(flag);
    ignoreOneButton.setEnabled(flag);
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    super.actionPerformed(e);
    if (e.getSource() == cancelButton)
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
  protected String getWindowSettingsKey()
  {
    return "workbench.gui.sql.retrywindow";
  }

  @Override
  protected void saveSettings()
  {
    super.saveSettings();
    if (enableReplace)
    {
      Settings.getInstance().setProperty(PROP_REPLACE_ERROR_STATEMENT, shouldReplaceOriginalStatement());
    }
  }

  public void showDialog(Window owner)
  {
	  super.showDialog(owner, ResourceMgr.getString("TxtWindowTitleErrorRetry"), true);
  }

  @Override
  public void showDialog(Window owner, String windowTitle, boolean isModal)
  {
    super.showDialog(owner, windowTitle, isModal);
  }
}
