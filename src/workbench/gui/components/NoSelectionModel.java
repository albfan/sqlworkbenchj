/*
 * NoSelectionModel.java
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

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author  info@sql-workbench.net
 */
public class NoSelectionModel
	implements ListSelectionModel
{
	
	/** Creates a new instance of NoSelectionModel */
	public NoSelectionModel()
	{
	}
	
	public void addListSelectionListener(ListSelectionListener x)
	{
	}
	
	public void addSelectionInterval(int index0, int index1)
	{
	}
	
	public void clearSelection()
	{
	}
	
	public int getAnchorSelectionIndex()
	{
		return -1;
	}
	
	public int getLeadSelectionIndex()
	{
		return -1;
	}
	
	/** Returns the last selected index or -1 if the selection is empty.
	 *
	 */
	public int getMaxSelectionIndex()
	{
		return -1;
	}
	
	public int getMinSelectionIndex()
	{
		return -1;
	}
	
	public int getSelectionMode()
	{
		return 0;
	}
	
	public boolean getValueIsAdjusting()
	{
		return false;
	}
	
	public void insertIndexInterval(int index, int length, boolean before)
	{
	}
	
	public boolean isSelectedIndex(int index)
	{
		return false;
	}
	
	public boolean isSelectionEmpty()
	{
		return true;
	}
	
	public void removeIndexInterval(int index0, int index1)
	{
	}
	
	public void removeListSelectionListener(ListSelectionListener x)
	{
	}
	
	public void removeSelectionInterval(int index0, int index1)
	{
	}
	
	public void setAnchorSelectionIndex(int index)
	{
	}
	
	public void setLeadSelectionIndex(int index)
	{
	}
	
	public void setSelectionInterval(int index0, int index1)
	{
	}
	
	public void setSelectionMode(int selectionMode)
	{
	}
	
	public void setValueIsAdjusting(boolean valueIsAdjusting)
	{
	}
	
}
