/*
 * MacroExpander.java
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
package workbench.gui.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import workbench.interfaces.MacroChangeListener;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.gui.editor.actions.NextWord;

import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroManager;
import workbench.sql.macros.MacroStorage;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroExpander
	implements MacroChangeListener, PropertyChangeListener
{

	public static final String CURSOR_PLACEHOLDER = "${c}";
	public static final String SELECT_PLACEHOLDER = "${s}";

	private final Object lockMonitor = new Object();

	private Map<String, MacroDefinition> macros;
	private int maxTypingPause = 350;

	private JEditTextArea editor;
	private final int macroClientId;

	public MacroExpander(int clientId, JEditTextArea textArea)
	{
		macroClientId = clientId;
		MacroManager.getInstance().getMacros(macroClientId).addChangeListener(this);
		macros = MacroManager.getInstance().getExpandableMacros(macroClientId);
		maxTypingPause = GuiSettings.getMaxExpansionPause();
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_EXPAND_MAXDURATION);
		this.editor = textArea;
	}

	public void dispose()
	{
		MacroStorage storage = MacroManager.getInstance().getMacros(macroClientId);
		if (storage != null)
		{
			storage.removeChangeListener(this);
		}
		Settings.getInstance().removePropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		maxTypingPause = GuiSettings.getMaxExpansionPause();
	}

	public int getMacroClientId()
	{
		return macroClientId;
	}

	private void readMap()
	{
		synchronized (lockMonitor)
		{
			macros = MacroManager.getInstance().getExpandableMacros(macroClientId);
		}
	}

	@Override
	public void macroListChanged()
	{
		readMap();
	}

	public boolean hasExpandableMacros()
	{
		return macros.size() > 0;
	}

	public String getReplacement(String word)
	{
		MacroDefinition macro = macros.get(word);
		if (macro == null) return null;
		return macro.getText();
	}

	public boolean expandWordAtCursor()
	{
		long duration = System.currentTimeMillis() - editor.getLastModifiedTime();
		if (!hasExpandableMacros()) return false;

		if (duration > maxTypingPause) return false;

		int currentLine = editor.getCaretLine();
		String line = editor.getLineText(currentLine);
		int pos = editor.getCaretPositionInLine(currentLine);
		if (pos < 0) return false;

		int start = TextUtilities.findWordStart(line, pos);
		int end = TextUtilities.findWordEnd(line, pos);

		if (start < end && start >= 0)
		{
			String word = line.substring(start, end);
			String replacement = getReplacement(word);
			if (replacement != null)
			{
				insertMacroText(replacement, start, end);
				return true;
			}
		}
		return false;
	}

	public void insertMacroText(String replacement)
	{
		insertMacroText(replacement, -1, -1);
	}

	private void insertMacroText(String replacement, int start, int end)
	{
		int newCaret = -1;
		int currentLine = editor.getCaretLine();
		int lineStart = editor.getLineStartOffset(currentLine);

		boolean doSelect = shouldSelect(replacement);
		int cursorPos = getCaretPositionInString(replacement);
		if (cursorPos > -1)
		{
			newCaret = lineStart + start + cursorPos;
		}

		replacement = replacement.replace(CURSOR_PLACEHOLDER, "").replace(SELECT_PLACEHOLDER, "");

		if (start > -1 && end > -1)
		{
			editor.replaceText(currentLine, start, end, replacement);
		}
		else
		{
			editor.insertText(replacement);
		}

		if (newCaret > -1)
		{
			editor.setCaretPosition(newCaret);
			if (doSelect)
			{
				NextWord.jump(editor, true);
			}
		}
	}

	private int getCaretPositionInString(String replacement)
	{
		int cursorPos = replacement.indexOf(CURSOR_PLACEHOLDER);
		if (cursorPos == -1)
		{
			cursorPos = cursorPos = replacement.indexOf(SELECT_PLACEHOLDER);
		}
		return cursorPos;
	}

	private boolean shouldSelect(String replacement)
	{
		return replacement.indexOf(SELECT_PLACEHOLDER) > -1;
	}

}
