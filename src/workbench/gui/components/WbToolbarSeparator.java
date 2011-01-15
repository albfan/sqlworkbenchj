/*
 * WbToolbarSeparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Dimension;

import javax.swing.JPanel;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbToolbarSeparator
	extends JPanel
{

	public WbToolbarSeparator()
	{
		super();
		Dimension d = new Dimension(7, 16);
		setOpaque(false);
		this.setPreferredSize(d);
		this.setMinimumSize(d);
		this.setMaximumSize(new Dimension(7, 24));
		this.setBorder(new DividerBorder(DividerBorder.VERTICAL_MIDDLE));
	}

}
