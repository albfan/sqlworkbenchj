/*
 * MainPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import java.awt.Font;
import java.util.List;
import java.util.Properties;
import javax.swing.JTabbedPane;

import workbench.db.WbConnection;
import workbench.gui.actions.WbAction;
import workbench.gui.components.WbToolbar;

/**
 *
 * @author  support@sql-workbench.net
 */
public interface MainPanel
{
	List getActions();
	WbToolbar getToolbar();
	void showStatusMessage(String aMsg);
	void showLogMessage(String aMsg);
	void clearLog();
	void showLogPanel();
	void showResultPanel();
	void setConnection(WbConnection aConnection);
	WbConnection getConnection();
	void addToToolbar(WbAction anAction, boolean aFlag);
	void setFont(Font aFont);
	void disconnect();
	void setTabTitle(JTabbedPane tab, int index);
	void setTabName(String name);
	String getId();
	boolean isConnected();
	boolean isBusy();
	void dispose();
	void saveSettings();
	void panelSelected();
}
