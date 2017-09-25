/*
 * SearchAndReplace.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.editor;

import java.awt.Container;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.FindAction;
import workbench.gui.actions.FindNextAction;
import workbench.gui.actions.FindPreviousAction;
import workbench.gui.actions.ReplaceAction;
import workbench.gui.actions.WbAction;
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
	private FindPreviousAction findPreviousAction;
	private FindNextAction findNextAction;
	private ReplaceAction replaceAction;

	private boolean wrapSearch;
  private boolean showNotifications = true;

	/**
	 * Create a new SearchAndReplace support.
	 *
	 * @param parentContainer the parent of the textcontainer, needed for displaying dialogs
	 * @param text the container holding the text
	 */
	public SearchAndReplace(Container parentContainer, TextContainer text)
	{
		this.editor = text;
		this.parent = parentContainer;
		this.findAction = new FindAction(this);
		this.findPreviousAction = new FindPreviousAction(this);
		this.findPreviousAction.setEnabled(false);
		this.findAction.setEnabled(true);
		this.findNextAction = new FindNextAction(this);
		this.findNextAction.setEnabled(false);
		this.replaceAction = new ReplaceAction(this);
		this.replaceAction.setEnabled(true);
	}

  public void setShowNotifications(boolean flag)
  {
    this.showNotifications = flag;
  }

	public boolean getWrapSearch()
	{
		return wrapSearch;
	}

	@Override
	public void setWrapSearch(boolean flag)
	{
		this.wrapSearch = flag;
	}

	public ReplaceAction getReplaceAction()
	{
		return this.replaceAction;
	}

	public FindPreviousAction getFindPreviousAction()
	{
		return this.findPreviousAction;
	}

	public FindNextAction getFindNextAction()
	{
		return this.findNextAction;
	}

	public FindAction getFindAction()
	{
		return this.findAction;
	}

	private String getText()
	{
		return editor.getText();
	}

	private String getSelectedText()
	{
		return editor.getSelectedText();
	}

	private int getCaretPosition()
	{
		return editor.getCaretPosition();
	}

	/**
	 * Show the find dialog and start searching.
	 * @return -1 if nothing was found,
	 *            the position of the found text otherwise
	 */
	@Override
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
			setWrapSearch(p.getWrapSearch());
			try
			{
				this.lastSearchCriteria = criteria;
				this.findNextAction.setEnabled(false);
				this.findPreviousAction.setEnabled(false);
				pos = this.findText(criteria, ignoreCase, wholeWord, useRegex);
				showDialog = false;
				this.findNextAction.setEnabled(pos > -1 || wrapSearch);
				this.findPreviousAction.setEnabled(pos > -1);
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

	@Override
	public int findPrevious()
	{
		if (this.lastSearchPattern == null) return -1;
		if (this.lastSearchPos == -1) return -1;

		Matcher m = this.lastSearchPattern.matcher(this.getText());
		int startPos = 0;
		int lastFound = -1;
		int lastEnd = -1;
		while (m.find(startPos))
		{
			int foundPos = m.start();
			if (foundPos >= this.lastSearchPos)
			{
				if (lastFound == -1)
				{
					lastFound = foundPos;
					lastEnd = m.end();
					Toolkit.getDefaultToolkit().beep();
				}
				lastSearchPos = lastFound;
				this.editor.select(this.lastSearchPos, lastEnd);
				return lastFound;
			}
			lastFound = foundPos;
			lastEnd = m.end();
			startPos = m.end() + 1;
		}
		return -1;
	}

	@Override
	public int findNext()
	{
		if (this.lastSearchPattern == null) return -1;
		if (this.lastSearchPos == -1 && !wrapSearch) return -1;

		Matcher m = this.lastSearchPattern.matcher(this.getText());

		int startPos = this.getCaretPosition() + 1;
		if (wrapSearch && lastSearchPos == -1)
		{
			startPos = 0;
		}

		boolean found = m.find(startPos);
		if (!found && wrapSearch)
		{
			found = m.find(0);
		}

		if (found)
		{
			this.lastSearchPos = m.start();
			int end = m.end();
			this.editor.select(this.lastSearchPos, end);
		}
    else if (showNotifications)
		{
			this.lastSearchPos = -1;
			Toolkit.getDefaultToolkit().beep();
			String msg = ResourceMgr.getString("MsgEditorCriteriaNotFound");
			msg = StringUtil.replace(msg, "%value%", this.lastSearchExpression);
			WbSwingUtilities.showMessage(this.parent, msg);
		}
		findNextAction.setEnabled(this.lastSearchPos > -1);
		return this.lastSearchPos;
	}

	@Override
	public int findFirst(String aValue, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		this.lastSearchPos = this.findText(aValue, ignoreCase, wholeWord, useRegex);
		findNextAction.setEnabled(this.lastSearchPos > -1);
		return lastSearchPos;
	}

	@Override
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
	@Override
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

	@Override
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
		return fixed;
	}

  /**
   * Return all matches for the given search expression.
   *
   * contextLines controls the number of lines before and after a "hit" are returned. <br/>
   *
   * If contextLines = 1, 3 lines will be returned for each occurance.<br/>
   * If contextLines = 2, 5 lines will be returned for each occurance.<br/>
   *
   * Only one SearchResult per editor line will returned.
   *
   * @param expression     the expression to search for
   * @param ignoreCase     if true search is case insensitive
   * @param wholeWord      if false partial matches in a string are returned
   * @param isRegex        if true, <tt>expression</tt> is assumed to be a RegEx
   * @param contextLines   the number of lines before and after the search hit to return for each match.
   * @return
   */
  @Override
  public List<SearchResult> findAll(String expression, boolean ignoreCase, boolean wholeWord, boolean useRegex, int contextLines)
  {
    String regex = getSearchExpression(expression, ignoreCase, wholeWord, useRegex);
    List<SearchResult> result = new ArrayList<>();
		Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher m = p.matcher(getText());

    int pos = 0;
    int previousLine = -1;

    while (m.find(pos))
    {
      int start = m.start();
      int end = m.end();
      int line = editor.getLineOfOffset(start);

      // don't return multiple hits in the same line
      if (line != previousLine)
      {
        String text = getLineContext(line, contextLines);
        SearchResult searchResult = new SearchResult(text, start, end - start, line, -1);
        result.add(searchResult);
      }

      previousLine = line;
      pos = end;
    }
    return result;
  }

  private String getLineContext(int line, int around)
  {
    if (line < 0) return null;
    if (around <= 0) return editor.getLineText(line);

    int startLine = Math.max(0, line - around);
    int endLine = Math.min(editor.getLineCount() - 1, line + around);
    StringBuilder result = new StringBuilder(around * 80);
    for (int l=startLine; l <= endLine; l++)
    {
      if (l > startLine) result.append('\n');
      result.append(editor.getLineText(l));
    }
    return result.toString();
  }

	@Override
	public void replaceAll(String value, String replacement, boolean selectedText, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		String old = null;
		if (selectedText)
		{
			old = this.getSelectedText();
      if (old == null) return;
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

		Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher m = p.matcher(old);
		String newText = m.replaceAll(replacement);

		if (selectedText)
		{
			this.editor.setSelectedText(newText);
			newLen = this.getText().length();
			int delta = newText.length() - old.length();
			selEnd += delta;
			if (selStart < selEnd)
			{
				this.editor.select(selStart, selEnd);
			}
			else if (cursor < newLen)
			{
				this.editor.setCaretPosition(cursor);
			}
		}
		else
		{
			this.editor.setText(newText);
			newLen = this.getText().length();
			if (cursor < newLen)
			{
				this.editor.setCaretPosition(cursor);
			}
			else
			{
				this.editor.setCaretPosition(0);
			}
		}
	}

	@Override
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
		this.lastSearchPattern = Pattern.compile(regex, Pattern.MULTILINE);
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
			int pos = -1;
			if (wrapSearch)
			{
				pos = findNext();
			}
			if (pos < 0 && showNotifications)
			{
				Toolkit.getDefaultToolkit().beep();
				String msg = ResourceMgr.getString("MsgEditorCriteriaNotFound");
				msg = StringUtil.replace(msg, "%value%", anExpression);
				WbSwingUtilities.showMessage(this.parent, msg);
			}
		}
		return this.lastSearchPos;
	}

	public boolean searchPatternMatchesSelectedText()
	{
		if (this.lastSearchPattern == null) return false;
		Matcher m = this.lastSearchPattern.matcher(this.getSelectedText());
		return m.matches();
	}

	public void dispose()
	{
		WbAction.dispose(findAction, findPreviousAction, findNextAction, replaceAction);
	}
}
