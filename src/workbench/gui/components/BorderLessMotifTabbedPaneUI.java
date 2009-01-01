/*
 * BorderLessMotifTabbedPaneUI.java
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
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.sun.java.swing.plaf.motif.MotifTabbedPaneUI;

/**
 * 
 * @author support@sql-workbench.net  
 */ 
public class BorderLessMotifTabbedPaneUI 
	extends MotifTabbedPaneUI
{
	private Color selColor;
	
	protected Insets getContentBorderInsets(int tabPlacement)
	{
		return TabbedPaneUIFactory.getBorderLessInsets(tabPlacement);
	}
	
	protected void installDefaults()
	{
		super.installDefaults();
		selColor = UIManager.getColor("TabbedPane.selected");
	}
	
	protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex)
	{
		int width = tabPane.getWidth();
		int height = tabPane.getHeight();
		Insets insets = tabPane.getInsets();
		
		int x = insets.left;
		int y = insets.top;
		int w = width - insets.right - insets.left;
		int h = height - insets.top - insets.bottom;
		
		switch(tabPlacement)
		{
			case LEFT:
				x += calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
				w -= (x - insets.left);
				break;
			case RIGHT:
				w -= calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
				break;
			case BOTTOM:
				h -= calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
				break;
			case TOP:
			default:
				y += calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
				h -= (y - insets.top);
		}
		// Fill region behind content area
		if (selColor == null)
		{
			g.setColor(tabPane.getBackground());
		}
		else
		{
			g.setColor(selColor);
		}
		g.fillRect(x,y,w,h);
		
		switch (tabPlacement)
		{
			case JTabbedPane.TOP:
				paintContentBorderTopEdge(g, tabPlacement, selectedIndex, x, y, w, h);
				break;
			case JTabbedPane.BOTTOM:
				paintContentBorderBottomEdge(g, tabPlacement, selectedIndex, x, y, w, h);
				break;
			case JTabbedPane.LEFT:
				paintContentBorderLeftEdge(g, tabPlacement, selectedIndex, x, y, w, h);
				break;
			case JTabbedPane.RIGHT:
				paintContentBorderRightEdge(g, tabPlacement, selectedIndex, x, y, w, h);
				break;
		}
	}
}
