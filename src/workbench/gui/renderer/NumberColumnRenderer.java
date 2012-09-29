/*
 * NumberColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingConstants;
import workbench.util.StringUtil;
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
		this.setHorizontalAlignment(SwingConstants.RIGHT);
	}

	public NumberColumnRenderer(int maxDigits, char sep)
	{
		super();
		formatter = new WbNumberFormatter(maxDigits, sep);
		this.setHorizontalAlignment(SwingConstants.RIGHT);
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
				if (n instanceof BigDecimal)
				{
					this.tooltip = ((BigDecimal)n).toPlainString();
				}
				else
				{
					displayValue = n.toString();
				}
			}
			else
			{
				this.tooltip = null;
			}
		}
		catch (Throwable th)
		{
			displayValue = aValue.toString();
			this.tooltip = null;
		}
	}

}
