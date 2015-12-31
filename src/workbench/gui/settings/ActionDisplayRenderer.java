/*
 * ActionDisplayRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.settings;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import workbench.gui.renderer.*;
import workbench.resource.GuiSettings;

/**
 * @author Thomas Kellerer
 */
public class ActionDisplayRenderer
	extends DefaultTableCellRenderer
	implements WbRenderer
{
	private boolean useAlternateColors = false;
	private Color alternateBackground = null;

	public ActionDisplayRenderer()
	{
		super();
		alternateBackground = GuiSettings.getAlternateRowColor();
		useAlternateColors = GuiSettings.getUseAlternateRowColor();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		try
		{
			ActionDisplay d = (ActionDisplay)value;
			this.setToolTipText(d.tooltip);
			boolean isAlternatingRow = this.useAlternateColors && ((row % 2) == 1);
			if (isSelected)
			{
				this.setBackground(table.getSelectionBackground());
				this.setForeground(table.getSelectionForeground());
			}
			else
			{
				this.setForeground(table.getForeground());
				if (isAlternatingRow)
				{
					this.setBackground(alternateBackground);
				}
				else
				{
					this.setBackground(table.getBackground());
				}
			}
			return super.getTableCellRendererComponent(table, d.text, isSelected, hasFocus, row, column);
		}
		catch (Exception e)
		{
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}

	}

	public int getDisplayWidth()
	{
		return getText().length();
	}

	@Override
	public String getDisplayValue()
	{
		return getText();
	}

	public void setUseAlternatingColors(boolean flag)
	{
		this.useAlternateColors = flag;
	}

	@Override
	public void prepareDisplay(Object value)
	{

	}

}
