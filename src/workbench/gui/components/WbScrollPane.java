/*
 * WbScrollPane.java
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

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.gui.WbSwingUtilities;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbScrollPane 
	extends JScrollPane
{
	private static boolean useCustomizedBorder = true;
	
	public WbScrollPane()
	{
		super();
		this.initDefaults();
	}

	public WbScrollPane(Component view)
	{
		super(view);
		this.initDefaults();
	}
	
	public WbScrollPane(Component view, int vsbPolicy, int hsbPolicy)
	{
		super(view, vsbPolicy, hsbPolicy);
		this.initDefaults();
	}
	
	public WbScrollPane(int vsbPolicy, int hsbPolicy)
	{
		super(vsbPolicy, hsbPolicy);
		this.initDefaults();
	}

	private void initDefaults()
	{
		if (useCustomizedBorder)
		{
			try
			{
				// With some Linux distributions (Debian) creating this border during
				// initialization fails. So if we can't create our own border
				// we simply skip this for the future
				Border myBorder = new CompoundBorder(WbSwingUtilities.getBevelBorder(), new EmptyBorder(0,1,0,0));
				if (myBorder == null) 
				{
					useCustomizedBorder = false;
				}		
				else
				{
					this.setBorder(myBorder);
				}
			}
			catch (Throwable e)
			{
				useCustomizedBorder = false;
			}
		}
	}
	
}
