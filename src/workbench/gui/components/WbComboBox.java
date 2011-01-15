/*
 * WbComboBox.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Dimension;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

/**
 * A JComboBox allowing a popup that is wider than the combobox itself.
 * As this is not natively supported by Java, this is done using an ugly hack
 * that is floating around on the internet, and which I have found in various places.
 * 
 * @author Thomas Kellerer
 */
public class WbComboBox
	extends JComboBox
{
	private boolean layingOut;
	private int popupWidth;

	public WbComboBox()
	{
	}

	public WbComboBox(ComboBoxModel aModel)
	{
		super(aModel);
	}

	/**
	 * Overriden to handle the popup Size
	 */
	public void doLayout()
	{
		try
		{
			layingOut = true;
			super.doLayout();
		}
		finally
		{
			layingOut = false;
		}
	}

	public void setPopupWidth(int width)
	{
		this.popupWidth = width;
	}

	/**
	 * Overriden to handle the popup Size
	 */
	public Dimension getSize()
	{
		Dimension dim = super.getSize();
		if (!layingOut && popupWidth != 0)
		{
			dim.width = popupWidth;
		}
		return dim;
	}
}
