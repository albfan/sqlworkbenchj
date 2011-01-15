/*
 * CutCopyPastePopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.menu;

import javax.swing.JPopupMenu;

import workbench.gui.actions.CopyAction;
import workbench.gui.actions.CutAction;
import workbench.gui.actions.PasteAction;
import workbench.gui.actions.WbAction;
import workbench.interfaces.ClipboardSupport;

/**
 * A popup menu with the usual Cut, Copy and Paste entries for text fields.
 *
 * @author  Thomas Kellerer
 */
public class CutCopyPastePopup
	extends JPopupMenu
{
	private CopyAction copy;
	private PasteAction paste;
	private CutAction cut;

	public CutCopyPastePopup(ClipboardSupport aClient)
	{
		super();
		this.cut = new CutAction(aClient);
		this.add(cut.getMenuItem());
		this.copy = new CopyAction(aClient);
		this.add(this.copy.getMenuItem());
		this.paste = new PasteAction(aClient);
		this.add(this.paste.getMenuItem());
	}

	public void addAction(WbAction anAction, boolean withSep)
	{
		if (withSep) this.addSeparator();
		this.add(anAction.getMenuItem());
	}

	public WbAction getCopyAction() { return this.copy; }
	public WbAction getCutAction() { return this.cut; }
	public WbAction getPasteAction() { return this.paste; }
}
