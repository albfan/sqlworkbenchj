/*
 * WbTextLabel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
import javax.swing.UIManager;

/**
 *  Displays a Label left aligned with no further overhead in painting
 *  (Faster then JLabel) this is used to in DwStatusBar to speed
 *  up processes that do give a lot of feedback (e.g. import)
 */
public class WbTextLabel
	extends JComponent
{
	private String text;
	private final Color textColor;
	private final int textX = 2;
	private int textY;
	
	public WbTextLabel()
	{
		this.setDoubleBuffered(true);
		this.setBackground(UIManager.getColor("Label.background")); 	
		this.textColor = UIManager.getColor("Label.foreground");
		this.setForeground(textColor);
		this.setOpaque(false);
		Font f = UIManager.getFont("Label.font");
		FontMetrics fm = this.getFontMetrics(f);
		textY = fm.getAscent() + 2;
	}
	
	public void setFont(Font f)
	{
		super.setFont(f);
		FontMetrics fm = this.getFontMetrics(f);
		textY = fm.getAscent() + 2;
	}

	public String getText() { return this.text; }
	
	public void setText(String label)
	{
		this.text = label;
		this.repaint();
	}
	
	public void paint(Graphics g)
	{
		g.setColor(this.textColor);
		g.drawString(this.text, textX, textY);
	}

}
