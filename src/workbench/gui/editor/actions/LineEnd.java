/*
 * LineEnd.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.KeyStroke;
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.resource.PlatformShortcuts;

/**
 *
 * @author Thomas Kellerer
 */
public class LineEnd
	extends EditorAction
{
	protected boolean select;

	public LineEnd()
	{
		super("TxtEdLineEnd", PlatformShortcuts.getDefaultEndOfLine(false));
		select = false;
	}

	protected LineEnd(String resourceKey, KeyStroke key)
	{
		super(resourceKey, key);
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = getTextArea(evt);

		int line = textArea.getCaretLine();
		int caret = textArea.getCaretPosition();

		int lastOfLine = textArea.getLineEndOffset(line) - 1;
		int lastVisibleLine = textArea.getFirstLine() + textArea.getVisibleLines();
		if (lastVisibleLine >= textArea.getLineCount())
		{
			lastVisibleLine = Math.min(textArea.getLineCount() - 1, lastVisibleLine);
		}
		else
		{
			lastVisibleLine -= (textArea.getElectricScroll() + 1);
		}

		int lastVisible = textArea.getLineEndOffset(lastVisibleLine) - 1;
		int lastDocument = textArea.getDocumentLength();

		if (caret == lastDocument)
		{
			textArea.getToolkit().beep();
			if (!select)
			{
				textArea.selectNone();
			}
			return;
		}
		else if (!Boolean.TRUE.equals(textArea.getClientProperty(InputHandler.SMART_HOME_END_PROPERTY)))
		{
			caret = lastOfLine;
		}
		else if (caret == lastVisible)
		{
			caret = lastDocument;
		}
		else if (caret == lastOfLine)
		{
			caret = lastVisible;
		}
		else
		{
			caret = lastOfLine;
		}

		if (select)
		{
			textArea.select(textArea.getMarkPosition(), caret);
		}
		else
		{
			textArea.setCaretPosition(caret);
		}
	}
}
