/*
 * TransferableMacroNode.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.tree.TreePath;

/**
 * Handle drag and drop in the profile Tree
 * @author Thomas Kellerer
 */
class TransferableMacroNode 
	implements Transferable
{
	public static final DataFlavor PROFILE_FLAVOR = new DataFlavor(TreePath.class, "MacroTreeElement");
	private TreePath[] path;
	
	public TransferableMacroNode(TreePath[] tp)
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
