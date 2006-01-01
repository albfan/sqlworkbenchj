/*
 * WbToolbarSeparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
 * @author  support@sql-workbench.net
 */
public class WbToolbarSeparator 
	extends JPanel
{

	/** Creates a new instance of ToolbarSeperator */
	public WbToolbarSeparator()
	{
		Dimension d = new Dimension(9, 18);
		this.setPreferredSize(d);
		this.setMinimumSize(d);
		this.setMaximumSize(new Dimension(9, 24));
		this.setBorder(new DividerBorder(DividerBorder.VERTICAL_MIDDLE));
	}

}
