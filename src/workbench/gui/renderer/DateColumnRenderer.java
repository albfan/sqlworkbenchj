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
	//private HashMap displayCache = new HashMap(500);
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

  public void prepareValue(Object value)
	{
		// this method will not be called with a null value, so we do not need
		// to check it here!
		try
		{
			Date d = (Date)value;
			this.displayValue = this.dateFormatter.format(d);
			this.tooltip = d.toString();
		}
		catch (Throwable cc)
		{
			this.displayValue = StringUtil.EMPTY_STRING;
			this.tooltip = null;
		}
  }
	
	public static void main(String[] args)
	{
		SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd");
		int loops = 50000000;
		long start,end;
		Date d = new Date();
		start = System.currentTimeMillis();
		for (int i=0; i < loops; i++)
		{
			String s= form.format(d);
		}
		end = System.currentTimeMillis();
		long duration = (end - start);
		long secs = duration / 1000;
		System.out.println("dauer " + (end - start));
		System.out.println("ms/call = " + duration / loops);
		System.out.println("secs=" + secs	);
		System.out.println("calls/s " + loops/secs);
		
	}
}
