/*
 * FocusIndicator.java
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

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import workbench.resource.Settings;

/**
 * A class that draws a border around the currently focused component
 *
 * @author Thomas Kellerer
 */
public class FocusIndicator
	implements FocusListener
{
	private Border focusBorder;
	private Border noFocusBorder;
	private Border originalBorder;
	private JComponent focusClient;
	private JComponent borderClient;
	private Color borderColor;

	public FocusIndicator(JComponent focusToCheck, JComponent client)
	{
		focusClient = focusToCheck;
		focusClient.addFocusListener(this);
		borderClient = client;
		borderColor = Settings.getInstance().getColor("workbench.gui.focusindicator.bordercolor", Color.YELLOW.brighter());
		initBorder();
	}

	private void initBorder()
	{
		if (noFocusBorder == null && focusBorder == null && originalBorder == null)
		{
			originalBorder = borderClient.getBorder();
			noFocusBorder = new CompoundBorder(new EmptyBorder(1,1,1,1), originalBorder);
			borderClient.setBorder(noFocusBorder);
			focusBorder = new CompoundBorder(new LineBorder(borderColor, 1), originalBorder);
		}
	}

	public void dispose()
	{
		if (this.focusClient != null)
		{
			focusClient.removeFocusListener(this);
		}

		if (this.borderClient != null && originalBorder != null)
		{
			this.borderClient.setBorder(originalBorder);
		}
	}

	public void focusGained(FocusEvent e)
	{
		if (this.borderClient != null)
		{
			initBorder();
			this.borderClient.setBorder(focusBorder);
		}
	}

	public void focusLost(FocusEvent e)
	{
		if (this.borderClient != null)
		{
			initBorder();
			this.borderClient.setBorder(noFocusBorder);
		}
	}

}
