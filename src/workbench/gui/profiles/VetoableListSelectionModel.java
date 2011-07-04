/*
 * VetoableListSelectionModel
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.profiles;

import javax.swing.DefaultListSelectionModel;
import workbench.interfaces.ListSelectionControl;

/**
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
		setSelectionMode(SINGLE_SELECTION);
		this.selectionController = listControl;
	}

	@Override
	public void setSelectionInterval(int index0, int index1)
	{
		if (selectionController.canChangeSelection())
		{
			super.setSelectionInterval(index0, index1);
		}
	}

}
