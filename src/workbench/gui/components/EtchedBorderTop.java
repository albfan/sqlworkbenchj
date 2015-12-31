/*
 * EtchedBorderTop.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

	@Override
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

	@Override
	public Insets getBorderInsets(Component c)
	{
		return new Insets(2, 0, 0, 0);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets)
	{
		insets.left = insets.right = insets.bottom = 0;
		insets.top = 2;
		return insets;
	}

}

