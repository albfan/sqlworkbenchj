/*
 * WbToolbarButton.java
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
import java.awt.Insets;

import javax.swing.Action;
import javax.swing.Icon;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbToolbarButton 
	extends WbButton
{
	public static final Insets MARGIN = new Insets(1,1,1,1);
	private Icon additionalIcon;
	
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
	
	public Dimension getPreferredSize()
	{
		Dimension result = super.getPreferredSize();
		if (this.additionalIcon != null)
		{
			result.setSize(result.getWidth() + 16, result.getHeight());
		}
		return result;
	}
	
	public void setSecondIcon(Icon i)
	{
		this.additionalIcon = i;
	}
	
}
