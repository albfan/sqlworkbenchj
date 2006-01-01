/*
 * WbLineBorder.java
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;

public class WbLineBorder extends AbstractBorder
{
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int TOP = 4;
	public static final int BOTTOM = 8;
	
	public static final int ALL = LEFT + RIGHT + TOP + BOTTOM;
	
	protected int type;
	protected int thickness;
	protected Color color;
	private Insets insets = new Insets(1, 1, 1, 1);
	public WbLineBorder(int type)
	{
		this(type, Color.LIGHT_GRAY);
	}

	/**
	 * Creates a divider border with the specified type and color
	 * @param aType (LEFT, RIGHT, TOP, BOTTOM)
	 * @param aColor the thickness of the border
	 */
	public WbLineBorder(int aType, Color aColor)
	{
		this.type = aType;
		this.color = aColor;
	}
	
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
//    Color bg = c.getBackground();
//    Color light = bg.brighter();
//    Color shade = bg.darker();
		g.setColor(this.color);
		if ((this.type & TOP) == TOP)
		{
			g.drawLine(x, y, x + width, y);
		}
		if ((this.type & BOTTOM) == BOTTOM)
		{
			g.drawLine(x, y + height - 1, x  + width, y + height - 1);
		}
		if ((this.type & LEFT) == LEFT)
		{
			g.drawLine(x, y, x, y + height);
		}
		if ((this.type & RIGHT) == RIGHT)
		{
			g.drawLine(x + width, y, x + width, y + height);
		}
		g.setColor(oldColor);
	}

	public Insets getBorderInsets(Component c)
	{
		return this.insets;
	}
	
	public Insets getBorderInsets(Component c, Insets i)
	{
		i.left = i.top = i.right = i.bottom = 2;
		return insets;
	}
	
}

