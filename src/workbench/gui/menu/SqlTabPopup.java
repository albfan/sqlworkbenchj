package workbench.gui.menu;

import javax.swing.JPopupMenu;
import workbench.gui.MainWindow;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.WbAction;

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
	
	public void addAction(WbAction anAction, boolean withSep)
	{
		if (withSep) this.addSeparator();
		this.add(anAction);
	}
	
	public RemoveTabAction getRemoveAction() { return this.remove; }
	public AddTabAction getAddAction() { return this.add; }
}
