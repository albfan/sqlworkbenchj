/*
 * CriteriaPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import workbench.gui.actions.WbAction;

/**
 * @author support@sql-workbench.net
 */
public interface CriteriaPanel
{
	void setFocusToEntryField();
	String getText();
	void setText(String aText);
	void addToToolbar(WbAction action, boolean atFront, boolean withSep);
	void saveSettings();
	void restoreSettings();
	void saveSettings(PropertyStorage props, String key);
	void restoreSettings(PropertyStorage props, String key);
	void setColumnList(String[] names);
}
