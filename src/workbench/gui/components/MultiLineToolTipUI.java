/*
 * MultiLineToolTipUI.java
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

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicToolTipUI;

import workbench.util.StringUtil;


public class MultiLineToolTipUI extends BasicToolTipUI
{
	private int maxWidth = 0;
	private Dimension prefSize = new Dimension();
	private List displayLines = new ArrayList();
	private static Toolkit toolkit = Toolkit.getDefaultToolkit();
	
	public void paint(Graphics g, JComponent c)
	{
		FontMetrics metrics = g.getFontMetrics();
		Dimension size = c.getSize();
		g.setColor(c.getBackground());
		g.fillRect(0, 0, size.width, size.height);
		g.setColor(c.getForeground());
		if (this.displayLines != null)
		{
			int count = displayLines.size();
			for (int i=0;i< count; i++)
			{
				g.drawString((String)displayLines.get(i), 3, (metrics.getHeight()) * (i+1));
			}
		}
	}
	
	public Dimension getPreferredSize(JComponent c)
	{
		FontMetrics metrics = c.getFontMetrics(c.getFont());
		String tipText = ((JToolTip)c).getTipText();
		this.displayLines.clear();
		if (tipText == null)
		{
			displayLines.add(StringUtil.EMPTY_STRING);
		}
		else
		{
			StringUtil.getTextLines(this.displayLines, tipText);
			String line;
			this.maxWidth = -1;
			for (int i=0; i < displayLines.size(); i++)
			{
				line = (String)this.displayLines.get(i);
				int width = SwingUtilities.computeStringWidth(metrics,line);
				this.maxWidth = (this.maxWidth < width) ? width : this.maxWidth;
			} 
		}
		int height = metrics.getHeight() * displayLines.size();
		prefSize.setSize(maxWidth + 6, height + 4);
		return prefSize;
	}
	
}

