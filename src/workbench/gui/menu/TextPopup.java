/*
 * LogPanelPopup.java
 *
 * Created on November 28, 2001, 11:24 PM
 */

package workbench.gui.menu;

import javax.swing.JPopupMenu;
import workbench.interfaces.ClipboardSupport;
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
	private CopyAction copy;
	private PasteAction paste;
	private ClearAction clear;
	private SelectAllAction selectAll;
	private CutAction cut;
	
	/** Creates new LogPanelPopup */
	public TextPopup(ClipboardSupport aClient)
	{
		this.cut = new CutAction(aClient);
		this.add(cut);
		this.copy = new CopyAction(aClient);
		this.add(this.copy);
		this.paste = new PasteAction(aClient);
		this.add(this.paste);
		this.addSeparator();
		this.clear = new ClearAction(aClient);
		this.add(this.clear);
		this.selectAll = new SelectAllAction(aClient);
		this.add(this.selectAll);
	}
	
	public void addAction(Action anAction, boolean withSep)
	{
		if (withSep) this.addSeparator();
		this.add(anAction);
	}
	

	public Action getCopyAction() { return this.copy; }
	public Action getCutAction() { return this.cut; }
	public Action getPasteAction() { return this.paste; }
	public Action getSelectAllAction() { return this.selectAll; }
	public Action getClearAction() { return this.clear; }
}
