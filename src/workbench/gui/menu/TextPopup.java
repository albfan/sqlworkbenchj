/*
 * TextPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.menu;


import workbench.gui.actions.ClearAction;
import workbench.gui.actions.SelectAllAction;
import workbench.gui.actions.WbAction;
import workbench.interfaces.ClipboardSupport;

/**
 * @author  support@sql-workbench.net
 */
public class TextPopup 
	extends CutCopyPastePopup
{
	private ClearAction clear;
	private SelectAllAction selectAll;
	
	public TextPopup(ClipboardSupport aClient)
	{
		super(aClient);
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
	
	public WbAction getSelectAllAction() { return this.selectAll; }
	public WbAction getClearAction() { return this.clear; }
}
