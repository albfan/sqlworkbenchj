/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExecuteSqlPanel;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbCheckBox;

import workbench.sql.ErrorDescriptor;
import workbench.sql.StatementRunner;
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

  public ErrorRetryPanel(ErrorDescriptor errorDescriptor, StatementRunner runner)
  {
    super(errorDescriptor, runner);
  }

  public void setEnableReplace(boolean flag)
  {
    enableReplace = flag;
  }

  public void setStatement(ScriptParser parser, int cmdIndex)
  {
    super.setStatement(parser, cmdIndex);
    if (parser == null) return;

    String verb = SqlUtil.getSqlVerb(sqlEditor.getText());
    if ("select".equalsIgnoreCase(verb) || "with".equalsIgnoreCase(verb))
    {
      showSelectHint();
    }
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

  protected void statementFinished()
  {
    closeDialog(WbSwingUtilities.CONTINUE_OPTION);
  }

  public boolean shouldReplaceOriginalStatement()
  {
    return replaceStatement.isSelected();
  }

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
    super.showDialog(owner, ResourceMgr.getString("TxtWindowTitleErrorRetry"));
  }
}
