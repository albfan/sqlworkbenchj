/*
 * GuiSettings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.awt.Color;
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

	public static boolean getUseShellFolders()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.filechooser.useshellfolder", true);
	}
	
	public static boolean getShowMaxRowsReached()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.maxrows.warning.show");
	}

	public static void setShowMaxRowsReached(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.maxrows.warning.show", flag);
	}

	public static Color getMaxRowsWarningColor()
	{
		return Settings.getInstance().getColor("workbench.gui.maxrows.warning.color", Color.RED);
	}

	public static void setMaxRowsWarningColor(Color c)
	{
		Settings.getInstance().setColor("workbench.gui.maxrows.warning.color", c);
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

	public static boolean getUseAnimatedIcon()
	{
		return Settings.getInstance().getBoolProperty(Settings.PROPERTY_ANIMATED_ICONS, false);
	}

	public static void setUseAnimatedIcon(boolean flag)
	{
		Settings.getInstance().setProperty(Settings.PROPERTY_ANIMATED_ICONS, flag);
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
				Settings.getInstance().setProperty("workbench.gui.display.showfilename", "none");
				break;
			case SHOW_FILENAME:
				Settings.getInstance().setProperty("workbench.gui.display.showfilename", "name");
				break;
			case SHOW_FULL_PATH:
				Settings.getInstance().setProperty("workbench.gui.display.showfilename", "path");
				break;
		}
	}

	public static int getShowFilenameInWindowTitle()
	{
		String type = Settings.getInstance().getProperty("workbench.gui.display.showfilename", "none");
		if ("name".equalsIgnoreCase(type)) return SHOW_FILENAME;
		if ("path".equalsIgnoreCase(type)) return SHOW_FULL_PATH;
		return SHOW_NO_FILENAME;
	}

	public static String getTitleGroupSeparator()
	{
		String sep = Settings.getInstance().getProperty("workbench.gui.display.titlegroupsep", "/");
		if ("XXX".equals(sep)) return "";
		return sep;
	}

	public static void setTitleGroupSeparator(String sep)
	{
		if (StringUtil.isBlank(sep)) sep = "XXX";
		Settings.getInstance().setProperty("workbench.gui.display.titlegroupsep", sep);
	}

	public static String getTitleGroupBracket()
	{
		return Settings.getInstance().getProperty("workbench.gui.display.titlegroupbracket", null);
	}

	public static void setTitleGroupBracket(String bracket)
	{
		Settings.getInstance().setProperty("workbench.gui.display.titlegroupbracket", bracket);
	}

	public static void setShowWorkspaceInWindowTitle(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.display.showpworkspace", flag);
	}

	public static boolean getShowWorkspaceInWindowTitle()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.display.showpworkspace", true);
	}

	public static void setShowProfileGroupInWindowTitle(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.display.showprofilegroup", flag);
	}

	public static boolean getShowProfileGroupInWindowTitle()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.display.showprofilegroup", false);
	}

	public static void setShowProductNameAtEnd(boolean flag)
	{
		Settings.getInstance().setProperty("workbench.gui.display.name_at_end", flag);
	}

	public static boolean getShowProductNameAtEnd()
	{
		return Settings.getInstance().getBoolProperty("workbench.gui.display.name_at_end", false);
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

	public static void setNullColor(Color c)
	{
		Settings.getInstance().setColor("workbench.gui.table.null.color", c);
	}

	public static Color getNullColor()
	{
		return Settings.getInstance().getColor("workbench.gui.table.null.color", null);
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
}
