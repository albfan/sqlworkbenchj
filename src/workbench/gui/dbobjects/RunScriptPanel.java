/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExecuteSqlPanel;
import workbench.gui.editor.SearchAndReplace;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class RunScriptPanel
  extends ExecuteSqlPanel
{
  private JButton cancelButton;
  private JButton closeButton;
  private JButton saveAsButton;
  private JButton startButton;
	private boolean wasRun;
  private String textToHighlight;

  public RunScriptPanel(WbConnection connection, String sql)
  {
    super(connection, sql);
  }

  @Override
  protected JPanel getToolPanel()
  {
    JPanel buttonPanel = new JPanel(new BorderLayout());

    startButton = new JButton(ResourceMgr.getString("LblStartSql"));
    cancelButton = new JButton(ResourceMgr.getString("LblCancelPlain"));
    cancelButton.setEnabled(false);
    closeButton = new JButton(ResourceMgr.getString("LblClose"));
    saveAsButton = new JButton(ResourceMgr.getString("MnuTxtFileSave"));

		startButton.addActionListener(this);
		cancelButton.addActionListener(this);
		closeButton.addActionListener(this);
		saveAsButton.setAction(sqlEditor.getFileSaveAsAction());
		saveAsButton.setEnabled(true);

    JPanel leftButtons = new JPanel(new GridLayout(1, 0));

    leftButtons.add(saveAsButton);
    leftButtons.add(startButton);
    leftButtons.add(cancelButton);
    buttonPanel.add(leftButtons, BorderLayout.LINE_START);
    buttonPanel.add(closeButton, BorderLayout.LINE_END);

    return buttonPanel;
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    super.actionPerformed(e);
		if (e.getSource() == startButton)
		{
			startSQL();
		}
		else if (e.getSource() == closeButton)
		{
			closeWindow();
		}
		else if (e.getSource() == cancelButton)
		{
			cancel();
		}
  }

  @Override
  protected void runSQL()
  {
    wasRun = true;
    super.runSQL();
  }

  @Override
  protected JLabel getLabel()
  {
    return null;
  }

  @Override
  protected String getWindowSettingsKey()
  {
    return "workbench.gui.runscript.window";
  }

	public boolean wasRun()
	{
		return wasRun;
	}

  @Override
  protected void setButtonsEnabled(boolean flag)
  {
    super.setButtonsEnabled(flag);
    closeButton.setEnabled(flag);
    startButton.setEnabled(flag);
    cancelButton.setEnabled(!startButton.isEnabled());
  }

	private void highlightText(String text)
	{
		if (StringUtil.isBlank(text)) return;

    SearchAndReplace replacer = sqlEditor.getReplacer();
    replacer.setShowNotifications(false);

		int start = replacer.findFirst(text, true, true, false);
		int end = start + text.length();
		if (start > -1)
		{
			sqlEditor.select(start, end);
		}
		sqlEditor.requestFocusInWindow();
	}

  @Override
  public void windowOpened(WindowEvent e)
  {
    super.windowOpened(e);
    sqlEditor.setCaretPosition(0);
    highlightText(textToHighlight);
  }

  /**
   * Opens a dialog showing this panel.
   *
   * This is run on the EDT.
   *
   * @param owner       the owner of the dialog
   * @param title       the title of the dialog
   */
	public void openWindow(Component comp, String title)
	{
		openWindow(comp, title, null);
	}

  /**
   * Opens a dialog showing this panel.
   *
   * This is run on the EDT.
   *
   * @param owner       the owner of the dialog
   * @param title       the title of the dialog
   * @param highlight   a string to be highlighted in the editor
   */
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

  /**
   * Opens a dialog showing this panel.
   *
   * This is run on the EDT.
   *
   * @param owner       the owner of the dialog
   * @param title       the title of the dialog
   * @param highlight   a string to be highlighted in the editor
   */
	public void openWindow(final Frame owner, final String title, final String highlight)
  {
    textToHighlight = highlight;
    WbSwingUtilities.invoke(new Runnable()
    {
      @Override
      public void run()
      {
        showDialog(owner, title);
      }
    });
  }

}
