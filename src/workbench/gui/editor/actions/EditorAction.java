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

import javax.swing.KeyStroke;
import workbench.gui.actions.WbAction;
import workbench.resource.ResourceMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class EditorAction
	extends WbAction
{

	protected EditorAction(String resourceKey, int key, int modifier)
	{
		super();
		init(resourceKey, key, modifier);
	}

	protected void init(String resourceKey, int key, int modifier)
	{
		setMenuText(ResourceMgr.getString(resourceKey));
		setDefaultAccelerator(KeyStroke.getKeyStroke(key, modifier));
		initializeShortcut();

	}
	
}
