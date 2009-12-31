/*
 * EtchedBorderTop.java
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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;

/**
 * 
 * @author Thomas Kellerer  
 */ 
public class EtchedBorderTop 
	extends AbstractBorder
{
	protected Color color;
	
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color bg = c.getBackground();
		Color light = bg.brighter();
		Color shade = bg.darker();
		
		int w = width;

		g.translate(x, y);

		g.setColor(shade);
		g.drawRect(0, 0, w-1, 0);

		g.setColor(light);
		g.drawLine(0, 1, w-1, 1);

		g.translate(-x, -y);
	}

	public Insets getBorderInsets(Component c)
	{
		return new Insets(2, 0, 0, 0);
	}
	
	public Insets getBorderInsets(Component c, Insets insets)
	{
		insets.left = insets.right = insets.bottom = 0;
		insets.top = 2; 
		return insets;
	}
	
}

