/*
 * PlainEditor.java
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
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import workbench.interfaces.Restoreable;
import workbench.interfaces.TextContainer;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.editor.SearchAndReplace;

/**
 * A simple text editor based on a JTextArea.
 * The panel displays also a checkbox to turn word wrapping on and off
 * and optionally an information label.
 *
 * @author Thomas Kellerer
 */
public class PlainEditor
	extends JPanel
	implements ActionListener, TextContainer, Restoreable
{
	private JTextArea editor;
	private JCheckBox wordWrap;
	private Color enabledBackground;
	private JLabel infoText;
	private JPanel toolPanel;
	private JScrollPane scroll;
  private String wrapSettingsKey;
  private TextComponentMouseListener editMenu;


	public PlainEditor()
  {
    this(Settings.PROP_PLAIN_EDITOR_WRAP, true, true);
  }

	public PlainEditor(String settingsKey, boolean allowEdit, boolean enableWordWrap)
	{
		super();
		editor = new JTextArea();
		enabledBackground = editor.getBackground();
		editor.putClientProperty("JTextArea.infoBackground", Boolean.TRUE);
		editMenu = new TextComponentMouseListener(editor);

		scroll = new JScrollPane(editor);
		editor.setFont(Settings.getInstance().getEditorFont());
		this.setLayout(new BorderLayout());

    wrapSettingsKey = settingsKey;
    if (enableWordWrap)
    {
      editor.setLineWrap(true);
      editor.setWrapStyleWord(true);
      toolPanel = new JPanel();
      toolPanel.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
      wordWrap = new JCheckBox(ResourceMgr.getString("LblWordWrap"));
      wordWrap.setSelected(true);
      wordWrap.setFocusable(false);
      wordWrap.addActionListener(this);
      toolPanel.add(wordWrap);
      this.add(toolPanel, BorderLayout.NORTH);
    }

		this.add(scroll, BorderLayout.CENTER);
		this.setFocusable(false);

		Document d = editor.getDocument();
		if (d != null)
		{
			int tabSize = Settings.getInstance().getEditorTabWidth();
			d.putProperty(PlainDocument.tabSizeAttribute, Integer.valueOf(tabSize));
		}

    if (allowEdit)
    {
      SearchAndReplace replacer = new SearchAndReplace(this, this);
      editMenu.addAction(replacer.getFindAction());
      editMenu.addAction(replacer.getFindNextAction());
      editMenu.addAction(replacer.getReplaceAction());
    }
    setEditable(allowEdit);
	}

  @Override
  public void removeNotify()
  {
    super.removeNotify();
    editMenu.dispose();
  }

	public void removeBorders()
	{
		scroll.setBorder(WbSwingUtilities.EMPTY_BORDER);
    if (toolPanel != null) toolPanel.setBorder(DividerBorder.BOTTOM_DIVIDER);
	}

	@Override
	public int getCaretPosition()
	{
		return this.editor.getCaretPosition();
	}

	@Override
	public int getSelectionEnd()
	{
		return this.editor.getSelectionEnd();
	}

	@Override
	public int getSelectionStart()
	{
		return this.editor.getSelectionStart();
	}

	@Override
	public void select(int start, int end)
	{
		this.editor.select(start, end);
	}

	public void setInfoText(String text)
	{
		if (this.infoText == null && toolPanel != null)
		{
			this.infoText = new JLabel();
			this.toolPanel.add(Box.createHorizontalStrut(10));
			this.toolPanel.add(infoText);
		}
		this.infoText.setText(text);
	}

  public int getScrollbarHeight()
  {
    int height = 0;
    JScrollBar bar = scroll.getHorizontalScrollBar();
    if (bar != null)
    {
      Dimension prefSize = bar.getPreferredSize();
      if (prefSize != null)
      {
        height = (int)prefSize.getHeight();
      }
      else
      {
        height = bar.getHeight();
      }
    }
    return height;
  }

	@Override
	public void requestFocus()
	{
		this.editor.requestFocus();
	}

	@Override
	public boolean requestFocusInWindow()
	{
		return this.editor.requestFocusInWindow();
	}

	@Override
	public void restoreSettings()
	{
    if (wordWrap != null)
    {
      boolean wrap = Settings.getInstance().getBoolProperty(wrapSettingsKey);
      wordWrap.setSelected(wrap);
      editor.setLineWrap(wrap);
    }
	}

	@Override
	public void saveSettings()
	{
    if (wordWrap != null)
    {
      Settings.getInstance().setProperty(wrapSettingsKey, wordWrap.isSelected());
    }
	}

	@Override
	public void setSelectedText(String aText)
	{
		this.editor.replaceSelection(aText);
	}

	@Override
	public String getText()
	{
		return this.editor.getText();
	}

	@Override
	public String getSelectedText()
	{
		return this.editor.getSelectedText();
	}

	@Override
	public void setText(String aText)
	{
		this.editor.setText(aText);
	}

	@Override
	public void setCaretPosition(int pos)
	{
		this.editor.setCaretPosition(pos);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		this.editor.setLineWrap(this.wordWrap.isSelected());
    if (wrapSettingsKey != null)
    {
      Settings.getInstance().setProperty(wrapSettingsKey, wordWrap.isSelected());
    }
	}

	@Override
	public void setEditable(boolean flag)
	{
		this.editor.setEditable(flag);
		this.editor.setBackground(enabledBackground);
	}

	@Override
	public boolean isEditable()
	{
		return this.editor.isEditable();
	}

	@Override
	public boolean isTextSelected()
	{
		return (getSelectionStart() < getSelectionEnd());
	}

  @Override
  public String getWordAtCursor(String wordChars)
  {
    return null;
  }

  public int getLineCount()
  {
    return editor.getLineCount();
  }
}
