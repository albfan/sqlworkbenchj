/*
 * LogPanelPopup.java
 *
 * Created on November 28, 2001, 11:24 PM
 */

package workbench.gui.menu;

import javax.swing.JPopupMenu;
import workbench.gui.MainWindow;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.RemoveTabAction;

/**
 *
 * @author  workbench@kellerer.org
 * @version
 */
public class SqlTabPopup extends JPopupMenu
{
	private MainWindow client;
	private AddTabAction add;
	private RemoveTabAction remove;
	
	/** Creates new LogPanelPopup */
	public SqlTabPopup(MainWindow aClient)
	{
		this.add = new AddTabAction(aClient);
		this.add(add.getMenuItem());
		this.remove = new RemoveTabAction(aClient);
		this.add(remove.getMenuItem());
	}
	
	public RemoveTabAction getRemoveAction() { return this.remove; }
	public AddTabAction getAddAction() { return this.add; }
}
