/*
 * SortArrowIcon.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 *
 * @author Thomas Kellerer
 */
public class CloseIcon
	implements Icon
{
	private final int size;
  private final BasicStroke stroke;
  private final Color foregroundColor;
  private final Color backgroundColor;

	public CloseIcon(int iconSize)
  {
    this(iconSize, null, null);
  }

	public CloseIcon(int iconSize, Color foreground, Color background)
	{
		size = iconSize;
    stroke = new BasicStroke((float)(size / 10), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
    foregroundColor = foreground;
    backgroundColor = background;
	}

	@Override
	public int getIconWidth()
	{
		return size;
	}

	@Override
	public int getIconHeight()
	{
		return size;
	}

  private Color getForeground(Component c)
  {
    if (c.isEnabled())
    {
      return foregroundColor == null ? c.getForeground() : foregroundColor;
    }
    UIDefaults def = UIManager.getDefaults();
    Color dc = def.getColor("Button.disabledForeground");
    if (dc != null)
    {
      return dc;
    }
    return c.getForeground();
  }

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
    Graphics2D g2 = (Graphics2D)g;

    int offset = (int)(size * 0.3);
    int p1 = offset;
    int p2 = size - offset;

    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    Color oldBack = g2.getBackground();
    if (backgroundColor != null)
    {
      g2.setColor(backgroundColor);
      g2.fillRect(x, y, x + (size - x), y + (size - y));
      g2.setColor(oldBack);
    }

    g2.setColor(getForeground(c));
    g2.setStroke(stroke);
    g2.drawLine(p1 + x, p1 + y, p2 + x, p2 + y);
    g2.drawLine(p2 + x, p1 + y, p1+ x, p2 + y);
	}
}

