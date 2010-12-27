/*
 * StructConverterTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.sql.SQLException;
import java.sql.Struct;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class StructConverterTest
{

	private final String timestampValue = "1980-01-02 03:04:05";
	private final String dateValue = "2010-09-08";
	private final String timeValue = "14:15:16";
	
	@Test
	public void testStructDisplay()
		throws Exception
	{
		final Struct embedded = new Struct() {
			@Override
			public String getSQLTypeName()
				throws SQLException
			{
				return "NESTED_TYPE";
			}

			@Override
			public Object[] getAttributes()
				throws SQLException
			{
				return new Object[] {new Integer(42), "Test", java.sql.Date.valueOf(dateValue) };
			}

			@Override
			public Object[] getAttributes(Map<String, Class<?>> map)
				throws SQLException
			{
				throw new UnsupportedOperationException("Not supported yet.");
			}
		};

		Struct data = new Struct() {
			@Override
			public String getSQLTypeName()
				throws SQLException
			{
				return "SOME_TYPE";
			}

			@Override
			public Object[] getAttributes()
				throws SQLException
			{
				return new Object[] {"Arthur", "Dent", java.sql.Timestamp.valueOf(timestampValue), java.sql.Time.valueOf(timeValue), embedded };
			}

			@Override
			public Object[] getAttributes(Map<String, Class<?>> map)
				throws SQLException
			{
				throw new UnsupportedOperationException("Not supported yet.");
			}
		};

		String display = StructConverter.getInstance().getStructDisplay(data);
		String nestedExpected = "NESTED_TYPE(42, 'Test', DATE '"  + dateValue + "')";
		String expected = "SOME_TYPE('Arthur', 'Dent', TIMESTAMP '" + timestampValue + "', TIME '" + timeValue + "', " + nestedExpected + ")";
		assertEquals(expected, display);
	}

}
