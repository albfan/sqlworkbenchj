/*
 * SortArrowIcon.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import javax.swing.Icon;

/**
 *
 * @author Thomas Kellerer
 */
public class SortArrowIcon
	implements Icon
{
	public static final int UP = 1;
	public static final int DOWN = 2;
	public static final SortArrowIcon ARROW_UP = new SortArrowIcon(UP);
	public static final SortArrowIcon ARROW_DOWN = new SortArrowIcon(DOWN);
	public static final SortArrowIcon SMALL_ARROW_UP = new SortArrowIcon(UP, 6);
	public static final SortArrowIcon SMALL_ARROW_DOWN = new SortArrowIcon(DOWN, 6);
	protected int direction;
	protected int width = 8;
	protected int height = 8;

	private SortArrowIcon(int dir, int size)
	{
		this.direction = dir;
		this.width = size;
		this.height = size;
	}

	private SortArrowIcon(int dir)
	{
		this.direction = dir;
	}

	@Override
	public int getIconWidth()
	{
		return width;
	}

	@Override
	public int getIconHeight()
	{
		return height;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		Color bg = c.getBackground();
		Color fg = c.getForeground();
		Color light = bg.brighter();
		Color shade = bg.darker();

		int w = width;
		int h = height;
		int m = w / 2;
		if (direction == UP)
		{
			g.setColor(shade);
			g.drawLine(x, y, x + w, y);
			g.drawLine(x, y, x + m, y + h);
			g.setColor(light);
			g.drawLine(x + w, y, x + m, y + h);
		}
		if (direction == DOWN)
		{
			g.setColor(shade);
			g.drawLine(x + m, y, x, y + h);
			g.setColor(light);
			g.drawLine(x, y + h, x + w, y + h);
			g.drawLine(x + m, y, x + w, y + h);
		}
		g.setColor(fg);
	}
}

