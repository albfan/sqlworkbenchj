/*
 * WbTextLabel.java
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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

	public void setForeground(Color c)
	{
		super.setForeground(c);
		textColor = c;
	}

	public void setBorder(Border b)
	{
		super.setBorder(b);
		hasBorder = (b != null);
	}

	public void setHorizontalAlignment(int align)
	{
		this.alignment = align;
	}

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
		validate();
	}

	public void forcePaint()
	{
		Graphics g = getGraphics();
		if (g == null) return;
		g.setColor(getBackground());
		g.clearRect(0, 0, this.getWidth() - 4, getHeight() - 1);
		paint(g);
	}

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

	public void mouseClicked(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

}
