/*
 * DuplicateCurrentLine.java
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

import javax.swing.text.BadLocationException;

import workbench.resource.Settings;

import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;

/**
 *
 * @author Thomas Kellerer
 */
public class DuplicateCurrentLine
	extends EditorAction
{
	public DuplicateCurrentLine()
	{
		super("TxtEdDupLine", null);
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		if (textArea == null) return;


		int insertPoint = -1;
		int currentLine = textArea.getCaretLine();
		int currentColumn = textArea.getCaretPositionInLine(currentLine);

		boolean wasSelected = false;

		String dupeText = textArea.getSelectedText();
		
		if (dupeText != null)
		{
			insertPoint = textArea.getSelectionEnd();
			textArea.select(insertPoint, insertPoint);
			wasSelected = true;
		}
		else
		{
			dupeText = textArea.getLineText(currentLine) + Settings.getInstance().getInternalEditorLineEnding();
			insertPoint = textArea.getLineEndOffset(currentLine);
		}

		try
		{
			textArea.getDocument().insertString(insertPoint, dupeText, null);
		}
		catch (BadLocationException bl)
		{
			bl.printStackTrace();
		}

		if (wasSelected)
		{
			textArea.select(insertPoint, insertPoint + dupeText.length());
		}
		else
		{
			int newPos = textArea.getLineStartOffset(currentLine + 1) + currentColumn;
			textArea.setCaretPosition(newPos);
		}
	}
}
