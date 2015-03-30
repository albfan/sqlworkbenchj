/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
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
import java.awt.Insets;
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
	private int height;
	private final int halfArrowWidth;
	private int direction;
	private Color arrowColor = Color.BLACK;
	boolean mouseOver = false;
	private final int[] yPointsUp = new int[3];
	private final int[] yPointsDown = new int[3];

	public ToggleIndicator()
	{
		addMouseListener(this);

		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();

		height = (int)Math.round( (float)dpi / 16);

		// make the arrow 4 times as wide as it is high
		halfArrowWidth = (height) * 2;

		int border = 1;
		height += border * 2;

		direction = DOWN;

		// the y points for the up and down arrow will not change because they only depend on the height
		yPointsDown[0] = border;
		yPointsDown[1] = height - border;
		yPointsDown[2] = border;

		yPointsUp[0] = height - border;
		yPointsUp[1] = border;
		yPointsUp[2] = height - border;
		Dimension min = new Dimension((int)(halfArrowWidth * 2.5), height);
		setMinimumSize(min);

		Dimension max = new Dimension(Integer.MAX_VALUE, height);
		setMaximumSize(max);
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
		Insets ins = getInsets();

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
		g.fillRect(ins.left, ins.top, width - ins.right, height - ins.bottom);
		g.setColor(fg);
		g.fillPolygon(xPoints, yPoints, 3);
	}

//	@Override
//	public Dimension getPreferredSize()
//	{
//		Dimension d = super.getPreferredSize();
//		d.height = height;
//		return d;
//	}

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
