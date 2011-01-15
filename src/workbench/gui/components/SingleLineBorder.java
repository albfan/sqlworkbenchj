/*
 * DividerBorder.java
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;

/**
 *
 * @author Thomas Kellerer
 */
public class SingleLineBorder
	extends AbstractBorder
{
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int TOP = 4;
	public static final int BOTTOM = 8;
	public static final int LEFT_RIGHT = 3;

	protected int borderType;
	private Color lineColor;

	/**
	 * Creates a divider border with the specified type
	 * @param type (LEFT, RIGHT, TOP, BOTTOM)
	 */
	public SingleLineBorder(int type, Color color)
	{
		super();
		this.borderType = type;
		lineColor = color;
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();

		g.setColor(lineColor);

		if ((this.borderType & TOP) == TOP)
		{
			g.drawLine(x, y, x + width, y);
		}

		if ((this.borderType & BOTTOM) == BOTTOM)
		{
			g.drawLine(x, y + height - 2, x + width, y + height - 2);
		}

		if ((this.borderType & LEFT) == LEFT)
		{
			g.drawLine(x, y, x, y + height);
		}
		if ((this.borderType & RIGHT) == RIGHT)
		{
			g.drawLine(x + width - 1, y, x + width - 1, y + height);
		}

		g.setColor(oldColor);
	}

	public Insets getBorderInsets(Component c)
	{
		return new Insets(1, 1, 1, 1);
	}

	public Insets getBorderInsets(Component c, Insets insets)
	{
		insets.left = insets.top = insets.right = insets.bottom = 1;
		return insets;
	}
}

