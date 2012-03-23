/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.util;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDateFormatter
	extends SimpleDateFormat
{
	// copied from the PostgreSQL driver
	public static final long DATE_POSITIVE_INFINITY = 9223372036825200000l;
	public static final long DATE_NEGATIVE_INFINITY = -9223372036832400000l;
	private static final StringBuffer POSITIVE_INFINITY = new StringBuffer("infinity");
	private static final StringBuffer NEGATIVE_INFINITY = new StringBuffer("-infinity");

	private boolean checkPgInfinity = true;

	public WbDateFormatter(String pattern, DateFormatSymbols formatSymbols)
	{
		super(pattern, formatSymbols);
	}

	public WbDateFormatter(String pattern, Locale locale)
	{
		super(pattern, locale);
	}

	public WbDateFormatter(String pattern)
	{
		super(pattern);
	}

	public WbDateFormatter()
	{
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos)
	{
		if (checkPgInfinity)
		{
			long dt = (date == null ? 0 : date.getTime());
			if (dt == DATE_POSITIVE_INFINITY)
			{
				return POSITIVE_INFINITY;
			}
			else if (dt == DATE_NEGATIVE_INFINITY)
			{
				return NEGATIVE_INFINITY;
			}
		}
		return super.format(date, toAppendTo, pos);
	}

	@Override
	public Date parse(String source)
		throws ParseException
	{
		if (checkPgInfinity)
		{
			if (source.equalsIgnoreCase("infinity") || source.equalsIgnoreCase("+infinity"))
			{
				return new Date(DATE_POSITIVE_INFINITY);
			}
			if (source.equalsIgnoreCase("-infinity"))
			{
				return new Date(DATE_NEGATIVE_INFINITY);
			}
		}
		return super.parse(source);
	}

	public void setCheckInfinity(boolean flag)
	{
		this.checkPgInfinity = flag;
	}
}
