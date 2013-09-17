/*
 * GuiSettings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.resource;

import java.awt.Color;
import java.util.Set;

import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import workbench.util.CollectionUtil;
import workbench.util.MacOSHelper;
import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class GuiSettings
{
	public static final String PROPERTY_CLOSE_ACTIVE_TAB = "workbench.gui.display.tab.closebutton.onlyactive";
	public static final String PROPERTY_SQLTAB_CLOSE_BUTTON = "workbench.gui.display.sqltab.closebutton";
	public static final String PROPERTY_RESULTTAB_CLOSE_BUTTON = "workbench.gui.display.resulttab.closebutton";
	public static final String PROPERTY_RESULTTAB_CLOSE_BUTTON_RIGHT = "workbench.gui.closebutton.right";
	public static final String PROPERTY_ALLOW_ALTER_TABLE = "workbench.dbexplorer.allow.alter";
	public static final String PROPERTY_EXEC_SEL_ONLY = "workbench.gui.editor.execute.onlyselected";
	public static final String PROPERTY_QUICK_FILTER_REGEX = "workbench.gui.quickfilter.useregex";
	public static final String PROPERTY_COMPLETE_CHARS = "workbench.editor.completechars";
	public static final String PROPERTY_SMART_COMPLETE = "workbench.editor.smartcomplete";
	public static final String PROPERTY_EXPAND_KEYSTROKE = "workbench.editor.expand.macro.key";
	public static final String PROPERTY_EXPAND_MAXDURATION = "workbench.editor.expand.maxduration";
	public static final String PROPERTY_SHOW_RESULT_SQL = "workbench.gui.display.result.sql";
	public static final String PROPERTY_MACRO_POPUP_WKSP = "workbench.gui.macropopup.useworkspace";

	public static final String PROP_TITLE_SHOW_WKSP = "workbench.gui.display.showpworkspace";
	public static final String PROP_TITLE_SHOW_URL = "workbench.gui.display.showurl";
	public static final String PROP_TITLE_SHOW_URL_USER = "workbench.gui.display.showurl.includeuser";
	public static final String PROP_TITLE_SHOW_PROF_GROUP = "workbench.gui.display.showprofilegroup";
	public static final String PROP_TITLE_APP_AT_END = "workbench.gui.display.name_at_end";
	public static final String PROP_TITLE_SHOW_EDITOR_FILE = "workbench.gui.display.showfilename";
	public static final String PROP_TITLE_GROUP_SEP = "workbench.gui.display.titlegroupsep";
	public static final String PROP_TITLE_GROUP_BRACKET = "workbench.gui.display.titlegroupbracket";

	public static final String PROP_NUMBER_ALIGN = "workbench.gui.renderer.numberalignment";


	public static final Set<String> WINDOW_TITLE_PROPS = CollectionUtil.treeSet(
		PROP_TITLE_APP_AT_END, PROP_TITLE_SHOW_WKSP, PROP_TITLE_SHOW_URL, PROP_TITLE_SHOW_PROF_GROUP,
		PROP_TITLE_SHOW_EDITOR_FILE, PROP_TITLE_GROUP_SEP, PROP_TITLE_GROUP_BRACKET);

	public static final String PROP_DBEXP_USE_SQLSORT = "workbench.dbexplorer.datapanel.applysqlorder";
	public static final String PROP_TABLE_HEADER_BOLD = "workbench.gui.table.header.bold";
	public static final String PROP_TABLE_HEADER_FULL_TYPE_INFO = "workbench.gui.table.header.typeinfo.full";
	public static final String PROP_WRAP_MULTILINE_RENDERER = "workbench.gui.display.multiline.renderer.wrap";
	public static final String PROP_WRAP_MULTILINE_EDITOR = "workbench.gui.display.multiline.editor.wrap";
	public static final String PROP_DBEXP_TABLE_HISTORY = "workbench.dbexplorer.tablelist.history";

	public static int getMaxExpansionPause()
	{
		return Settings.getInstance().getIntProperty(PROPERTY_EXPAND_MAXDURATION, 350);
	}


	public static KeyStroke getExpansionKey()
	{
		String value = Settings.getInstance().getProperty(PROPERTY_EXPAND_KEYSTROKE, "32,0");
		String[] elements = value.split(",");
		int code = Integer.valueOf(elements[0]);
		int modifier = Integer.valueOf(elements[1]);
		return KeyStroke.getKeyStroke(code, modifier);
	}

	public static void setExpansionKey(KeyStroke key)
	{
		int code = key.getKeyCode();
		int modifier = key.getModifiers();
		Settings.getInstance().setProperty(PROPERTY_EXPAND_KEYSTROKE, Integer.toString(code) + "," + Integer.toString(modifier));
	}

	public static boolean getStoreMacroPopupInWorkspace()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_MACRO_POPUP_WKSP, false);
	}

	public static void setStoreMacroPopupInWorkspace(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_MACRO_POPUP_WKSP, flag);
	}

	public static boolean getShowResultSQL()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_SHOW_RESULT_SQL, false);
	}

	public static void setShowResultSQL(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_SHOW_RESULT_SQL, flag);
	}

	public static boolean getHighlightErrorStatement()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.editor.execute.highlighterror", true);
	}

	public static void setHighlightErrorStatement(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.editor.execute.highlighterror", flag);
	}

	public static boolean getAutoRetrieveFKTree()
	{
		return Settings.getInstance().getBoolProperty("workbench.dbexplorer.fktree.autoload", true);
	}

	public static void setAutorRetrieveFKTree(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.dbexplorer.fktree.autoload", flag);
	}

	public static boolean getDisableEditorDuringExecution()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.editor.exec.disable", true);
	}

	public static void setDisableEditorDuringExecution(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.editor.exec.disable", flag);
	}

	public static boolean getUseRegexInQuickFilter()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_QUICK_FILTER_REGEX, true);
	}

	public static void setUseRegexInQuickFilter(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_QUICK_FILTER_REGEX, flag);
	}

	public static boolean getUseShellFolders()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.filechooser.useshellfolder", true);
	}

	public static boolean getShowMaxRowsReached()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.maxrows.warning.show", true);
	}

	public static void setShowMaxRowsReached(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.maxrows.warning.show", flag);
	}

	public static int getRowNumberMargin()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.table.rownumber.margin", 1);
	}

	public static boolean getShowTableRowNumbers()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.table.rownumber.show", false);
	}

	public static void setShowTableRowNumbers(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.table.rownumber.show", flag);
	}

	public static boolean getCycleCompletionPopup()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.autocompletion.cycle.popup", true);
	}

	public static void setCycleCompletionPopup(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.autocompletion.cycle.popup", flag);
	}

	public static boolean getSortCompletionColumns()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.autocompletion.sortcolumns", true);
	}

	public static void setSortCompletionColumns(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.autocompletion.sortcolumns", flag);
	}

	public static boolean getPartialCompletionSearch()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.autocompletion.partialsearch", false);
	}

	public static void setPartialCompletionSearch(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.autocompletion.partialsearch", flag);
	}

	public static boolean getFilterCompletionSearch()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.autocompletion.filtersearch", false);
	}

	public static void setFilterCompletionSearch(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.autocompletion.filtersearch", flag);
	}

	public static boolean getRetrieveQueryComments()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.query.retrieve.comments", false);
	}

	public static void setRetrieveQueryComments(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.query.retrieve.comments", flag);
	}

	public static boolean useSystemTrayForAlert()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.script.alert.systemtray", false);
	}

	public static void setUseSystemTrayForAlert(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.script.alert.systemtray", flag);
	}

	public static boolean showScriptFinishedAlert()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.script.alert", false);
	}

	public static void setShowScriptFinishedAlert(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.script.alert", flag);
	}

	public static long getScriptFinishedAlertDuration()
	{
		String s = Settings.getInstance().getProperty("workbench.gui.script.alert.minduration", null);
		if (StringUtil.isBlank(s)) return 0;
		return Long.parseLong(s);
	}

	public static void setScriptFinishedAlertDuration(long millis)
	{
		Settings.getInstance().setProperty("workbench.gui.script.alert.minduration", Long.toString(millis));
	}

	public static boolean showSynonymTargetInDbExplorer()
	{
		return Settings.getInstance().getBoolProperty("workbench.dbexplorer.synonyms.showtarget", true);
	}

	public static void setShowSynonymTargetInDbExplorer(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.dbexplorer.synonyms.showtarget", flag);
	}

	public static boolean allowAlterInDbExplorer()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_ALLOW_ALTER_TABLE, false);
	}

	public static void setAllowAlterInDbExplorer(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_ALLOW_ALTER_TABLE, flag);
	}

	public static boolean getKeepCurrentSqlHighlight()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.keep.currentsql.selection", true);
	}

	public static void setKeepCurrentSqlHighlight(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.keep.currentsql.selection", flag);
	}

	public static int getDefaultMaxRows()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.data.maxrows", 0);
	}

	public static void setDefaultMaxRows(int rows)
	{
		Settings.getInstance().setProperty("workbench.gui.data.maxrows", rows);
	}

	public static boolean getUseLRUForTabs()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.tabs.lru", true);
	}

	public static void setUseLRUForTabs(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.tabs.lru", flag);
	}

	public static boolean getExecuteOnlySelected()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_EXEC_SEL_ONLY, false);
	}

	public static void setExecuteOnlySelected(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_EXEC_SEL_ONLY, flag);
	}

	public static boolean getFollowFileDirectory()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.editor.followfiledir", false);
	}

	public static void setFollowFileDirectory(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.editor.followfiledir", flag);
	}

	public static String getDefaultFileDir()
	{
		return Settings.getInstance().getProperty("workbench.gui.editor.defaultdir", null);
	}

	public static void setDefaultFileDir(String path)
	{
		Settings.getInstance().setProperty("workbench.gui.editor.defaultdir", path);
	}

	public static boolean getShowCloseButtonOnRightSide()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_RESULTTAB_CLOSE_BUTTON_RIGHT, true);
	}

	public static void setShowCloseButtonOnRightSide(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_RESULTTAB_CLOSE_BUTTON_RIGHT, flag);
	}

	public static boolean getCloseActiveTabOnly()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_CLOSE_ACTIVE_TAB, false);
	}

	public static void setCloseActiveTabOnly(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_CLOSE_ACTIVE_TAB, flag);
	}

	public static boolean getShowSqlTabCloseButton()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_SQLTAB_CLOSE_BUTTON, false);
	}

	public static void setShowTabCloseButton(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_SQLTAB_CLOSE_BUTTON, flag);
	}

	public static boolean getShowResultTabCloseButton()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_RESULTTAB_CLOSE_BUTTON, false);
	}

	public static void setShowResultTabCloseButton(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_RESULTTAB_CLOSE_BUTTON, flag);
	}

	public static boolean getWrapMultilineEditor()
	{
		return Settings.getInstance().getBoolProperty(PROP_WRAP_MULTILINE_EDITOR, false);
	}

	public static void setWrapMultilineEditor(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_WRAP_MULTILINE_EDITOR, flag);
	}

	public static boolean getWrapMultilineRenderer()
	{
		return Settings.getInstance().getBoolProperty(PROP_WRAP_MULTILINE_RENDERER, false);
	}

	public static void setWrapMultilineRenderer(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_WRAP_MULTILINE_RENDERER, flag);
	}


	public static int getMultiLineThreshold()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.display.multilinethreshold", 250);
	}

	public static void setMultiLineThreshold(int value)
	{
		Settings.getInstance().setProperty("workbench.gui.display.multilinethreshold", value);
	}

	public static int getDefaultFormFieldWidth()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.form.fieldwidth", 30);
	}

	public static void setDefaultFormFieldWidth(int chars)
	{
		Settings.getInstance().setProperty("workbench.gui.form.fieldwidth", chars);
	}

	public static int getDefaultFormFieldLines()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.form.fieldlines", 5);
	}

	public static void setDefaultFormFieldLines(int lines)
	{
		Settings.getInstance().setProperty("workbench.gui.form.fieldlines", lines);
	}

	public static boolean getConfirmTabClose()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.closetab.confirm", false);
	}

	public static void setConfirmTabClose(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.closetab.confirm", flag);
	}

	public static boolean getShowTabIndex()
	{
		return Settings.getInstance().getBoolProperty(Settings.PROPERTY_SHOW_TAB_INDEX, true);
	}

	public static void setShowTabIndex(boolean flag)
	{
		Settings.getInstance().setProperty(Settings.PROPERTY_SHOW_TAB_INDEX, flag);
	}

	public static boolean getIncludeHeaderInOptimalWidth()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.optimalwidth.includeheader", true);
	}

	public static void setIncludeHeaderInOptimalWidth(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.optimalwidth.includeheader", flag);
	}

	public static boolean getAutomaticOptimalRowHeight()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.optimalrowheight.automatic", false);
	}

	public static void setAutomaticOptimalRowHeight(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.optimalrowheight.automatic", flag);
	}

	public static boolean getAutomaticOptimalWidth()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.optimalwidth.automatic", true);
	}

	public static void setAutomaticOptimalWidth(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.optimalwidth.automatic", flag);
	}

	public static void setUseDynamicLayout(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.dynamiclayout", flag);
	}

	public static int getProfileDividerLocation()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.profiles.divider", -1);
	}

	public static void setProfileDividerLocation(int aValue)
	{
		Settings.getInstance().setProperty("workbench.gui.profiles.divider", Integer.toString(aValue));
	}

	public static void setMinColumnWidth(int width)
	{
		Settings.getInstance().setProperty("workbench.gui.optimalwidth.minsize", width);
	}

	public static int getMinColumnWidth()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.optimalwidth.minsize", 50);
	}

	public static int getMaxColumnWidth()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.optimalwidth.maxsize", 850);
	}

	public static void setMaxColumnWidth(int width)
	{
		Settings.getInstance().setProperty("workbench.gui.optimalwidth.maxsize", width);
	}

	public static int getAutRowHeightMaxLines()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.optimalrowheight.maxlines", 10);
	}

	public static void setAutRowHeightMaxLines(int lines)
	{
		Settings.getInstance().setProperty("workbench.gui.optimalrowheight.maxlines", lines);
	}

	public static boolean getIgnoreWhitespaceForAutoRowHeight()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.optimalrowheight.ignore.emptylines", false);
	}

	public static void setIgnoreWhitespaceForAutoRowHeight(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.optimalrowheight.ignore.emptylines", flag);
	}

	public static void setLookAndFeelClass(String aClassname)
	{
		Settings.getInstance().setProperty("workbench.gui.lookandfeelclass", aClassname);
	}

	public static String getLookAndFeelClass()
	{
		return Settings.getInstance().getProperty("workbench.gui.lookandfeelclass", "");
	}

	public static Boolean getUseBrushedMetal()
	{
		return Boolean.valueOf(Settings.getInstance().getBoolProperty("workbench.gui.macos.brushedmetal", false));
	}

	public static void setUseBrushedMetal(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.macos.brushedmetal", flag);
	}

	public static int getMaxMacrosInMenu()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.macro.maxmenuitems", 9);
	}

	public static final int SHOW_NO_FILENAME = 0;
	public static final int SHOW_FILENAME = 1;
	public static final int SHOW_FULL_PATH = 2;

	public static void setShowFilenameInWindowTitle(int type)
	{
		switch (type)
		{
			case SHOW_NO_FILENAME:
				Settings.getInstance().setProperty(PROP_TITLE_SHOW_EDITOR_FILE, "none");
				break;
			case SHOW_FILENAME:
				Settings.getInstance().setProperty(PROP_TITLE_SHOW_EDITOR_FILE, "name");
				break;
			case SHOW_FULL_PATH:
				Settings.getInstance().setProperty(PROP_TITLE_SHOW_EDITOR_FILE, "path");
				break;
		}
	}

	public static int getShowFilenameInWindowTitle()
	{
		String type = Settings.getInstance().getProperty(PROP_TITLE_SHOW_EDITOR_FILE, "none");
		if ("name".equalsIgnoreCase(type)) return SHOW_FILENAME;
		if ("path".equalsIgnoreCase(type)) return SHOW_FULL_PATH;
		return SHOW_NO_FILENAME;
	}

	public static String getTitleGroupSeparator()
	{
		String sep = Settings.getInstance().getProperty(PROP_TITLE_GROUP_SEP, "/");
		if ("XXX".equals(sep)) return "";
		return sep;
	}

	public static void setTitleGroupSeparator(String sep)
	{
		if (StringUtil.isBlank(sep)) sep = "XXX";
		Settings.getInstance().setProperty(PROP_TITLE_GROUP_SEP, sep);
	}

	public static String getTitleGroupBracket()
	{
		return Settings.getInstance().getProperty(PROP_TITLE_GROUP_BRACKET, null);
	}

	public static void setTitleGroupBracket(String bracket)
	{
		Settings.getInstance().setProperty(PROP_TITLE_GROUP_BRACKET, bracket);
	}

	public static void setShowWorkspaceInWindowTitle(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_TITLE_SHOW_WKSP, flag);
	}

	public static boolean getShowWorkspaceInWindowTitle()
	{
		return Settings.getInstance().getBoolProperty(PROP_TITLE_SHOW_WKSP, true);
	}

	public static void setShowURLinWindowTitle(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_TITLE_SHOW_URL, flag);
	}

	/**
	 * Return true if the JDBC URL should be shown in the Window title instead of the profilename
	 * @return
	 */
	public static boolean getShowURLinWindowTitle()
	{
		return Settings.getInstance().getBoolProperty(PROP_TITLE_SHOW_URL, false);
	}

	public static void setIncludeUserInTitleURL(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_TITLE_SHOW_URL_USER, flag);
	}

	public static boolean getIncludeUserInTitleURL()
	{
		return Settings.getInstance().getBoolProperty(PROP_TITLE_SHOW_URL_USER, false);
	}

	public static void setShowProfileGroupInWindowTitle(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_TITLE_SHOW_PROF_GROUP, flag);
	}

	public static boolean getShowProfileGroupInWindowTitle()
	{
		return Settings.getInstance().getBoolProperty(PROP_TITLE_SHOW_PROF_GROUP, false);
	}

	public static void setShowProductNameAtEnd(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_TITLE_APP_AT_END, flag);
	}

	public static boolean getShowProductNameAtEnd()
	{
		return Settings.getInstance().getBoolProperty(PROP_TITLE_APP_AT_END, false);
	}

	public static boolean getShowToolbar()
	{
		return Settings.getInstance().getBoolProperty(Settings.PROPERTY_SHOW_TOOLBAR, true);
	}

	public static void setShowToolbar(final boolean show)
	{
		Settings.getInstance().setProperty(Settings.PROPERTY_SHOW_TOOLBAR, show);
	}

	public static boolean getAllowRowHeightResizing()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.display.rowheightresize", false);
	}

	public static void setAllowRowHeightResizing(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.display.rowheightresize", flag);
	}

	public static boolean getUseAlternateRowColor()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.table.alternate.use", false);
	}

	public static void setUseAlternateRowColor(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.table.alternate.use", flag);
	}

	public static boolean getAlwaysEnableSaveButton()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.savebutton.always.enabled", false);
	}

	public static String getDisplayNullString()
	{
		return Settings.getInstance().getProperty("workbench.gui.renderer.nullstring", null);
	}

	public static void setDisplayNullString(String value)
	{
		if (StringUtil.isBlank(value))
		{
			Settings.getInstance().setProperty("workbench.gui.renderer.nullstring", null);
		}
		else
		{
			Settings.getInstance().setProperty("workbench.gui.renderer.nullstring", value.trim());
		}
	}

	public static void setNullColor(Color c)
	{
		Settings.getInstance().setColor("workbench.gui.table.null.color", c);
	}

	public static Color getNullColor()
	{
		return Settings.getInstance().getColor("workbench.gui.table.null.color", null);
	}

	public static void setColumnModifiedColor(Color c)
	{
		Settings.getInstance().setColor("workbench.gui.table.modified.color", c);
	}

	public static Color getColumnModifiedColor()
	{
		return Settings.getInstance().getColor("workbench.gui.table.modified.color", null);
	}

	public static Color getExpressionHighlightColor()
	{
		return Settings.getInstance().getColor("workbench.gui.table.searchhighlite.color", Color.YELLOW);
	}

	public static void setExpressionHighlightColor(Color c)
	{
		Settings.getInstance().setColor("workbench.gui.table.searchhighlite.color", c);
	}

	public static Color getAlternateRowColor()
	{
		Color defColor = (getUseAlternateRowColor() ? new Color(245,245,245) : null);
		return Settings.getInstance().getColor("workbench.gui.table.alternate.color", defColor);
	}

	public static void setAlternateRowColor(Color c)
	{
		Settings.getInstance().setColor("workbench.gui.table.alternate.color", c);
	}

	public static void setRequiredFieldColor(Color c)
	{
		Settings.getInstance().setColor("workbench.gui.edit.requiredfield.color", c);
	}

	public static Color getRequiredFieldColor()
	{
		return Settings.getInstance().getColor("workbench.gui.edit.requiredfield.color", new Color(255,100,100));
	}

	public static void setHighlightRequiredFields(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.edit.requiredfield.dohighlight", flag);
	}

	public static boolean getHighlightRequiredFields()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.edit.requiredfield.dohighlight", true);
	}

	public static void setConfirmDiscardResultSetChanges(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.edit.warn.discard.changes", flag);
	}

	public static boolean getConfirmDiscardResultSetChanges()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.edit.warn.discard.changes", false);
	}

	public static boolean getShowSelectionSummary()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.selection.summar", true);
	}

	public static void setShowSelectionSummary(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.selection.summar", flag);
	}

	public static boolean getForceRedraw()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.forcedraw", MacOSHelper.isMacOS());
	}

	public static boolean getShowMnemonics()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.showmnemonics", true);
	}

	public static boolean getTransformSequenceDisplay()
	{
		return Settings.getInstance().getBoolProperty("workbench.dbexplorer.sequence.transpose", true);
	}

	public static boolean limitMenuLength()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.limit.menu", true);
	}

	public static int maxMenuItems()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.menu.items.max", 9999);
	}

	public static int getWheelScrollLines()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.editor.wheelscroll.units", -1);
	}

	public static void setWheelScrollLines(int lines)
	{
		Settings.getInstance().setProperty("workbench.gui.editor.wheelscroll.units", lines);
	}

	public static boolean getApplySQLSortInDbExplorer()
	{
		return Settings.getInstance().getBoolProperty(PROP_DBEXP_USE_SQLSORT, false);
	}

	public static void setApplySQLSortInDbExplorer(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_DBEXP_USE_SQLSORT, flag);
	}

	public static boolean getDbExplorerShowTableHistory()
	{
		return Settings.getInstance().getBoolProperty(PROP_DBEXP_TABLE_HISTORY, true);
	}

	public static void setDbExplorerShowTableHistory(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_DBEXP_TABLE_HISTORY, flag);
	}

	public static int getNumberDataAlignment()
	{
		String align = Settings.getInstance().getProperty(PROP_NUMBER_ALIGN, "right");

		if ("left".equalsIgnoreCase(align)) return SwingConstants.LEFT;
		return SwingConstants.RIGHT;
	}

	public static void setNumberDataAlignment(String align)
	{
		if (StringUtil.isNonBlank(align))
		{
			if ("left".equalsIgnoreCase(align) || "right".equalsIgnoreCase(align))
			{
				Settings.getInstance().setProperty(PROP_NUMBER_ALIGN, align.toLowerCase());
			}
		}
	}

	public static boolean getShowMaxRowsTooltip()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.maxrows.tooltipwarning", true);
	}

	public static void setShowMaxRowsTooltip(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.maxrows.tooltipwarning", flag);
	}

	public static boolean showSelectFkValueAtTop()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.sql.show.selectfk.top", false);
	}

	public static boolean showTableHeaderInBold()
	{
		return Settings.getInstance().getBoolProperty(PROP_TABLE_HEADER_BOLD, false);
	}

	public static void setShowTableHeaderInBold(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_TABLE_HEADER_BOLD, flag);
	}
}
