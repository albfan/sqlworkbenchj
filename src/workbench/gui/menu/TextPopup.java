/*
 * LogPanelPopup.java
 *
 * Created on November 28, 2001, 11:24 PM
 */

package workbench.gui.menu;

import javax.swing.JPopupMenu;
import workbench.gui.ClipboardSupport;
import java.awt.MenuItem;
import workbench.gui.actions.CutAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import workbench.gui.actions.ClearAction;
import workbench.gui.actions.CopyAction;
import workbench.gui.actions.PasteAction;
import workbench.gui.actions.SelectAllAction;

/**
 *
 * @author  thomas
 * @version
 */
public class TextPopup extends JPopupMenu
{
	private ClipboardSupport client;
	
	/** Creates new LogPanelPopup */
	public TextPopup(ClipboardSupport aClient)
	{
		this.add(new CutAction(aClient));
		this.add(new CopyAction(aClient));
		this.add(new PasteAction(aClient));
		this.addSeparator();
		this.add(new ClearAction(aClient));
		this.add(new SelectAllAction(aClient));
	}
	
	public void addAction(Action anAction, boolean withSep)
	{
		if (withSep) this.addSeparator();
		this.add(anAction);
	}
	
}
