/*
 * MainPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.interfaces;

import java.awt.Font;
import java.util.List;
import javax.swing.JTabbedPane;

import workbench.db.WbConnection;
import workbench.gui.actions.WbAction;
import workbench.gui.components.WbToolbar;

/**
 *
 * @author  info@sql-workbench.net
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
	String getId();
	boolean isConnected();
	boolean isBusy();
	void dispose();
}
