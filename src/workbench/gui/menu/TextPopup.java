/*
 * TextPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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
 * @author  support@sql-workbench.net
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
