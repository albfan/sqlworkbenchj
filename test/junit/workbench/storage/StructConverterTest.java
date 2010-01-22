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
import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class StructConverterTest
	extends TestCase
{

	public StructConverterTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		super.tearDown();
	}

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
				return new Object[] {new Integer(42), new String("Test"), new java.sql.Date(1) };
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
				return new Object[] {"Arthur", "Dent", new java.sql.Timestamp(1), new java.sql.Time(1), embedded };
			}

			@Override
			public Object[] getAttributes(Map<String, Class<?>> map)
				throws SQLException
			{
				throw new UnsupportedOperationException("Not supported yet.");
			}
		};

		CharSequence display = StructConverter.getInstance().getStructDisplay(data);
		assertEquals("SOME_TYPE('Arthur', 'Dent', TIMESTAMP '1970-01-01 01:00:00', TIME '01:00:00', NESTED_TYPE(42, 'Test', DATE '1970-01-01'))", display);
	}
}
