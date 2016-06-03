/*
 * DateColumnRenderer.java
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
package workbench.gui.renderer;

import javax.swing.SwingConstants;

import workbench.log.LogMgr;

import workbench.util.WbDateFormatter;

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
	private final WbDateFormatter dateFormatter;

	public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public DateColumnRenderer()
	{
		super();
		this.dateFormatter = new WbDateFormatter(DEFAULT_FORMAT);
		this.setHorizontalAlignment(SwingConstants.RIGHT);
	}

	public DateColumnRenderer(String aDateFormat, boolean variableFractions)
	{
		this();
		this.setFormat(aDateFormat, variableFractions);
	}

	public final void setFormat(String aDateFormat, boolean variableFractions)
	{
		try
		{
			synchronized (this.dateFormatter)
			{
				this.dateFormatter.applyPattern(aDateFormat, variableFractions);
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
      if (value instanceof java.sql.Timestamp)
      {
        this.displayValue = this.dateFormatter.formatTimestamp((java.sql.Timestamp)value);
      }
      else if (value instanceof java.sql.Date)
      {
        this.displayValue = this.dateFormatter.formatDate((java.sql.Date)value);
      }
      else if (value instanceof java.sql.Time)
      {
        this.displayValue = this.dateFormatter.formatTime((java.sql.Time)value);
      }
  		else
      {
        this.displayValue = this.dateFormatter.formatUtilDate((java.util.Date)value);
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
