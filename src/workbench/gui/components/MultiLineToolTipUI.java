/*
 * MultiLineToolTipUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.text.View;

import workbench.util.StringUtil;


/**
 * @author Thomas Kellerer
 */
public class MultiLineToolTipUI
	extends BasicToolTipUI
{
	private static final String[] EMPTY_LINES = new String[] { StringUtil.EMPTY_STRING };
	private Dimension prefSize = new Dimension();
	private String[] displayLines;
	private View view;
	private final Map renderingHints = (Map) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");

	@Override
	public void paint(Graphics g, JComponent c)
	{
		Graphics2D g2d = (Graphics2D) g;
		if (renderingHints != null)
		{
			g2d.addRenderingHints(renderingHints);
		}

		Dimension size = c.getSize();
		if (view != null)
		{
			Rectangle r = new Rectangle(size);
			view.paint(g, r);
		}
		else
		{
			FontMetrics metrics = g.getFontMetrics();
			g.setColor(c.getBackground());
			g.fillRect(0, 0, size.width, size.height);
			g.setColor(c.getForeground());
			if (this.displayLines != null)
			{
				int h = metrics.getHeight();
				int count = displayLines.length;
				for (int i=0;i< count; i++)
				{
					g2d.drawString(displayLines[i], 3, (h * (i+1)) - 2);
				}
			}
		}
	}

	@Override
	public Dimension getPreferredSize(JComponent c)
	{
		FontMetrics metrics = c.getFontMetrics(c.getFont());
		String tipText = ((JToolTip)c).getTipText();
		view = null;
		if (tipText == null)
		{
			this.displayLines = EMPTY_LINES;
		}
		else if (tipText.startsWith("<html>"))
		{
			view = BasicHTML.createHTMLView(c, tipText);
			prefSize = super.getPreferredSize(c);
		}
		else
		{
			displayLines = StringUtil.PATTERN_CRLF.split(tipText);
			int maxWidth = -1;
			for (String displayLine : displayLines)
			{
				int width = SwingUtilities.computeStringWidth(metrics, displayLine);
				maxWidth = (maxWidth < width) ? width : maxWidth;
			}
			int height = metrics.getHeight() * displayLines.length;
			prefSize.setSize(maxWidth + 6, height + 2);
		}
		return prefSize;
	}

}

