/*
 * DbDateFormatter.java
 *
 * Created on August 14, 2002, 3:36 PM
 */

package workbench.storage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import workbench.util.StringUtil;
import workbench.util.WbPersistence;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DbDateFormatter
{
	public static final String DATE_PLACEHOLDER = "%formatted_date_literal%";
	public static DbDateFormatter DEFAULT_FORMATTER = new DbDateFormatter("yyyy-MM-dd HH:mm:ss");
	
	private SimpleDateFormat formatter;
	private String format;

	private String functionCall;

	/** Creates a new instance of DbDateFormatter */
	public DbDateFormatter()
	{
	}

	public DbDateFormatter(String aFormat)
	{
		this.setFormat(aFormat);
		this.setFunctionCall(null);
	}
	
	public String getFormat()
	{
		return this.format;
	}
	public void setFormat(String aFormat)
	{
		this.format = aFormat;
		this.formatter = new SimpleDateFormat(aFormat);
	}

	public String getFunctionCall()
	{
		return this.functionCall;
	}

	public void setFunctionCall(String aCall)
	{
		this.functionCall = aCall;
	}

	public String getLiteral(Date aDate)
	{
		String dateStr;
		try
		{
			dateStr = "'" + this.formatter.format(aDate) + "'";
			if (this.functionCall != null)
			{
				dateStr = StringUtil.replace(this.functionCall, DATE_PLACEHOLDER, dateStr);
			}
		}
		catch (Throwable th)
		{
			dateStr = "'" + aDate.toString() + "'";
		}
		return dateStr;
	}

	public static void main(String args[])
	{
		HashMap m = new HashMap();
		DbDateFormatter format = new DbDateFormatter();
		format.setFormat("yyyy-MM-dd HH:mm:ss");
		format.setFunctionCall("to_date(%formatted_date_literal%, 'yyyy-MM-dd hh24:mi:ss')");

		m.put("Oracle", format);
		m.put("Oracle8", format);

		format = new DbDateFormatter();
		format.setFormat("yyyy-MM-dd HH:mm:ss");
		format.setFunctionCall(null);

		
		m.put("Microsoft SQL Server", format);
		WbPersistence.writeObject(m, "../src/workbench/storage/DateLiteralFormats.xml");

		m.put(SqlSyntaxFormatter.GENERAL_SQL, format);

	}
}
