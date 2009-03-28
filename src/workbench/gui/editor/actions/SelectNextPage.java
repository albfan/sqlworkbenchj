/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */

package workbench.gui.editor.actions;

import java.awt.event.KeyEvent;

/**
 *
 * @author support@sql-workbench.net
 */
public class SelectNextPage
	extends NextPage
{

	public SelectNextPage()
	{
		super("TxtEdNxtPageSel", KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_MASK);
		select = true;
	}

}
