/*
 * StructConverterTest.java
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
