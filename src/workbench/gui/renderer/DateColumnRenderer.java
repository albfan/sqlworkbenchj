/*
 * DateColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.text.SimpleDateFormat;
import javax.swing.SwingConstants;
import workbench.log.LogMgr;

/**
 * A class to render date and timestamp values.
 * <br/>
 * The values are formatted according to the global settings.
 *
 * @author  Thomas Kellerer
 */
public class DateColumnRenderer
	extends ToolTipRenderer
{
	private final SimpleDateFormat dateFormatter;

	public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public DateColumnRenderer()
	{
		super();
		this.dateFormatter = new SimpleDateFormat(DEFAULT_FORMAT);
		this.setHorizontalAlignment(SwingConstants.RIGHT);
	}

	public DateColumnRenderer(String aDateFormat)
	{
		this();
		this.setFormat(aDateFormat);
	}

	public void setFormat(String aDateFormat)
	{
		try
		{
			synchronized (this.dateFormatter)
			{
				this.dateFormatter.applyPattern(aDateFormat);
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DateColumnRenderer.setFormat()", "Error when setting date format [" + aDateFormat + "] default format [" + DEFAULT_FORMAT + "] will be used instead", e);
			this.dateFormatter.applyPattern(DEFAULT_FORMAT);
		}
	}

	@Override
	public void prepareDisplay(Object value)
	{
		try
		{
			java.util.Date d = (java.util.Date)value;
			synchronized (this.dateFormatter)
			{
				this.displayValue = this.dateFormatter.format(d);
			}
			if (showTooltip)
			{
				this.tooltip = displayValue;
			}
			else
			{
				this.tooltip = null;
			}
		}
		catch (Throwable cc)
		{
			this.displayValue = value.toString();
			setTooltip(displayValue);
		}
	}

}
