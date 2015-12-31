/*
 * WbTextLabel.java
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Displays a Label left or right aligned with no further overhead in painting
 * (Faster then JLabel) this is used in DwStatusBar to speed up processes that
 * give a lot of feedback through the status bar (e.g. WbImport)
 *
 * @author Thomas Kellerer
 */
public class WbTextLabel
	extends JComponent
	implements MouseListener
{
	private final static int DEFAULT_TEXT_Y = 15;
	private String text;
	private Color textColor;
	private int textX = 2;
	private int textY = DEFAULT_TEXT_Y;
	private int alignment = SwingConstants.LEFT;
	private FontMetrics fm;
	private boolean hasBorder;
	private Map renderingHints;
	private int minCharacters;

	public WbTextLabel()
	{
		super();
		this.setDoubleBuffered(true);
		this.setBackground(UIManager.getColor("Label.background"));
		this.textColor = UIManager.getColor("Label.foreground");
		this.setForeground(textColor);
		this.setOpaque(false);
		Font f = UIManager.getFont("Label.font");
		if (f != null)
		{
			super.setFont(f);
			this.fm = this.getFontMetrics(f);
			textY = (fm != null ? fm.getAscent() + 2 : DEFAULT_TEXT_Y);
		}
		// For some reason clicks into WbTextLabel wind up at the parent container
		// unless we capture the events ourselves
		addMouseListener(this);
		Toolkit tk = Toolkit.getDefaultToolkit();
		renderingHints = (Map)tk.getDesktopProperty("awt.font.desktophints");
	}

	public void setMininumCharacters(int min)
	{
		this.minCharacters = min;
	}

	@Override
	public Dimension getPreferredSize()
	{
		if (this.fm != null)
		{
			Rectangle2D charBounds = fm.getStringBounds("M", getGraphics());
			int charWidth = (int)charBounds.getWidth();
			int charHeight = (int)charBounds.getHeight();

			int minWidth = charWidth * minCharacters;

			int textWidth = minWidth;
			if (text != null)
			{
				Rectangle2D bounds = fm.getStringBounds(text, getGraphics());
				textWidth = (int)bounds.getWidth();
				charHeight = (int)bounds.getHeight();
			}
			textWidth = Math.max(minWidth, textWidth);
			return new Dimension(textWidth, charHeight + 2);
		}
		return super.getPreferredSize();
	}

	@Override
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	@Override
	public void setForeground(Color c)
	{
		super.setForeground(c);
		textColor = c;
	}

	@Override
	public void setBorder(Border b)
	{
		super.setBorder(b);
		hasBorder = (b != null);
	}

	public void setHorizontalAlignment(int align)
	{
		this.alignment = align;
	}

	@Override
	public void setFont(Font f)
	{
		super.setFont(f);
		this.fm = this.getFontMetrics(f);
		textY = (fm != null ? fm.getAscent() + 2 : DEFAULT_TEXT_Y);
	}

	public String getText()
	{
		return this.text;
	}

	public void setText(String label)
	{
		this.text = label;
		if (alignment == SwingConstants.RIGHT)
		{
			int w = (fm != null ? fm.stringWidth(this.text) : 15);
			textX = this.getWidth() - w - 4;
		}
		invalidate();
		repaint();
	}

	public void forcePaint()
	{
		Graphics g = getGraphics();
		if (g == null) return;
		g.setColor(getBackground());
		g.clearRect(0, 0, this.getWidth() - 4, getHeight() - 1);
		paint(g);
	}

	@Override
	public void paint(Graphics g)
	{
		if (g == null) return;
		if (hasBorder) super.paint(g);
		if (text != null)
		{
			Graphics2D g2d = (Graphics2D) g;
			if (renderingHints != null)
			{
				g2d.addRenderingHints(renderingHints);
			}
			g.setColor(this.textColor);
			g.drawString(this.text, textX, textY);
		}
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
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

}
