/*
 * NoSelectionModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author  Thomas Kellerer
 */
public class NoSelectionModel
	implements ListSelectionModel
{

	@Override
	public void addListSelectionListener(ListSelectionListener x)
	{
	}

	@Override
	public void addSelectionInterval(int index0, int index1)
	{
	}

	@Override
	public void clearSelection()
	{
	}

	@Override
	public int getAnchorSelectionIndex()
	{
		return -1;
	}

	@Override
	public int getLeadSelectionIndex()
	{
		return -1;
	}

	/** Returns the last selected index or -1 if the selection is empty.
	 *
	 */
	@Override
	public int getMaxSelectionIndex()
	{
		return -1;
	}

	@Override
	public int getMinSelectionIndex()
	{
		return -1;
	}

	@Override
	public int getSelectionMode()
	{
		return 0;
	}

	@Override
	public boolean getValueIsAdjusting()
	{
		return false;
	}

	@Override
	public void insertIndexInterval(int index, int length, boolean before)
	{
	}

	@Override
	public boolean isSelectedIndex(int index)
	{
		return false;
	}

	@Override
	public boolean isSelectionEmpty()
	{
		return true;
	}

	@Override
	public void removeIndexInterval(int index0, int index1)
	{
	}

	@Override
	public void removeListSelectionListener(ListSelectionListener x)
	{
	}

	@Override
	public void removeSelectionInterval(int index0, int index1)
	{
	}

	@Override
	public void setAnchorSelectionIndex(int index)
	{
	}

	@Override
	public void setLeadSelectionIndex(int index)
	{
	}

	@Override
	public void setSelectionInterval(int index0, int index1)
	{
	}

	@Override
	public void setSelectionMode(int selectionMode)
	{
	}

	@Override
	public void setValueIsAdjusting(boolean valueIsAdjusting)
	{
	}

}
