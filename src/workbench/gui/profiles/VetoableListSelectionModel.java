/*
 * VetoableListSelectionModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.profiles;

import javax.swing.DefaultListSelectionModel;
import workbench.interfaces.ListSelectionControl;

/**
 * A list selection model that supports "cancelling" of a selection change.
 *
 * The selection mode for this model is always SINGLE_SELECTION.
 *
 * @author Thomas Kellerer
 */
public class VetoableListSelectionModel
	extends DefaultListSelectionModel
{
	private ListSelectionControl selectionController;

	public VetoableListSelectionModel(ListSelectionControl listControl)
	{
		super();
		if (listControl == null)
		{
			throw new NullPointerException("ListControl must not be null");
		}

		super.setSelectionMode(SINGLE_SELECTION);
		this.selectionController = listControl;
	}

	/**
	 * Set the new selection but ask the registered ListSelectionControl before doing so.
	 *
	 * @param newIndex the index to be selected
	 * @param ignored not used because of the single selection mode
	 *
	 * @see ListSelectionControl#canChangeSelection()
	 */
	@Override
	public void setSelectionInterval(int newIndex, int ignored)
	{
		if (selectionController.canChangeSelection())
		{
			super.setSelectionInterval(newIndex, newIndex);
		}
	}

	/**
	 * Set the selection mode to SINGLE_SELECTION.
	 *
	 * @param selectionMode must be SINGLE_SELECTIONI
	 * @throws IllegalArgumentException if selectionMode is not SINGLE_SELECTION
	 */
	@Override
	public void setSelectionMode(int selectionMode)
	{
		if (selectionMode == SINGLE_SELECTION)
		{
			super.setSelectionMode(selectionMode);
		}
		else
		{
			throw new IllegalArgumentException("Only SINGLE_SELECTION_MODE allowed");
		}
	}

	@Override
	public void setLeadSelectionIndex(int leadIndex)
	{
		if (selectionController.canChangeSelection())
		{
			super.setLeadSelectionIndex(leadIndex);
		}
	}

}
