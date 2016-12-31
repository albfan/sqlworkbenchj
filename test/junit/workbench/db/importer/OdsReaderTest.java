/*
 * OdsReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.importer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OdsReaderTest
	extends WbTestCase
{
	public OdsReaderTest()
	{
		super("OdsReaderTest");
	}

	@Test
	public void testReadSecondSheet()
		throws Exception
	{
		TestUtil util = getTestUtil();
		File input = util.copyResourceFile(this, "data.ods");
		OdsReader reader = new OdsReader(input, 1, null);
		try
		{
			reader.load();
			assertEquals(5, reader.getRowCount());
			reader.setActiveWorksheet("orders");
			assertEquals(5, reader.getRowCount());
			reader.setActiveWorksheet("person");
			assertEquals(3, reader.getRowCount());
		}
		finally
		{
			reader.done();
		}
		assertTrue(input.delete());
	}

	@Test
	public void testReader()
		throws Exception
	{
		TestUtil util = getTestUtil();
		File input = util.copyResourceFile(this, "data.ods");
		OdsReader reader = new OdsReader(input, 0, null);

		try
		{
			reader.load();
			List<String> header = reader.getHeaderColumns();
			assertNotNull(header);
			assertEquals(6, header.size());
			assertEquals("id", header.get(0));
			assertEquals("firstname", header.get(1));
			assertEquals("lastname", header.get(2));
			assertEquals("hiredate", header.get(3));
			assertEquals("salary", header.get(4));
			assertEquals("last_login", header.get(5));
			assertEquals(3, reader.getRowCount());

			// check first data row
			List<Object> values = reader.getRowValues(1);
			assertEquals(6, values.size());
			Number n = (Number)values.get(0);
			assertEquals(1, n.intValue());
			String s = (String)values.get(1);
			assertEquals("Arthur", s);
			s = (String)values.get(2);
			assertEquals("Dent", s);
			Date hire = (Date)values.get(3);
			assertNotNull(hire);
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
			String dt = fmt.format(hire);
			assertEquals("2010-06-07", dt);
			Double sal = (Double)values.get(4);
			assertNotNull(sal);
			assertEquals(4200.24, sal.doubleValue(), 0.01);

			Date ts = (Date)values.get(5);
			SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String tsv = tsFmt.format(ts);
			assertEquals("2012-04-05 16:17:18", tsv);

			values = reader.getRowValues(2);
			assertEquals(6, values.size());
			n = (Number)values.get(0);
			assertEquals(2, n.intValue());
			s = (String)values.get(1);
			assertEquals("Ford", s);
			s = (String)values.get(2);
			assertEquals("Prefect", s);
			hire = (Date)values.get(3);
			assertNotNull(hire);

			dt = fmt.format(hire);
			assertEquals("1980-07-24", dt);
			sal = (Double)values.get(4);
			assertNotNull(sal);
			assertEquals(1234.56, sal.doubleValue(), 0.01);

			ts = (Date)values.get(5);
			tsv = tsFmt.format(ts);
			assertEquals("2012-07-08 15:16:17", tsv);
		}
		finally
		{
			reader.done();
		}
		assertTrue(input.delete());
	}
}
