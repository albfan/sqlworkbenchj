/*
 * TransferableProfileNode.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.tree.TreePath;

/**
 * Handle drag and drop in the profile Tree
 * @author support@sql-workbench.net
 */
class TransferableProfileNode 
	implements Transferable
{
	public static final DataFlavor PROFILE_FLAVOR = new DataFlavor(TreePath.class, "ProfileTreeElement");
	private TreePath[] path;
	
	public TransferableProfileNode(TreePath[] tp)
	{
		path = tp;
	}
	
	public DataFlavor[] getTransferDataFlavors()
	{
		return new DataFlavor[] { PROFILE_FLAVOR };
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		return (flavor.getRepresentationClass() == PROFILE_FLAVOR.getRepresentationClass());
	}
	
	public synchronized Object getTransferData(DataFlavor flavor)
		throws UnsupportedFlavorException, IOException
	{
		if (isDataFlavorSupported(flavor))
		{
			return path;
		}
		else
		{
			throw new UnsupportedFlavorException(flavor);
		}
	}
}
