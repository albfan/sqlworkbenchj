/*
 * MainPanel.java
 *
 * Created on August 9, 2002, 4:15 PM
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
 * @author  tkellerer
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
	void dispose();
}
