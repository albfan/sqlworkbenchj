package workbench.gui.menu;

import javax.swing.JPopupMenu;
import workbench.gui.MainWindow;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.FileDiscardAction;
import workbench.gui.actions.FileOpenAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.RenameTabAction;
import workbench.gui.actions.WbAction;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.MainPanel;

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
	private RenameTabAction rename;

	/** Creates new LogPanelPopup */
	public SqlTabPopup(MainWindow aClient)
	{
		MainPanel panel = aClient.getCurrentPanel();
		if (panel instanceof SqlPanel)
		{
			this.add = new AddTabAction(aClient);
			this.add(add.getMenuItem());
		}
		this.remove = new RemoveTabAction(aClient);
		this.add(remove.getMenuItem());

		if (aClient.canRenameTab())
		{
			this.rename = new RenameTabAction(aClient);
			this.add(rename.getMenuItem());
		}


		if (panel instanceof SqlPanel)
		{
			this.addSeparator();
			FileOpenAction open = new FileOpenAction((SqlPanel)panel);
			open.removeIcon();
			this.add(open.getMenuItem());
			FileDiscardAction discard = new FileDiscardAction((SqlPanel)panel);
			discard.removeIcon();
			this.add(discard.getMenuItem());
			this.remove.setEnabled(aClient.canCloseTab());
		}
	}

	public void addAction(WbAction anAction, boolean withSep)
	{
		if (withSep) this.addSeparator();
		this.add(anAction);
	}

	public RemoveTabAction getRemoveAction() { return this.remove; }
	public AddTabAction getAddAction() { return this.add; }
}