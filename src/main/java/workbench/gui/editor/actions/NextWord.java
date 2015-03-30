/*
 * NextWord.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.editor.actions;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.TextUtilities;
import workbench.resource.PlatformShortcuts;

/**
 *
 * @author Thomas Kellerer
 */
public class NextWord
	extends EditorAction
{
	protected boolean select;


	public NextWord()
	{
		super("TxtEdNxtWord", PlatformShortcuts.getDefaultNextWord(false));
		select = false;
	}

	public NextWord(String resourceKey, KeyStroke key)
	{
		super(resourceKey, key);
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = getTextArea(evt);
		jump(textArea, select);
	}

	public static void jump(JEditTextArea textArea, boolean select)
	{
		int caret = textArea.getCaretPosition();
		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		int lineEnd = textArea.getLineEndOffset(line);
		caret -= lineStart;

		String lineText = textArea.getLineText(textArea.getCaretLine());

		if (caret == lineText.length())
		{
			if (lineStart + caret == textArea.getDocumentLength())
			{
				textArea.getToolkit().beep();
				return;
			}
			caret++;
		}
		else
		{
			caret = TextUtilities.findWordEnd(lineText, caret);
		}

		int newPos = 0;
		if (caret == -1)
		{
			newPos = lineEnd - 1;
		}
		else
		{
			newPos = lineStart + caret;
		}

		if (select)
		{
			textArea.select(textArea.getMarkPosition(), newPos);
		}
		else
		{
			textArea.setCaretPosition(newPos);
		}
	}
}
