/*
 * MultiLineToolTipUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicToolTipUI;

import workbench.util.StringUtil;


/**
 * @author Thomas Kellerer  
 */ 
public class MultiLineToolTipUI 
	extends BasicToolTipUI
{
	private final String[] emptyLines = new String[] { StringUtil.EMPTY_STRING };
	private int maxWidth = 0;
	private Dimension prefSize = new Dimension();
	private String[] displayLines;
	
	public void paint(Graphics g, JComponent c)
	{
		FontMetrics metrics = g.getFontMetrics();
		Dimension size = c.getSize();
		g.setColor(c.getBackground());
		g.fillRect(0, 0, size.width, size.height);
		g.setColor(c.getForeground());
		if (this.displayLines != null)
		{
			int h = metrics.getHeight();
			int count = displayLines.length;
			for (int i=0;i< count; i++)
			{
				g.drawString(displayLines[i], 3, (h * (i+1)) - 2);
			}
		}
	}
	
	public Dimension getPreferredSize(JComponent c)
	{
		FontMetrics metrics = c.getFontMetrics(c.getFont());
		String tipText = ((JToolTip)c).getTipText();
		if (tipText == null)
		{
			this.displayLines = emptyLines;
		}
		else
		{
			displayLines = StringUtil.PATTERN_CRLF.split(tipText);//
			this.maxWidth = -1;
			for (int i=0; i < displayLines.length; i++)
			{
				int width = SwingUtilities.computeStringWidth(metrics,displayLines[i]);
				this.maxWidth = (this.maxWidth < width) ? width : this.maxWidth;
			} 
		}
		int height = metrics.getHeight() * displayLines.length;
		prefSize.setSize(maxWidth + 6, height + 2);
		return prefSize;
	}
	
}

