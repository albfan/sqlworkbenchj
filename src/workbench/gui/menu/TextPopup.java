/*
 * LogPanelPopup.java
 *
 * Created on November 28, 2001, 11:24 PM
 */

package workbench.gui.menu;

import javax.swing.JPopupMenu;

import workbench.gui.actions.ClearAction;
import workbench.gui.actions.CopyAction;
import workbench.gui.actions.CutAction;
import workbench.gui.actions.PasteAction;
import workbench.gui.actions.SelectAllAction;
import workbench.gui.actions.WbAction;
import workbench.interfaces.ClipboardSupport;

/**
 *
 * @author  workbench@kellerer.org
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
		this.add(cut.getMenuItem());
		this.copy = new CopyAction(aClient);
		this.add(this.copy.getMenuItem());
		this.paste = new PasteAction(aClient);
		this.add(this.paste.getMenuItem());
		this.addSeparator();
		this.clear = new ClearAction(aClient);
		this.add(this.clear.getMenuItem());
		this.selectAll = new SelectAllAction(aClient);
		this.add(this.selectAll.getMenuItem());
	}
	
	public void addAction(WbAction anAction, boolean withSep)
	{
		if (withSep) this.addSeparator();
		this.add(anAction.getMenuItem());
	}
	
	public WbAction getCopyAction() { return this.copy; }
	public WbAction getCutAction() { return this.cut; }
	public WbAction getPasteAction() { return this.paste; }
	public WbAction getSelectAllAction() { return this.selectAll; }
	public WbAction getClearAction() { return this.clear; }
}
