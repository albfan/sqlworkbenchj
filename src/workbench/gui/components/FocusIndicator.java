/*
 * FocusIndicator.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.components;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class FocusIndicator 
	implements FocusListener
{
	private Border focusBorder = null;
	private Border originalBorder = null;
	private JComponent focusClient;
	private JComponent borderClient;
	private boolean borderInitialized = false;
	private Color borderColor = null;
	
	public FocusIndicator(JComponent checkFocus, JComponent border)
	{
		focusClient = checkFocus;
		checkFocus.addFocusListener(this);
		borderClient = border;
		borderColor = Settings.getInstance().getColor("workbench.gui.focusindicator.bordercolor", Color.YELLOW.brighter());
	}

	public void dispose()
	{
		if (this.focusClient != null)
		{
			focusClient.removeFocusListener(this);
		}
		
		if (this.borderClient != null && borderInitialized)
		{
			this.borderClient.setBorder(originalBorder);
		}
	}
	
	public void focusGained(FocusEvent e)
	{
		if (this.borderClient != null)
		{
			if (originalBorder == null) 
			{
				originalBorder = borderClient.getBorder();
				focusBorder = new CompoundBorder(new LineBorder(borderColor, 1), originalBorder);
				borderInitialized = true;
			}
			this.borderClient.setBorder(focusBorder);
		}
		
	}

	public void focusLost(FocusEvent e)
	{
		if (this.borderClient != null)
		{
			this.borderClient.setBorder(originalBorder);
		}
	}

}
