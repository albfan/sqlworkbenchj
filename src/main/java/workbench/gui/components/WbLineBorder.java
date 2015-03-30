/*
 * WbLineBorder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class WbLineBorder
	extends AbstractBorder
{
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int TOP = 4;
	public static final int BOTTOM = 8;

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
		super();
		this.type = aType;
		this.color = aColor;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
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

	@Override
	public Insets getBorderInsets(Component c)
	{
		return this.insets;
	}

	@Override
	public Insets getBorderInsets(Component c, Insets i)
	{
		i.left = i.top = i.right = i.bottom = 2;
		return insets;
	}

}

