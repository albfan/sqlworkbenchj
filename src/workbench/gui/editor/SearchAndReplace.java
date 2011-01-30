/*
 * SearchAndReplace.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import java.awt.Container;
import java.awt.Toolkit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.FindAction;
import workbench.gui.actions.FindAgainAction;
import workbench.gui.actions.ReplaceAction;
import workbench.gui.components.ReplacePanel;
import workbench.gui.components.SearchCriteriaPanel;
import workbench.interfaces.Replaceable;
import workbench.interfaces.Searchable;
import workbench.interfaces.TextContainer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 * This class offeres Search & Replace for a TextContainer
 *
 * @author Thomas Kellerer
 */
public class SearchAndReplace
	implements Replaceable, Searchable
{
	private String lastSearchExpression;
	private String lastSearchCriteria;
	private Pattern lastSearchPattern;
	private int lastSearchPos;
	private ReplacePanel replacePanel;

	private TextContainer editor;
	private Container parent;

	private FindAction findAction;
	private FindAgainAction findAgainAction;
	private ReplaceAction replaceAction;

	/**
	 * Create a new SearchAndReplace support.
	 * @param parentContainer the parent of the textcontainer, needed for displaying dialogs
	 * @param text the container holding the text
	 */
	public SearchAndReplace(Container parentContainer, TextContainer text)
	{
		this.editor = text;
		this.parent = parentContainer;
		this.findAction = new FindAction(this);
		this.findAction.setEnabled(true);
		this.findAgainAction = new FindAgainAction(this);
		this.findAgainAction.setEnabled(false);
		this.replaceAction = new ReplaceAction(this);
		this.replaceAction.setEnabled(true);
	}

	public ReplaceAction getReplaceAction() { return this.replaceAction; }
	public FindAgainAction getFindAgainAction() { return this.findAgainAction; }
	public FindAction getFindAction() { return this.findAction; }

	private String getText() { return editor.getText(); }
	private String getSelectedText() { return editor.getSelectedText();  }
	private int getCaretPosition() { return editor.getCaretPosition(); }

	/**
	 * Show the find dialog and start searching.
	 * @return -1 if nothing was found,
	 *            the position of the found text otherwise
	 */
	public int find()
	{
		boolean showDialog = true;
		String crit = this.getSelectedText();

		// Do not use multi-line selections as the default search criteria
		if (crit != null && crit.indexOf('\n') > -1) crit = null;

		if (crit == null) crit = this.lastSearchCriteria;
		SearchCriteriaPanel p = new SearchCriteriaPanel(crit);

		int pos = -1;
		while (showDialog)
		{
			boolean doFind = p.showFindDialog(this.parent);
			if (!doFind) return -1;
			String criteria = p.getCriteria();
			boolean ignoreCase = p.getIgnoreCase();
			boolean wholeWord = p.getWholeWordOnly();
			boolean useRegex = p.getUseRegex();
			try
			{
				this.lastSearchCriteria = criteria;
				this.findAgainAction.setEnabled(false);
				pos = this.findText(criteria, ignoreCase, wholeWord, useRegex);
				showDialog = false;
				this.findAgainAction.setEnabled(pos > -1);
			}
			catch (Exception e)
			{
				pos = -1;
				WbSwingUtilities.showErrorMessage(this.parent, ExceptionUtil.getDisplay(e));
				showDialog = true;
			}
		}
		return pos;
	}

	public int findNext()
	{
		if (this.lastSearchPattern == null) return -1;
		if (this.lastSearchPos == -1) return -1;

		Matcher m = this.lastSearchPattern.matcher(this.getText());

		if (m.find(this.getCaretPosition() + 1))
		{
			this.lastSearchPos = m.start();
			int end = m.end();
			this.editor.select(this.lastSearchPos, end);
		}
		else
		{
			this.lastSearchPos = -1;
			Toolkit.getDefaultToolkit().beep();
			String msg = ResourceMgr.getString("MsgEditorCriteriaNotFound");
			msg = StringUtil.replace(msg, "%value%", this.lastSearchExpression);
			WbSwingUtilities.showMessage(this.parent, msg);
		}
		return this.lastSearchPos;
	}

	public int findFirst(String aValue, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		int pos = this.findText(aValue, ignoreCase, wholeWord, useRegex);
		return pos;
	}

	public void replace()
	{
		if (this.replacePanel == null)
		{
			this.replacePanel = new ReplacePanel(this);
		}
		this.replacePanel.showReplaceDialog(this.parent, this.editor.getSelectedText());
	}

	/**
	 *	Find and replace the next occurance of the current search string
	 */
	public boolean replaceNext(String aReplacement, boolean useRegex)
	{
		try
		{
			int pos = this.findNext();
			if (pos > -1)
			{
				String text = this.getSelectedText();
				Matcher m = this.lastSearchPattern.matcher(text);
				String newText = m.replaceAll(fixSpecialReplacementChars(aReplacement, useRegex));
				this.editor.setSelectedText(newText);
			}

			return (pos > -1);
		}
		catch (Exception e)
		{
			LogMgr.logError("SearchAndReplace.replaceNext()", "Error replacing value", e);
			WbSwingUtilities.showErrorMessage(e.getMessage());
			return false;
		}
	}

	public boolean isTextSelected()
	{
		int selStart = this.editor.getSelectionStart();
		int selEnd = this.editor.getSelectionEnd();
		return (selStart > -1 && selEnd > selStart);
	}

	/**
	 * Replace special characters in the input string so that it can be used
	 * as a replacement using regular expressions.
	 */
	public static String fixSpecialReplacementChars(String input, boolean useRegex)
	{
		if (!useRegex)
		{
			return StringUtil.quoteRegexMeta(input);
		}

		String fixed = input.replaceAll("\\\\n", "\n");
		fixed = fixed.replaceAll("\\\\r", "\r");
		fixed = fixed.replaceAll("\\\\t", "\t");
		fixed = StringUtil.quoteRegexMeta(fixed);
		return fixed;
	}

	public int replaceAll(String value, String replacement, boolean selectedText, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		String old = null;
		if (selectedText)
		{
			old = this.getSelectedText();
		}
		else
		{
			old = this.getText();
		}
		int cursor = this.getCaretPosition();
		int selStart = this.editor.getSelectionStart();
		int selEnd = this.editor.getSelectionEnd();
		int newLen = -1;
		String regex = getSearchExpression(value, ignoreCase, wholeWord, useRegex);
		replacement = fixSpecialReplacementChars(replacement, useRegex);

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(old);
		String newText = m.replaceAll(replacement);

		if (selectedText)
		{
			this.editor.setSelectedText(newText);
			newLen = this.getText().length();
		}
		else
		{
			this.editor.setText(newText);
			newLen = this.getText().length();
			selStart = -1;
			selEnd = -1;
		}
		if (cursor < newLen)
		{
			this.editor.setCaretPosition(cursor);
		}
		if (selStart > -1 && selEnd > selStart && selStart < newLen && selEnd < newLen)
		{
			this.editor.select(selStart, selEnd);
		}
		return 0;
	}

	public boolean replaceCurrent(String replacement, boolean useRegex)
	{
		if (this.searchPatternMatchesSelectedText())
		{
			try
			{
				Matcher m = this.lastSearchPattern.matcher(this.getSelectedText());
				String newText = m.replaceAll(fixSpecialReplacementChars(replacement, useRegex));
				this.editor.setSelectedText(newText);
				return true;
			}
			catch (Exception e)
			{
				LogMgr.logError("SearchAndReplace.replaceCurrent()", "Error replacing value", e);
				WbSwingUtilities.showErrorMessage(e.getMessage());
				return false;
			}
		}
		else
		{
			return replaceNext(replacement, useRegex);
		}
	}

	public int findText(String anExpression, boolean ignoreCase)
	{
		return this.findText(anExpression, ignoreCase, false, true);
	}

	public static String getSearchExpression(String anExpression, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		StringBuilder result = new StringBuilder(anExpression.length() + 10);

		final String ignoreModifier = "(?i)";

		if (ignoreCase)
		{
			result.append(ignoreModifier);
		}

		if (!useRegex)
		{
			result.append('(');
			result.append(StringUtil.quoteRegexMeta(anExpression));
			result.append(')');
		}
		else
		{
			result.append(anExpression);
		}

		if (wholeWord)
		{
			char c = anExpression.charAt(0);
			// word boundary dos not work if the expression starts with
			// a special Regex character. So in that case, we'll just ignore it
			if (StringUtil.REGEX_SPECIAL_CHARS.indexOf(c) == -1)
			{
				if (ignoreCase)
				{
					result.insert(ignoreModifier.length(), "\\b");
				}
				else
				{
					result.insert(0, "\\b");
				}
			}
			c = anExpression.charAt(anExpression.length() - 1);
			if (StringUtil.REGEX_SPECIAL_CHARS.indexOf(c) == -1)
			{
				result.append("\\b");
			}
		}
		return result.toString();
	}

	public boolean isCurrentSearchCriteria(String aValue, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		if (this.lastSearchExpression == null) return false;
		if (aValue == null) return false;
		String regex = getSearchExpression(aValue, ignoreCase, wholeWord, useRegex);
		return regex.equals(this.lastSearchExpression);
	}

	public int findText(String anExpression, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		String regex = getSearchExpression(anExpression, ignoreCase, wholeWord, useRegex);

		int end = -1;
		this.lastSearchPattern = Pattern.compile(regex);
		this.lastSearchExpression = anExpression;
		Matcher m = this.lastSearchPattern.matcher(this.getText());

		int startPos = this.isTextSelected() ? this.editor.getSelectionStart() : this.getCaretPosition();
		if (m.find(startPos))
		{
			this.lastSearchPos = m.start();
			end = m.end();
			this.editor.select(this.lastSearchPos, end);
		}
		else
		{
			this.lastSearchPos = -1;
			Toolkit.getDefaultToolkit().beep();
			String msg = ResourceMgr.getString("MsgEditorCriteriaNotFound");
			msg = StringUtil.replace(msg, "%value%", anExpression);
			WbSwingUtilities.showMessage(this.parent, msg);
		}
		return this.lastSearchPos;
	}


	public boolean searchPatternMatchesSelectedText()
	{
		if (this.lastSearchPattern == null) return false;
		Matcher m = this.lastSearchPattern.matcher(this.getSelectedText());
		return m.matches();
	}

}
