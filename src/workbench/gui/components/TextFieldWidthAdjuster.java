/*
 * TextFieldWidthAdjuster.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 *
 * @author Thomas Kellerer
 */
public class TextFieldWidthAdjuster
{

	public void adjustAllFields(JComponent container)
	{
		Component[] children = container.getComponents();
		if (children == null) return;
		for (Component c : children)
		{
			if (c instanceof JTextField)
			{
				adjustField((JTextField)c);
			}
		}
	}

	public void adjustField(JTextField field)
	{
		Font font = field.getFont();
		if (font != null)
		{
			FontMetrics fm = field.getFontMetrics(font);
			if (fm != null)
			{
				int width = fm.charWidth('M');
				Dimension preferred = field.getPreferredSize();
				int height = 0;
				if (preferred != null)
				{
					height = (int)preferred.getHeight();
				}
				else
				{
					height = fm.getLeading() + fm.getMaxDescent() + fm.getMaxAscent();
				}
				int columns = field.getColumns();
				Dimension min = new Dimension(width * columns + 1, height);
				field.setMinimumSize(min);
			}
		}
	}
}
