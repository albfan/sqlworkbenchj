/*
 * PreviousLine.java
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
package workbench.gui.editor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;

/**
 *
 * @author Thomas Kellerer
 */
public class PreviousLine
	extends EditorAction
{
	protected boolean select;

	public PreviousLine()
	{
		super("TxtEdPrvLine", KeyEvent.VK_UP, 0);
		select = false;
	}

	public PreviousLine(String resourceKey, int key, int modifier)
	{
		super(resourceKey, key, modifier);
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		int caret = textArea.getCaretPosition();
		int line = textArea.getCaretLine();

		if (line == 0)
		{
			textArea.getToolkit().beep();
			if (!select)
			{
				textArea.selectNone();
			}
			return;
		}

		int magic = textArea.getMagicCaretPosition();
		if (magic == -1)
		{
			magic = textArea.offsetToX(line, caret - textArea.getLineStartOffset(line));
		}

		caret = textArea.getLineStartOffset(line - 1) + textArea.xToOffset(line - 1, magic);

		if (select)
		{
			textArea.select(textArea.getMarkPosition(), caret);
		}
		else
		{
			textArea.setCaretPosition(caret);
		}

		textArea.setMagicCaretPosition(magic);
	}
}
