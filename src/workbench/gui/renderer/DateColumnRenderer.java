/*
 * DateColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
 *
 * @author  support@sql-workbench.net
 */
public class DateColumnRenderer
	extends ToolTipRenderer
{
	private SimpleDateFormat dateFormatter;

	public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	public DateColumnRenderer()
	{
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

	public void prepareDisplay(Object value)
	{
		try
		{
			java.util.Date d = (java.util.Date)value;
			synchronized (this.dateFormatter)
			{
				this.displayValue = this.dateFormatter.format(d);
			}
			tooltip = d.toString();
		}
		catch (Throwable cc)
		{
			this.displayValue = value.toString();
			tooltip = value.toString();
		}
  }
	
}
