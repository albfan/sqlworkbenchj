/*
 * RenameableTab.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.Component;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Thomas Kellerer
 */
public interface RenameableTab
{
	void setCurrentTabTitle(String newName);
	String getCurrentTabTitle();
	boolean canRenameTab();
	void addTabChangeListener(ChangeListener listener);
	Component getComponent();
}
