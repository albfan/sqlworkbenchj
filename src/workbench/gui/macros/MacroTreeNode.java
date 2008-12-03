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

package workbench.gui.macros;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author support@sql-workbench.net
 */
public class MacroTreeNode
	extends DefaultMutableTreeNode
{
	private Object data;

	public MacroTreeNode(Object dataObject)
	{
		super(dataObject);
		setDataObject(dataObject);
	}

	public MacroTreeNode(Object dataObject, boolean allowsChildren)
	{
		super(dataObject, allowsChildren);
		setDataObject(dataObject);
	}

	public Object getDataObject()
	{
		return data;
	}

	public void setDataObject(Object dataObject)
	{
		this.data = dataObject;
	}
}
