/*
 * SortArrowIcon.java
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
import java.awt.Graphics;

import javax.swing.Icon;

public class SortArrowIcon
  implements Icon
{
  public static final int UP = 1;
  public static final int DOWN = 2;

	public static final SortArrowIcon ARROW_UP = new SortArrowIcon(UP);
	public static final SortArrowIcon ARROW_DOWN = new SortArrowIcon(DOWN);

	public static final SortArrowIcon SMALL_ARROW_UP = new SortArrowIcon(UP,6);
	public static final SortArrowIcon SMALL_ARROW_DOWN = new SortArrowIcon(DOWN,6);
	
  protected int direction;
  protected int width = 8;
  protected int height = 8;
  
  private SortArrowIcon(int dir, int size)
	{
		this.direction = dir;
		this.width = size;
		this.height = size;
	}
	
  private SortArrowIcon(int dir)
  {
    this.direction = dir;
  }
  
  public int getIconWidth()
  {
    return width;
  }
  
  public int getIconHeight()
  {
    return height;
  }
  
  public void paintIcon(Component c, Graphics g, int x, int y)
  {
    Color bg = c.getBackground();
		Color fg = c.getForeground();
    Color light = bg.brighter();
    Color shade = bg.darker();
  
    int w = width;
    int h = height;
    int m = w / 2;
    if (direction == UP)
    {
      g.setColor(shade);
      g.drawLine(x, y, x + w, y);
      g.drawLine(x, y, x + m, y + h);
      g.setColor(light);
      g.drawLine(x + w, y, x + m, y + h);
    }
    if (direction == DOWN)
    {
      g.setColor(shade);
      g.drawLine(x + m, y, x, y + h);
      g.setColor(light);
      g.drawLine(x, y + h, x + w, y + h);
      g.drawLine(x + m, y, x + w, y + h);
    }
		g.setColor(fg);
  }
}

