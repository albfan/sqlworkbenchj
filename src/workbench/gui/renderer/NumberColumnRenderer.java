/*
 * NumberColumnRenderer.java
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
package workbench.gui.renderer;

import java.math.BigDecimal;

import workbench.resource.GuiSettings;

import workbench.util.WbNumberFormatter;

/**
 * Display numeric values according to the global formatting settings.
 *
 * @author  Thomas Kellerer
 */
public class NumberColumnRenderer
	extends ToolTipRenderer
{
	private WbNumberFormatter formatter;

	public NumberColumnRenderer()
	{
		super();
		formatter = new WbNumberFormatter(4, '.');
		this.setHorizontalAlignment(GuiSettings.getNumberDataAlignment());
	}

	public NumberColumnRenderer(int maxDigits, char sep)
	{
		this(maxDigits, sep, false);
	}
	
	public NumberColumnRenderer(int maxDigits, char sep, boolean fixedDigits)
	{
		super();
		formatter = new WbNumberFormatter(maxDigits, sep, fixedDigits);
		this.setHorizontalAlignment(GuiSettings.getNumberDataAlignment());
	}

	@Override
	public void prepareDisplay(Object aValue)
	{
		try
		{
			Number n = (Number) aValue;

			displayValue = formatter.format(n);

			if (showTooltip)
			{
				// Tooltip should be unformatted to show the "real" value
				if (n instanceof BigDecimal)
				{
					tooltip = ((BigDecimal)n).toPlainString();
				}
				else
				{
					tooltip = n.toString();
				}
			}
			else
			{
				tooltip = null;
			}
		}
		catch (Throwable th)
		{
			displayValue = aValue.toString();
			tooltip = null;
		}
	}

}
