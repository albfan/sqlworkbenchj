/*
 * WbToolbarButton.java
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

import java.awt.Insets;

import javax.swing.Action;
import javax.swing.Icon;

/**
 *
 * @author  info@sql-workbench.net
 */
public class WbToolbarButton extends javax.swing.JButton
{

	public static final Insets MARGIN = new Insets(1,1,1,1);

	public WbToolbarButton()
	{
		super();
	}

	public WbToolbarButton(String aText)
	{
		super(aText);
		this.setMargin(MARGIN);
	}
	public WbToolbarButton(Action a)
	{
		super(a);
		this.setText(null);
		this.setMargin(MARGIN);
	}

	public WbToolbarButton(Icon icon)
	{
		this.setIcon(icon);
		this.setText(null);
	}
	
	public void setAction(Action a)
	{
		super.setAction(a);
		this.setMargin(MARGIN);
		this.setText(null);
	}
}
