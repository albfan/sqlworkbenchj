/*
 * DividerBorder.java
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;

/**
 *
 * @author support@sql-workbench.net
 */
public class DividerBorder
	extends AbstractBorder
{
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int TOP = 4;
	public static final int BOTTOM = 8;
	public static final int LEFT_RIGHT = 3;

	public static final int VERTICAL_MIDDLE = 16;
	public static final int HORIZONTAL_MIDDLE = 32;

	protected int borderType;
	public static final DividerBorder BOTTOM_DIVIDER = new DividerBorder(BOTTOM);

	/**
	 * Creates a divider border with the specified type
	 * @param type (LEFT, RIGHT, TOP, BOTTOM)
	 */
	public DividerBorder(int type)
	{
		super();
		this.borderType = type;
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();

		Color bg = c.getBackground();
		Color light = bg.brighter();
		Color shade = bg.darker();

		if ((this.borderType & TOP) == TOP)
		{
			g.setColor(shade);
			g.drawLine(x, y, x + width, y);
			g.setColor(light);
			g.drawLine(x, y + 1, x  + width, y + 1);
		}

		if ((this.borderType & BOTTOM) == BOTTOM)
		{
			g.setColor(shade);
			g.drawLine(x, y + height - 2, x + width, y + height - 2);
			g.setColor(light);
			g.drawLine(x, y + height - 1, x  + width, y + height - 1);
		}

		if ((this.borderType & LEFT) == LEFT)
		{
			g.setColor(shade);
			g.drawLine(x, y, x, y + height);
			g.setColor(light);
			g.drawLine(x + 1, y, x + 1, y + height);

		}
		if ((this.borderType & RIGHT) == RIGHT)
		{
			g.setColor(shade);
			g.drawLine(x + width - 2, y, x + width - 2, y + height);
			g.setColor(light);
			g.drawLine(x + width - 1, y, x + width - 1, y + height);
		}

		if ((this.borderType & VERTICAL_MIDDLE) == VERTICAL_MIDDLE)
		{
			g.setColor(shade);
			int w2 = (int)width / 2;
			g.drawLine(x + w2, y, x + w2, y + height);
			g.setColor(light);
			g.drawLine(x + w2 + 1, y, x + w2 + 1, y + height);
		}
		if ((this.borderType & HORIZONTAL_MIDDLE) == HORIZONTAL_MIDDLE)
		{
			g.setColor(shade);
			int h2 = (int)height / 2;
			g.drawLine(0, y + h2, width, y + h2);
			g.setColor(light);
			g.drawLine(0, y + h2 + 1, width, y + h2 + 1);
		}

		g.setColor(oldColor);
	}

	public Insets getBorderInsets(Component c)
	{
		return new Insets(2, 2, 2, 2);
	}

	public Insets getBorderInsets(Component c, Insets insets)
	{
		insets.left = insets.top = insets.right = insets.bottom = 2;
		return insets;
	}

}

