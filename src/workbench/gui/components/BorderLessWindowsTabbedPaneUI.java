package workbench.gui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI;

public class BorderLessWindowsTabbedPaneUI extends WindowsTabbedPaneUI
{
	private Color selColor;
	
	protected Insets getContentBorderInsets(int tabPlacement)
	{
		switch (tabPlacement)
		{
			case JTabbedPane.TOP:
				return new Insets(3,1,1,1);
			case JTabbedPane.BOTTOM:
				return new Insets(1,1,3,1);
			case JTabbedPane.LEFT:
				return new Insets(1,3,1,1);
			case JTabbedPane.RIGHT:
				return new Insets(0,0,0,5);
			default:
				return new Insets(0,0,0,0);
		}
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
