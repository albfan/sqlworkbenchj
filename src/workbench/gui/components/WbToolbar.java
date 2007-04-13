/*
 * WbToolbar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Container;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbToolbar 
	extends JToolBar
{

	public WbToolbar()
	{
		this.setFloatable(false);
		this.setRollover(true);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
	}

	public void addNotify()
	{
		super.addNotify();
		Container p = getParent();
		if (p != null)
		{
			this.setBackground(p.getBackground());
		}
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
		return button;
	}

	public JButton add(WbAction a)
	{
		return add(a, -1);
	}
	
	public JButton add(WbAction a, int index)
	{
		JButton button = a.getToolbarButton();
		this.add(button, index);
		return button;
	}

	public void addSeparator()
	{
		this.addSeparator(this.getComponentCount());
	}

	public void addSeparator(int index)
	{
		if (isRollover())
			this.add(new WbToolbarSeparator(), index);
		else
			super.addSeparator();
	}
	
	public void addDefaultBorder()
	{
		Border b = new CompoundBorder(new EmptyBorder(1,0,1,0), new EtchedBorder());
		this.setBorder(b);
		this.setBorderPainted(true);
		this.setRollover(true);
	}
}
