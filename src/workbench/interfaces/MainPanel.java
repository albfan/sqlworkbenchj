/*
 * MainPanel.java
 *
 * Created on August 9, 2002, 4:15 PM
 */

package workbench.interfaces;

import java.awt.Font;
import java.util.List;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JToolBar;
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
	void saveSettings();
	void restoreSettings();
	void showStatusMessage(String aMsg);
	void showLogMessage(String aMsg);
	void clearLog();
	void showLogPanel();
	void showResultPanel();
	void setConnection(WbConnection aConnection);
	void addToToolbar(WbAction anAction, boolean aFlag);
	void setFont(Font aFont);
	void disconnect();
}
