/*
 * WbExportPostgresTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.postgres;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import org.junit.Test;
import workbench.util.WbFile;
import static org.junit.Assert.*;
import workbench.sql.StatementRunner;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WbExportPostgresTest
	extends WbTestCase
{

	private static final String TEST_ID = "wb_export_pg";

	public WbExportPostgresTest()
	{
		super(TEST_ID);
	}


	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"create table ranges (product_id integer, start_date date, end_date date);\n" +
			"insert into ranges (product_id, start_date, end_date) values (1, '-infinity', date '2009-12-31'); \n" +
			"insert into ranges (product_id, start_date, end_date) values (1, date '2010-01-01', date '2011-12-31'); \n" +
			"insert into ranges (product_id, start_date, end_date) values (1, date '2012-01-01', 'infinity'); \n" +
			"commit;\n"
			);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testExportInfinity()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);
    
		StatementRunner runner = getTestUtil().createConnectedStatementRunner(con);

		WbFile output = new WbFile(getTestUtil().getBaseDir(), "ranges.txt");
		runner.runStatement("WbExport -file='" + output.getAbsolutePath() + "' -type=text -header=false -type=text -dateFormat='yyyy-MM-dd'");
		runner.runStatement("select start_date, end_date from ranges order by start_date");
		assertTrue(output.exists());
		List<String> lines = StringUtil.readLines(output);
		assertEquals(3, lines.size());
		List<String> elements = StringUtil.stringToList(lines.get(0), "\t");
		assertEquals(2, elements.size());
		assertEquals("-infinity", elements.get(0));
		assertEquals("2009-12-31", elements.get(1));

		elements = StringUtil.stringToList(lines.get(1), "\t");
		assertEquals(2, elements.size());
		assertEquals("2010-01-01", elements.get(0));
		assertEquals("2011-12-31", elements.get(1));

		elements = StringUtil.stringToList(lines.get(2), "\t");
		assertEquals(2, elements.size());
		assertEquals("2012-01-01", elements.get(0));
		assertEquals("infinity", elements.get(1));
	}

}
