/*
 * WbTextLabel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.components;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.UIManager;
import workbench.util.StringUtil;


/**
 *  Displays a Label left aligned with no further overhead in painting
 *  (Faster then JLabel) this is used to in DwStatusBar because during
 *  loading and importing the status display would be too slow.
 */
public class WbTextLabel
	extends JComponent
{
	protected String text = StringUtil.EMPTY_STRING;

	public WbTextLabel()
	{
		this.setDoubleBuffered(true);
		this.setForeground(UIManager.getColor("Label.foreground"));
		this.setBackground(UIManager.getColor("Label.background")); 	
	}
	
	public void setText(String label)
	{
		if (label == null) this.text = StringUtil.EMPTY_STRING;
		else this.text = label;
		this.repaint();
	}
	
	public String getText() { return this.text; }
	
	public void paint(Graphics g)
	{
		final int w = this.getWidth();
		final int h = this.getHeight();
		FontMetrics fm = g.getFontMetrics();
		
		final int textX = 2;
		final int textY = fm.getAscent() + 2;
		
		g.setColor(this.getForeground());
		g.drawString(this.text, textX, textY);
	}

}
