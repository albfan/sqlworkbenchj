/*
 * WbTextLabel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 *  Displays a Label left or right aligned with no further overhead in painting
 *  (Faster then JLabel) this is used to in DwStatusBar to speed
 *  up processes that do give a lot of feedback (e.g. import)
 * 
 * @author support@sql-workbench.net  
 */ 
public class WbTextLabel
	extends JComponent
{
	private final static int DEFAULT_TEXT_Y = 15;
	private String text;
	private final Color textColor;
	private int textX = 2;
	private int textY = DEFAULT_TEXT_Y;
	private int alignment = SwingConstants.LEFT;
	private FontMetrics fm;
	private boolean hasBorder = false;
	
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
	
	public void paint(Graphics g)
	{
		if (g == null) return;
		if (hasBorder) super.paint(g);
		if (text != null) 
		{
			g.setColor(this.textColor);
			g.drawString(this.text, textX, textY);
		}
	}

}
