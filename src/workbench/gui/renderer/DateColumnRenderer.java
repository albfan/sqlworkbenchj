/*
 * DateColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.text.SimpleDateFormat;

import javax.swing.SwingConstants;

import workbench.util.StringUtil;

/**
 *
 * @author  info@sql-workbench.net
 */
public class DateColumnRenderer
	extends ToolTipRenderer
{
	private SimpleDateFormat dateFormatter;

	public static final String DEFAULT_FORMAT = "yyyy-MM-dd";
	
	public DateColumnRenderer()
	{
		this(DEFAULT_FORMAT);
	}

	public DateColumnRenderer(String aDateFormat)
	{
		if (aDateFormat == null)
		{
			aDateFormat = DEFAULT_FORMAT;
		}
		this.dateFormatter = new SimpleDateFormat(aDateFormat);
    this.setHorizontalAlignment(SwingConstants.RIGHT);
	}

	public void prepareDisplay(Object value)
	{
		// this method will not be called with a null value, so we do not need
		// to check it here!
		try
		{
			java.util.Date d = (java.util.Date)value;
			this.displayValue = this.dateFormatter.format(d);
			this.tooltip = d.toString();
		}
		catch (Throwable cc)
		{
			this.displayValue = StringUtil.EMPTY_STRING;
			this.tooltip = null;
		}
  }
	
}
