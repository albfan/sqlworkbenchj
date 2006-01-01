/*
 * UnderlineBorder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;

public class UnderlineBorder extends AbstractBorder
{
	protected JTextField label;
	private Insets insets = new Insets(0, 0, 0, 0);
	public UnderlineBorder(JTextField aLabel)
	{
		this.label = aLabel;
	}
	
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
		
    Color fg = this.label.getForeground();
		g.setColor(fg);
		Font f = this.label.getFont();
		FontMetrics fm = this.label.getFontMetrics(f);
		int size = fm.stringWidth(this.label.getText());
		g.drawLine(x, y + height - 1, x + size, y + height -1);
		g.setColor(oldColor);
	}
	
	public Insets getBorderInsets(Component c)
	{
		return insets;
	}
	
	public Insets getBorderInsets(Component c, Insets i)
	{
		i.left = i.top = i.right = i.bottom = 0;
		return i;
	}
	

}

