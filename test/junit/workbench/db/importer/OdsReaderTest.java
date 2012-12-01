/*
 * OdsReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.importer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;
import org.junit.After;
import org.junit.Before;
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

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	@Test
	public void testReader()
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.copyResourceFile(this, "data.ods");
		File input = new File(util.getBaseDir(), "data.ods");
		OdsReader reader = new OdsReader(input, 0);
		try
		{
			reader.load();
			List<String> header = reader.getHeaderColumns();
			assertNotNull(header);
			assertEquals(5, header.size());
			assertEquals("id", header.get(0));
			assertEquals("firstname", header.get(1));
			assertEquals("lastname", header.get(2));
			assertEquals("hiredate", header.get(3));
			assertEquals("salary", header.get(4));
			assertEquals(3, reader.getRowCount());

			// check first data row
			List<Object> values = reader.getRowValues(1);
			assertEquals(5, values.size());
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

			values = reader.getRowValues(2);
			assertEquals(5, values.size());
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
		}
		finally
		{
			reader.done();
		}
	}
}
