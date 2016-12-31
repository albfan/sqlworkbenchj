/*
 * TextFieldWidthAdjuster.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
