/*
 * ValueConverterTest.java
 * JUnit based test
 *
 * Created on April 12, 2007, 9:53 PM
 */

package workbench.util;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class ValueConverterTest 
	extends TestCase
{
	
	public ValueConverterTest(String testName)
	{
		super(testName);
	}
	
	protected void setUp() throws Exception
	{
		super.setUp();
	}
	
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
	
	public void testBooleanConvert()
		throws Exception
	{
		String value = "true";
		ValueConverter converter = new ValueConverter();
		converter.setAutoConvertBooleanNumbers(true);
		try
		{
			Object i = converter.convertValue("true", Types.INTEGER);
			assertEquals("Wrong value returned", new Integer(1), i);
			i = converter.convertValue("false", Types.INTEGER);
			assertEquals("Wrong value returned", new Integer(0), i);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		boolean exception = false;
		try
		{
			Object i = converter.convertValue("gaga", Types.INTEGER);
		}
		catch (Exception e)
		{
			exception = true;
		}
		assertEquals("Not exception thrown", true, exception);

		converter.setAutoConvertBooleanNumbers(false);
		exception = false;
		try
		{
			Object i = converter.convertValue("true", Types.INTEGER);
		}
		catch (Exception e)
		{
			exception = true;
		}
		assertEquals("Not exception thrown", true, exception);
		
	}
	
	public void testConvertValue() throws Exception
	{
		String aDate = "2007-04-01";
		ValueConverter converter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(2007, 3, 1);
		Date expResult = new java.sql.Date(c.getTime().getTime());
		Date result = converter.parseDate(aDate);
		
		assertEquals(expResult, result);
		
		Object data = converter.convertValue("42", Types.INTEGER);
		assertEquals("Number not converted", new Integer(42), data);
		
		data = converter.convertValue(aDate, Types.DATE);
		assertEquals(expResult, data);
		
		c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.MILLISECOND);
		java.sql.Date now = new java.sql.Date(c.getTime().getTime());
		data = converter.convertValue("today", Types.DATE);
		assertEquals(0, now.compareTo((java.sql.Date)data));
		
		data = converter.convertValue("now", Types.TIME);
		assertTrue(data instanceof java.sql.Time);
		
		
	}
	
}
