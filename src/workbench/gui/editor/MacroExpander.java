/*
 * MacroExpander.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import workbench.interfaces.MacroChangeListener;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroManager;

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

	public MacroExpander(JEditTextArea textArea)
	{
		MacroManager.getInstance().getMacros().addChangeListener(this);
		macros = MacroManager.getInstance().getExpandableMacros();
		maxTypingPause = GuiSettings.getMaxExpansionPause();
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_EXPAND_MAXDURATION);
		this.editor = textArea;
	}

	public void dispose()
	{
		MacroManager.getInstance().getMacros().removeChangeListener(this);
		Settings.getInstance().removePropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		maxTypingPause = GuiSettings.getMaxExpansionPause();
	}

	private void readMap()
	{
		synchronized (lockMonitor)
		{
			macros = MacroManager.getInstance().getExpandableMacros();
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
				int newCaret = -1;
				int lineStart = editor.getLineStartOffset(currentLine);
				boolean doSelect = false;
				int cursorPos = replacement.indexOf(MacroExpander.CURSOR_PLACEHOLDER);

				if (cursorPos > -1)
				{
					replacement = replacement.replace(MacroExpander.CURSOR_PLACEHOLDER, "");
					newCaret = lineStart + cursorPos;
				}
				else
				{
					cursorPos = replacement.indexOf(MacroExpander.SELECT_PLACEHOLDER);
					if (cursorPos > -1)
					{
						replacement = replacement.replace(MacroExpander.SELECT_PLACEHOLDER, "");
						newCaret = lineStart + cursorPos;
						doSelect = true;
					}
				}

				editor.replaceText(currentLine, start, end, replacement);

				if (newCaret > -1)
				{
					editor.setCaretPosition(newCaret);

					if (doSelect)
					{
						// When expanding the macro, the current line could have changed
						int caretLine = editor.getCaretLine();
						int newLineStart = editor.getLineStartOffset(caretLine);
						String newLine = editor.getLineText(caretLine);
						int newEnd = TextUtilities.findWordEnd(newLine, newCaret) + newLineStart;
						if (newEnd > newCaret)
						{
							editor.select(newCaret, newEnd);
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	public void insertMacroText(String replacement)
	{
		int newCaret = -1;
		int currentLine = editor.getCaretLine();
		int lineStart = editor.getLineStartOffset(currentLine);
		boolean doSelect = false;
		int cursorPos = replacement.indexOf(MacroExpander.CURSOR_PLACEHOLDER);

		if (cursorPos > -1)
		{
			replacement = replacement.replace(MacroExpander.CURSOR_PLACEHOLDER, "");
			newCaret = lineStart + cursorPos;
		}
		else
		{
			cursorPos = replacement.indexOf(MacroExpander.SELECT_PLACEHOLDER);
			if (cursorPos > -1)
			{
				replacement = replacement.replace(MacroExpander.SELECT_PLACEHOLDER, "");
				newCaret = lineStart + cursorPos;
				doSelect = true;
			}
		}

		editor.insertText(replacement);

		if (newCaret > -1)
		{
			editor.setCaretPosition(newCaret);

			if (doSelect)
			{
				// When expanding the macro, the current line could have changed
				int caretLine = editor.getCaretLine();
				int newLineStart = editor.getLineStartOffset(caretLine);
				String newLine = editor.getLineText(caretLine);
				int newEnd = TextUtilities.findWordEnd(newLine, newCaret) + newLineStart;
				if (newEnd > newCaret)
				{
					editor.select(newCaret, newEnd);
				}
			}
		}
	}

}
