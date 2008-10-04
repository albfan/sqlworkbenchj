/*
 * TablePopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.menu;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import workbench.gui.actions.ClearAction;
import workbench.gui.actions.CopyAction;
import workbench.gui.actions.CutAction;
import workbench.gui.actions.PasteAction;
import workbench.gui.actions.SelectAllAction;
import workbench.gui.actions.WbAction;
import workbench.interfaces.ClipboardSupport;

/**
 * @author support@sql-workbench.net
 */
public class TablePopup extends JPopupMenu
{
	private CopyAction copy;
	private PasteAction paste;
	private ClearAction clear;
	private SelectAllAction selectAll;
	private CutAction cut;

	public TablePopup(ClipboardSupport aClient)
	{
		super();
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

	public WbAction getCopyAction() { return this.copy; }
	public WbAction getCutAction() { return this.cut; }
	public WbAction getPasteAction() { return this.paste; }
	public WbAction getSelectAllAction() { return this.selectAll; }
	public WbAction getClearAction() { return this.clear; }
}
