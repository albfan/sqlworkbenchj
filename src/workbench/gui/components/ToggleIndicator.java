/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

import workbench.gui.renderer.ColorUtils;

/**
 *
 * @author Thomas Kellerer
 */
public class ToggleIndicator
	extends JPanel
	implements MouseListener
{
	private static final int UP = 1;
	private static final int DOWN = 2;
	private final int height;
	private final int halfArrowWidth;
	private int direction;
	private Color arrowColor = Color.BLACK;
	boolean mouseOver = false;
	private final int[] yPointsUp = new int[3];
	private final int[] yPointsDown = new int[3];

	public ToggleIndicator()
	{
		setOpaque(true);
		addMouseListener(this);

		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();

		int border = 4;
		height = (int)Math.round( (float)dpi / 14) + border;

		// make the arrow 4 times as wide as it is high
		halfArrowWidth = (height - border) * 2;

		direction = DOWN;

		// the y points for the up and down arrow will not change because they only depend on the height
		yPointsDown[0] = border / 2;
		yPointsDown[1] = height - border / 2;
		yPointsDown[2] = border / 2;

		yPointsUp[0] = height - border / 2;
		yPointsUp[1] = border / 2;
		yPointsUp[2] = height - border / 2;
	}

	public void setDirectionUp()
	{
		this.direction = UP;
		repaint();
	}

	public void setDirectionDown()
	{
		this.direction = DOWN;
		repaint();
	}

	@Override
	public void setBackground(Color bg)
	{
		super.setBackground(bg);
		arrowColor = ColorUtils.blend(bg, Color.BLACK, 132);
	}

	@Override
	public void paintComponent(Graphics g)
	{
		Graphics2D g2d = (Graphics2D) g;

		int width = getWidth();

		int[] xPoints = new int[3];
		int[] yPoints;

		int w2 = width / 2;

		xPoints[0] = w2 - halfArrowWidth;
		xPoints[1] = w2;
		xPoints[2] = w2 + halfArrowWidth;

		if (direction == DOWN)
		{
			yPoints = yPointsDown;
		}
		else
		{
			yPoints = yPointsUp;
		}
		Color bg = getBackground();
		Color fg = arrowColor;

		if (mouseOver)
		{
			bg = bg.darker();
			fg = Color.BLACK;
		}

		g.setColor(bg);
		g.fillRect(0, 0, width, height);
		g.setColor(fg);
		g.fillPolygon(xPoints, yPoints, 3);
	}

	@Override
	public Dimension getMinimumSize()
	{
		Dimension d = super.getMinimumSize();
		d.height = height;
		return d;
	}

	@Override
	public Dimension getMaximumSize()
	{
		Dimension d = super.getMaximumSize();
		d.height = height;
		return d;
	}

	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		d.height = height;
		return d;
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		mouseOver = true;
		repaint();
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		mouseOver = false;
		repaint();
	}

}
