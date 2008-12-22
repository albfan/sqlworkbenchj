/*
 * WbToolbarButton.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Insets;
import javax.swing.Action;
import javax.swing.Icon;
import workbench.WbManager;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbToolbarButton 
	extends WbButton
{
	public static final Insets MARGIN = new Insets(1,1,1,1);

	public WbToolbarButton()
	{
		super();
	}

	public WbToolbarButton(String aText)
	{
		super(aText);
		init();
	}
	
	public WbToolbarButton(Action a)
	{
		super(a);
		this.setText(null);
		iconButton = true;
		init();
	}

	public WbToolbarButton(Icon icon)
	{
		super(icon);
		this.setText(null);
		init();
	}
	
	public void setAction(Action a)
	{
		super.setAction(a);
		this.setText(null);
		init();
	}
	
	private void init()
	{
		this.setMargin(MARGIN);

		// The toolbar buttons in Java 1.5 are somewhat broken
		// the distance between buttons is too small and the rollover
		// effect does not work with the windows classic look and feel.
		if (WbManager.getInstance().isJava15 && WbManager.getInstance().isWindowsClassic())
		{
			enableBasicRollover();
		}
	}
	
}
