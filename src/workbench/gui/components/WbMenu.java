/*
 * WbMenu.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.Action;
import javax.swing.JMenu;

/**
 *
 * @author  info@sql-workbench.net
 */
public class WbMenu
	extends JMenu
{
	private String parentMenuId = null;
	private boolean createSeparator = false;
	
	public WbMenu()
	{
		super();
	}
	
	public WbMenu(String aText)
	{
		super(aText);
	}

	public WbMenu(String aText, boolean b)
	{
		super(aText, b);
	}
	
	public WbMenu(Action anAction)
	{
		super(anAction);
	}

	public void setParentMenuId(String id)
	{
		this.parentMenuId = id;
	}
	public String getParentMenuId() 
	{
		return this.parentMenuId;
	}
	
	public void setCreateMenuSeparator(boolean aFlag)
	{
		this.createSeparator = aFlag;
	}
	public boolean getCreateMenuSeparator()
	{
		return this.createSeparator;
	}
	
	public void setText(String aText)
	{
		int pos = aText.indexOf('&');
		if (pos > -1)
		{
			char mnemonic = aText.charAt(pos + 1);
			aText = aText.substring(0, pos) + aText.substring(pos + 1);
			this.setMnemonic(mnemonic);
		}
		super.setText(aText);
	}	
	
}
