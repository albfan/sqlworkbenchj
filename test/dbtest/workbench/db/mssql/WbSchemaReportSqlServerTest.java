/*
 * WbOraShowTest.java
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
package workbench.db.mssql;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.WbSchemaReport;

import workbench.util.FileUtil;
import workbench.util.WbFile;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class WbSchemaReportSqlServerTest
extends WbTestCase
{
	public WbSchemaReportSqlServerTest()
	{
		super("WbSchemaReportSqlServerTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("report_test");
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull(con);

		TestUtil.executeScript(con,
			"create table foo (id integer);\n" +
			"create table bar (id integer not null primary key);\n " +
			"create view v_foo as select * from foo; \n" +
			"create view foo_view as select * from foo; \n" +
			"create schema s2; \n" +
			"create table s2.fx (id integer); \n" +
			"commit;"
			);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull(con);
		SQLServerTestUtil.dropAllObjects(con);
		TestUtil.executeScript(con,
			"drop table s2.fx;\n" +
			"drop schema s2;\n" +
			"commit;");
	}

	@Test
	public void testExecute()
		throws Exception
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		assertNotNull("No connection available", con);

		WbSchemaReport reporter = new WbSchemaReport();
		reporter.setConnection(con);

		TestUtil util = getTestUtil();
		WbFile output = util.getFile("ms_diff.xml");
		String sql = "WbSchemaReport -schemas=dbo -types=table -file='" + output.getFullPath() + "'";
		StatementRunnerResult result = reporter.execute(sql);
		assertTrue(result.getMessages().toString(), output.exists());

		String xml = FileUtil.readFile(output, "UTF-8");

		String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "2", count);

		assertTrue(output.delete());

		sql = "WbSchemaReport -types=table,view -file='" + output.getFullPath() + "'";
		result = reporter.execute(sql);
		assertTrue(result.getMessages().toString(), output.exists());

		xml = FileUtil.readFile(output, "UTF-8");

		count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "2", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def)");
		assertEquals("Incorrect view count", "2", count);

		assertTrue(output.delete());

		sql = "WbSchemaReport -includeViews=true -file='" + output.getFullPath() + "'";
		result = reporter.execute(sql);
		assertTrue(result.getMessages().toString(), output.exists());

		xml = FileUtil.readFile(output, "UTF-8");

		count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "2", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def)");
		assertEquals("Incorrect view count", "2", count);
		assertTrue(output.delete());

		sql = "WbSchemaReport -objectTypeNames=table:f* -objectTypeNames=view:v* -file='" + output.getFullPath() + "'";
		result = reporter.execute(sql);
		assertTrue(result.getMessages().toString(), output.exists());

		xml = FileUtil.readFile(output, "UTF-8");

		count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "1", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='foo'])");
		assertEquals("foo table not written", "1", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def)");
		assertEquals("Incorrect view count", "1", count);
		count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def[@name='v_foo'])");
		assertEquals("Incorrect view count", "1", count);

		assertTrue(output.delete());

		sql = "WbSchemaReport -objectTypeNames=table:dbo.f* -objectTypeNames=table:s2.f* -file='" + output.getFullPath() + "'";
		result = reporter.execute(sql);
		assertTrue(result.getMessages().toString(), output.exists());
		xml = FileUtil.readFile(output, "UTF-8");

		count = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='fx']/table-schema/text()");
		assertEquals("Wrong schema", "s2", count);

		count = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='foo']/table-schema/text()");
		assertEquals("Wrong schema", "dbo", count);

	}

}
