/*
 * ReorderBorder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;

/**
 *
 * @author Thomas Kellerer
 */
public class ReorderBorder
	extends AbstractBorder
{
	private final int thickness = 2;
	private Color color = Color.DARK_GRAY;

	public ReorderBorder()
	{
		super();
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
		g.setColor(color);
		final int arrowLen = 4;
		for (int i=0; i < arrowLen; i++)
		{
			g.drawLine(x + i, y, x + i, y + (arrowLen-i));
		}
		g.fillRect(x, y, width, thickness);
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

