/*
 * WbToolbar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbToolbar
	extends JToolBar
{
	public WbToolbar()
	{
		super();
		this.setFloatable(false);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setBorderPainted(true);
		this.setRollover(true);
	}

	public JButton add(Action a)
	{
		JButton button;

		if (a instanceof WbAction)
		{
			button = ((WbAction)a).getToolbarButton();
		}
		else
		{
			button = new WbToolbarButton(a);
		}
		this.add(button);
		button.setRolloverEnabled(true);
		return button;
	}

	public JButton add(WbAction a)
	{
		return add(a, -1);
	}

	public JButton add(WbAction a, int index)
	{
		JButton button = a.getToolbarButton();
		button.setRolloverEnabled(true);
		this.add(button, index);
		return button;
	}

	public void addSeparator()
	{
		this.addSeparator(-1);
	}

	public void addSeparator(int index)
	{
		this.add(new WbToolbarSeparator(), index);
	}

	public void addDefaultBorder()
	{
		Border b = new CompoundBorder(new EmptyBorder(1,0,1,0), BorderFactory.createEtchedBorder());
		this.setBorder(b);
		this.setBorderPainted(true);
		this.setRollover(true);
	}
}
