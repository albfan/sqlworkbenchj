/*
 * DateColumnRenderer.java
 *
 * Created on 15. Juli 2002, 20:38
 */

package workbench.gui.renderer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.swing.SwingConstants;

import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DateColumnRenderer
	extends ToolTipRenderer
{
	private SimpleDateFormat dateFormatter;

	public static final String DEFAULT_FORMAT = "yyyy-MM-dd";
	private HashMap displayCache = new HashMap(500);
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

	public void clearDisplayCache() 
	{
		this.displayCache.clear();
	}
	
  public String[] getDisplay(Object value)
	{
		Date d = null;
		String newVal = null;
		String tip = null;

		if (value == null )
		{
			return ToolTipRenderer.EMPTY_DISPLAY;
		}
		else
		{
			try
			{
				d = (Date)value;
				tip = d.toString();
				newVal = (String)this.displayCache.get(d);
				if (newVal == null)
				{
					newVal = this.dateFormatter.format(d);
					this.displayCache.put(d, newVal);
				}
			}
			catch (ClassCastException cc)
			{
				newVal = StringUtil.EMPTY_STRING;
				tip = null;
			}
		}
		displayResult[0] = newVal;
		displayResult[1] = tip;
		
		return displayResult;
  }
	
}
