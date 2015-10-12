/*
 * LogArea.java
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
package workbench.gui.sql;

import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import workbench.interfaces.TextContainer;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.editor.SearchAndReplace;

/**
 * @author Thomas Kellerer
 */
public class LogArea
	extends JTextArea
	implements PropertyChangeListener, TextContainer
{
	private TextComponentMouseListener contextMenu;
  private int maxLines = Integer.MAX_VALUE;

	public LogArea()
  {
    this(null);
  }

	public LogArea(Container owner)
	{
		super();
    setDoubleBuffered(true);
		setBorder(WbSwingUtilities.EMPTY_BORDER);
		setFont(Settings.getInstance().getMsgLogFont());
		setEditable(false);
		setLineWrap(true);
		setWrapStyleWord(true);

		initColors();

		contextMenu = new TextComponentMouseListener();
		addMouseListener(contextMenu);

    if (owner != null)
    {
      SearchAndReplace searcher = new SearchAndReplace(owner, this);
      contextMenu.addAction(searcher.getFindAction());
      contextMenu.addAction(searcher.getFindNextAction());
    }

		Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_EDITOR_FG_COLOR, Settings.PROPERTY_EDITOR_BG_COLOR);
	}

  @Override
  public void setSelectedText(String text)
  {
    super.replaceSelection(text);
  }

  @Override
  public boolean isTextSelected()
  {
    return getSelectionEnd() > getSelectionStart();
  }

  @Override
  public String getWordAtCursor(String wordChars)
  {
    return null;
  }


	public void dispose()
	{
		setText("");
		Settings.getInstance().removePropertyChangeListener(this);
		if (contextMenu != null)
		{
			contextMenu.dispose();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		initColors();
	}

	private void initColors()
	{
		setBackground(Settings.getInstance().getEditorBackgroundColor());
		setForeground(Settings.getInstance().getEditorTextColor());
	}

  public void setMaxLineCount(int count)
  {
    this.maxLines = count;
  }

  public void deleteLine(int lineNumber)
  {
    try
    {
      int start = getLineStartOffset(lineNumber);
      int end = getLineEndOffset(lineNumber);
      getDocument().remove(start, (end - start));
    }
    catch (BadLocationException ble)
    {
      // ignoreO
    }
  }
  
  public void addLine(String line)
  {
    if (line == null) return;

    if (getLineCount() >= maxLines)
    {
      System.out.println("*** Current line count: " + getLineCount() + " exceeds limit of: " + maxLines);
      deleteLine(0);
    }
    append(line + "\n");
    int start;
    try
    {
      start = getLineStartOffset(getLineCount() - 1);
      setCaretPosition(start);
    }
    catch (BadLocationException ex)
    {
      // ignore
    }

  }

}
