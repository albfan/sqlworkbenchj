/*
 * GuiSettings.java
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
package workbench.resource;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.Set;

import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import workbench.db.objectcache.ObjectCacheStorage;

import workbench.gui.sql.FileReloadType;
import workbench.interfaces.ResultReceiver;
import workbench.log.LogMgr;

import workbench.util.CollectionUtil;
import workbench.util.MacOSHelper;
import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class GuiSettings
{
	public static final String PROPERTY_HILITE_ERROR_LINE = "workbench.gui.editor.execute.highlighterror";
	public static final String PROPERTY_CLOSE_ACTIVE_TAB = "workbench.gui.display.tab.closebutton.onlyactive";
	public static final String PROPERTY_SQLTAB_CLOSE_BUTTON = "workbench.gui.display.sqltab.closebutton";
	public static final String PROPERTY_RESULTTAB_CLOSE_BUTTON = "workbench.gui.display.resulttab.closebutton";
	public static final String PROPERTY_RESULTTAB_CLOSE_BUTTON_RIGHT = "workbench.gui.closebutton.right";
	public static final String PROPERTY_EXEC_SEL_ONLY = "workbench.gui.editor.execute.onlyselected";
	public static final String PROPERTY_QUICK_FILTER_REGEX = "workbench.gui.quickfilter.useregex";
	public static final String PROPERTY_COMPLETE_CHARS = "workbench.editor.completechars";
	public static final String PROPERTY_SMART_COMPLETE = "workbench.editor.smartcomplete";
	public static final String PROPERTY_EXPAND_KEYSTROKE = "workbench.editor.expand.macro.key";
	public static final String PROPERTY_EXPAND_MAXDURATION = "workbench.editor.expand.maxduration";
	public static final String PROPERTY_SHOW_RESULT_SQL = "workbench.gui.display.result.sql";
	public static final String PROPERTY_MACRO_POPUP_WKSP = "workbench.gui.macropopup.useworkspace";
	public static final String PROPERTY_MACRO_POPUP_CLOSE_ESC = "workbench.gui.macropopup.esc.closes";
	public static final String PROPERTY_MACRO_POPUP_RUN_ON_ENTER = "workbench.gui.macropopup.enter.run";

	public static final String PROP_TITLE_SHOW_WKSP = "workbench.gui.display.showpworkspace";
	public static final String PROP_TITLE_SHOW_URL = "workbench.gui.display.showurl";
	public static final String PROP_TITLE_SHOW_URL_USER = "workbench.gui.display.showurl.includeuser";
	public static final String PROP_TITLE_SHOW_PROF_GROUP = "workbench.gui.display.showprofilegroup";
	public static final String PROP_TITLE_APP_AT_END = "workbench.gui.display.name_at_end";
	public static final String PROP_TITLE_SHOW_EDITOR_FILE = "workbench.gui.display.showfilename";
	public static final String PROP_TITLE_GROUP_SEP = "workbench.gui.display.titlegroupsep";
	public static final String PROP_TITLE_GROUP_BRACKET = "workbench.gui.display.titlegroupbracket";

	public static final String PROP_FONT_ZOOM_WHEEL = "workbench.gui.fontzoom.mousewheel";
	public static final String PROP_NUMBER_ALIGN = "workbench.gui.renderer.numberalignment";

	public static final String PROPERTY_BOOKMARKS_UPDATE_ON_OPEN = "workbench.bookmarks.update.ondisplay";
	public static final String PROPERTY_BOOKMARKS_USE_WBRESULT = "workbench.bookmarks.use.wbresult";
	public static final String PROPERTY_BOOKMARKS_PARSE_PROCS = "workbench.bookmarks.parse.procs";
	public static final String PROPERTY_BOOKMARKS_PROCS_INCL_PNAME = "workbench.bookmarks.procs.parm_name";
	public static final String PROPERTY_BOOKMARKS_MAX_COL_WIDTH = "workbench.bookmarks.list.max_col_width";
	public static final String PROPERTY_BOOKMARKS_PKG_DEF_CHAR = "workbench.bookmarks.package_spec.character";

	public static final Set<String> WINDOW_TITLE_PROPS = CollectionUtil.treeSet(
		PROP_TITLE_APP_AT_END, PROP_TITLE_SHOW_WKSP, PROP_TITLE_SHOW_URL, PROP_TITLE_SHOW_PROF_GROUP,
		PROP_TITLE_SHOW_EDITOR_FILE, PROP_TITLE_GROUP_SEP, PROP_TITLE_GROUP_BRACKET);

	public static final String PROP_TABLE_HEADER_BOLD = "workbench.gui.table.header.bold";
	public static final String PROP_TABLE_HEADER_FULL_TYPE_INFO = "workbench.gui.table.header.typeinfo.full";
	public static final String PROP_WRAP_MULTILINE_RENDERER = "workbench.gui.display.multiline.renderer.wrap";
	public static final String PROP_WRAP_MULTILINE_EDITOR = "workbench.gui.display.multiline.editor.wrap";

	public static final String PROP_FILE_RELOAD_TYPE = "workbench.gui.editor.file.reloadtype";
	public static final String PROP_LOCAL_OBJECT_CACHE = "workbench.gui.completioncache.localstorage";
	public static final String PROP_LOCAL_OBJECT_CACHE_MAXAGE = PROP_LOCAL_OBJECT_CACHE + ".maxage";
	public static final String PROP_LOCAL_OBJECT_CACHE_DIR = PROP_LOCAL_OBJECT_CACHE + ".cachedir";

	public static final String PROP_USE_CURRENT_LINE_FOR_CURRENT_STMT = "workbench.gui.sql.current.line.statement";

	public static final String PROP_COPY_TEXT_DISPLAY_DLG = "workbench.gui.copy.text.displayoptions";

	public static final String PROP_BOOKMARK_PREFIX = "workbench.gui.bookmarks.";
	public static final String PROP_BOOKMARKS_SAVE_WIDTHS = PROP_BOOKMARK_PREFIX + "colwidths.save";
	public static final String PROP_BOOKMARKS_SAVE_SORT = PROP_BOOKMARK_PREFIX + "sort.save";

  public static final String PROP_PLAIN_EDITOR_WRAP = "workbench.editor.plain.wordwrap";
  public static final String PROP_GLOBAL_SEARCH_SAVE_COLWIDTHS = "workbench.gui.global.search.save.colwidths";


	public static boolean showApplyDDLHint()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.apply.ddl.hint", true);
	}

	public static boolean getSaveBookmarkColWidths()
	{
		return Settings.getInstance().getBoolProperty(PROP_BOOKMARKS_SAVE_WIDTHS, false);
	}

	public static void setSaveBookmarksColWidths(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_BOOKMARKS_SAVE_WIDTHS, flag);
	}

	public static boolean getSaveBookmarkSort()
	{
		return Settings.getInstance().getBoolProperty(PROP_BOOKMARKS_SAVE_SORT, false);
	}

	public static void setSaveBookmarksSort(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_BOOKMARKS_SAVE_SORT, flag);
	}

	public static boolean getUseResultTagForBookmarks()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_BOOKMARKS_USE_WBRESULT, false);
	}

	public static void setUseResultTagForBookmarks(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_BOOKMARKS_USE_WBRESULT, flag);
	}

	public static boolean getProcBookmarksIncludeParmName()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_BOOKMARKS_PROCS_INCL_PNAME, false);
	}

	public static void setProcBookmarksIncludeParmName(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_BOOKMARKS_PROCS_INCL_PNAME, flag);
	}

	public static boolean getParseProceduresForBookmarks()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_BOOKMARKS_PARSE_PROCS, true);
	}

	public static void setParseProceduresForBookmarks(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_BOOKMARKS_PARSE_PROCS, flag);
	}

	/**
	 * Return the prefix that should be shown for bookmarks inside Oracle's package specification.
	 */
	public static String getBookmarksPkgSpecPrefix()
	{
		return Settings.getInstance().getProperty(PROPERTY_BOOKMARKS_PKG_DEF_CHAR, "$");
	}

	public static boolean updateAllBookmarksOnOpen()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_BOOKMARKS_UPDATE_ON_OPEN, false);
	}

	/**
	 * Return the maximum width in characters for the columns in the bookmark selector window.
	 *
	 * @return the max. number of characters, 0 (or lower) means unlimited.
	 * @see #setBookmarksMaxColumnWidth(int)
	 */
	public static int getBookmarksMaxColumnWidth()
	{
		return Settings.getInstance().getIntProperty(PROPERTY_BOOKMARKS_MAX_COL_WIDTH, 60);
	}

	/**
	 * Define the maximum width in characters for the columns in the bookmark selector window.
	 *
	 * @param numChars the maximum width in characters
	 * @see #getBookmarksMaxColumnWidth()
	 */
	public static void setBookmarksMaxColumnWidth(int numChars)
	{
		Settings.getInstance().setProperty(PROPERTY_BOOKMARKS_MAX_COL_WIDTH, numChars);
	}

	/**
	 * Return the number of milliseconds before the expansion key is no longer "hot".
	 *
	 * @return the max. delay (in ms) between typing a macro name and typing the expansing key
	 * @see #getExpansionKey()
	 */
	public static int getMaxExpansionPause()
	{
		return Settings.getInstance().getIntProperty(PROPERTY_EXPAND_MAXDURATION, 350);
	}

	/**
	 * Return the keystroke that should trigger expanding a macro.
	 *
	 * The default is the space key.
	 * @see #getMaxExpansionPause()
	 */
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

	public static void setRunMacroWithEnter(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_MACRO_POPUP_RUN_ON_ENTER, flag);
	}

	public static boolean getRunMacroWithEnter()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_MACRO_POPUP_RUN_ON_ENTER, true);
	}

	public static boolean getCloseMacroPopupWithEsc()
	{
		return Settings.getInstance().getBoolProperty(PROPERTY_MACRO_POPUP_CLOSE_ESC, false);
	}

	public static void setCloseMacroPopupWithEsc(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_MACRO_POPUP_CLOSE_ESC, flag);
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
		return Settings.getInstance().getBoolProperty(PROPERTY_HILITE_ERROR_LINE, false);
	}

	public static void setHighlightErrorStatement(boolean flag)
	{
		Settings.getInstance().setProperty(PROPERTY_HILITE_ERROR_LINE, flag);
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
		return Settings.getInstance().getBoolProperty("workbench.gui.autocompletion.partialsearch", true);
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

	public static File getDefaultFileDir()
	{
		String dirName = Settings.getInstance().getProperty("workbench.gui.editor.defaultdir", null);
		if (dirName == null) return null;
		return new File(dirName);
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

  /**
   * Return the style transformation to be used to display the NULL string.
   *
   * Valid values are:
   *
   * <ul>
   * <li>0 = No style change</li>
   * <li>1 = Font.ITALIC</li>
   * <li>2 = Font.BOLD</li>
   * <li>3 = Font.BOLD + Font.ITALIC</li>
   * </ul>
   * @return the style to be used for Font.deriveFont()
   */
	public static int getDisplayNullFontStyle()
	{
    String styleValue = Settings.getInstance().getProperty("workbench.gui.renderer.null.fontstyle", null);

    if ("italic".equalsIgnoreCase(styleValue)) return Font.ITALIC;
    if ("bold".equalsIgnoreCase(styleValue)) return Font.BOLD;
    if ("bolditalic".equalsIgnoreCase(styleValue)) return Font.ITALIC + Font.BOLD;

    // 1 = Font.ITALIC
    // 2 = Font.BOLD
    // 3 = Bold + Italic
    int style = StringUtil.getIntValue(styleValue, 0);
    if (style < 0 || style > 3) return 0;

    return style;
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

	public static void setReloadType(FileReloadType type)
	{
		Settings.getInstance().setProperty(PROP_FILE_RELOAD_TYPE, type.name());
	}

	public static FileReloadType getReloadType()
	{
		String type = Settings.getInstance().getProperty(PROP_FILE_RELOAD_TYPE, "none");
		try
		{
			return FileReloadType.valueOf(type);
		}
		catch (Exception ex)
		{
			return FileReloadType.none;
		}
	}

	public static void setLocalStorageMaxAge(String duration)
	{
		Settings.getInstance().setProperty(PROP_LOCAL_OBJECT_CACHE_MAXAGE, duration);
	}

	public static String getLocalStorageMaxAge()
	{
		return Settings.getInstance().getProperty(PROP_LOCAL_OBJECT_CACHE_MAXAGE, "5d");
	}

	public static void setLocalStorageForObjectCache(ObjectCacheStorage storage)
	{
		Settings.getInstance().setProperty(PROP_LOCAL_OBJECT_CACHE, storage.name());
	}

	public static ObjectCacheStorage getLocalStorageForObjectCache()
	{
		String type = Settings.getInstance().getProperty(PROP_LOCAL_OBJECT_CACHE, ObjectCacheStorage.profile.name());
		try
		{
			return ObjectCacheStorage.valueOf(type);
		}
		catch (Exception ex)
		{
			return ObjectCacheStorage.never;
		}
	}

	public static boolean getUseTablenameAsResultName()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.data.resultname.firsttable", false);
	}

	public static void setUseTablenameAsResultName(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.data.resultname.firsttable", flag);
	}

	public static boolean getDefaultAppendResults()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.data.append.results", false);
	}

	public static void setDefaultAppendResults(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.data.append.results", flag);
	}

	public static boolean alwaysDisplayCopyAsTextDialog()
	{
		return Settings.getInstance().getBoolProperty(PROP_COPY_TEXT_DISPLAY_DLG, true);
	}

	public static void setAlwaysDisplayCopyAsTextDialog(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_COPY_TEXT_DISPLAY_DLG, flag);
	}

	public static boolean jumpToError()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.editor.errorjump", true);
	}

	public static void setJumpToError(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.editor.errorjump", flag);
	}

  public static int getMinTagLength()
  {
    return Settings.getInstance().getIntProperty("workbench.gui.profiles.tagfilter.minlength", 2);
  }

	public static boolean focusToProfileQuickFilter()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.profiles.quickfilter.initialfocus", true);
	}

	public static void setFocusToProfileQuickFilter(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.profiles.quickfilter.initialfocus", flag);
	}

  public static boolean restoreProfileSelectionBeforeFilter()
  {
    return Settings.getInstance().getBoolProperty("workbench.gui.profiles.filter.reset.restore.oldprofile", true);
  }

  public static void setRestoreProfileSelectionBeforeFilter(boolean flag)
  {
    Settings.getInstance().setProperty("workbench.gui.profiles.filter.reset.restore.oldprofile", flag);
  }

	public static boolean enableProfileQuickFilter()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.profiles.quickfilter", true);
	}

	public static void setEnableProfileQuickFilter(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.profiles.quickfilter", flag);
	}

	public static boolean getZoomFontWithMouseWheel()
	{
		return Settings.getInstance().getBoolProperty(PROP_FONT_ZOOM_WHEEL, true);
	}

	public static void setZoomFontWithMouseWheel(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_FONT_ZOOM_WHEEL, flag);
	}

	public static int getDpiThreshold()
	{
		return Settings.getInstance().getIntProperty("workbench.gui.dpi.threshold", 144);
	}

	public static boolean showScriptFinishTime()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.sql.script.showtime", false);
	}

	public static void setShowScriptFinishTime(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.sql.script.showtime", flag);
	}

	public static boolean showScriptStmtFinishTime()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.sql.script.statement.showtime", showScriptFinishTime());
	}

	public static void setShowScriptStmtFinishTime(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.sql.script.statement.showtime", flag);
	}

	public static boolean getUseStatementInCurrentLine()
	{
		return Settings.getInstance().getBoolProperty(PROP_USE_CURRENT_LINE_FOR_CURRENT_STMT, false);
	}

	public static void setUseStatementInCurrentLine(boolean flag)
	{
		Settings.getInstance().setProperty(PROP_USE_CURRENT_LINE_FOR_CURRENT_STMT, flag);
	}

	public static boolean getUseLastIfNoCurrentStmt()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.sql.uselast.alternative", getUseStatementInCurrentLine());
	}

	public static void setUseLastIfNoCurrentStmt(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.sql.uselast.alternative", flag);
	}

	public static boolean getVariablesDDEditable()
	{
		return Settings.getInstance().getBoolProperty("workbench.sql.parameter.prompt.dd.editable", false);
	}

	public static boolean cancellingVariablePromptStopsExecution()
	{
		return Settings.getInstance().getBoolProperty("workbench.sql.parameter.prompt.cancel.stops", true);
	}

  public static boolean enableErrorPromptForWbInclude()
  {
    return Settings.getInstance().getBoolProperty("workbench.gui.sql.error.include.prompt", true);
  }

  public static boolean retryForSingleStatement()
  {
    return Settings.getInstance().getBoolProperty("workbench.gui.sql.error.retry.single", false);
  }

  public static void setErrorPromptType(ErrorPromptType type)
  {
    Settings.getInstance().setProperty("workbench.gui.sql.error.prompt", type.name());
  }

  public static ErrorPromptType getErrorPromptType()
  {
    String value = Settings.getInstance().getProperty("workbench.gui.sql.error.prompt", ErrorPromptType.PromptWithErroressage.name());
    try
    {
      return ErrorPromptType.valueOf(value);
    }
    catch (Throwable th)
    {
      return ErrorPromptType.PromptWithErroressage;
    }
  }

  public static boolean allowWordWrapForErrorMessage()
  {
    return Settings.getInstance().getBoolProperty("workbench.gui.sql.error.prompt.wordwrap", false);
  }

  public static void setShowMessageInErrorContinueDialog(boolean flag)
  {
    Settings.getInstance().setProperty("workbench.sql.error.prompt.show.error", flag);
  }

	public static void setDataTooltipType(DataTooltipType type)
  {
    Settings.getInstance().setProperty("workbench.gui.data.sql.tooltip", type.name());
  }
	public static DataTooltipType getDataTooltipType()
	{
    String value = Settings.getInstance().getProperty("workbench.gui.data.sql.tooltip", DataTooltipType.full.name());
    try
    {
      return DataTooltipType.valueOf(value);
    }
    catch (Throwable th)
    {
      if ("true".equalsIgnoreCase(value)) return DataTooltipType.full;
      if ("false".equalsIgnoreCase(value)) return DataTooltipType.none;
    }
    return DataTooltipType.full;
	}

	public static boolean getTagCompletionUseContainsFilter()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.tags.dropdown.containsfilter", true);
	}

  public static boolean getSaveSearchAllColWidths()
  {
    return Settings.getInstance().getBoolProperty(PROP_GLOBAL_SEARCH_SAVE_COLWIDTHS, true);
  }

  /**
   * Returns the default ShowType to be used for the reference table navigaition.
   *
   */
  public static ResultReceiver.ShowType getDefaultShowType()
  {
    String type = Settings.getInstance().getProperty("workbench.gui.refnavigator.showtype", ResultReceiver.ShowType.showNone.name());
    try
    {
      return ResultReceiver.ShowType.valueOf(type);
    }
    catch (Throwable th)
    {
      LogMgr.logError("GuiSettings.getDefaultShowType()", "Invalid type \"" + type + "\" specified!", th);
      return ResultReceiver.ShowType.appendText;
    }
  }

  /**
   * Returns the default ShowType to be used for the reference table navigaition.
   *
   */
  public static ResultReceiver.ShowType getDefaultShowTypeNewTab()
  {
    String type = Settings.getInstance().getProperty("workbench.gui.refnavigator.showtype.newtab", ResultReceiver.ShowType.replaceText.name());
    try
    {
      return ResultReceiver.ShowType.valueOf(type);
    }
    catch (Throwable th)
    {
      LogMgr.logError("GuiSettings.getDefaultShowType()", "Invalid type \"" + type + "\" specified!", th);
      return ResultReceiver.ShowType.appendText;
    }
  }

  public static boolean installFocusManager()
  {
    return Settings.getInstance().getBoolProperty("workbench.gui.install.focusmgr", true);
  }
}
