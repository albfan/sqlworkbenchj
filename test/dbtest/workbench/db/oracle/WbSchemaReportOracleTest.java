/*
 * WbOraShowTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.oracle;


import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.WbSchemaReport;

import workbench.util.FileUtil;
import workbench.util.WbFile;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class WbSchemaReportOracleTest
extends WbTestCase
{
	public WbSchemaReportOracleTest()
	{
		super("WbSchemaReportOracleTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"CREATE SEQUENCE seq_one;"  +
			"create table foo (id integer);\n" +
			"create table bar (id integer not null primary key);\n " +
			"create view v_foo as select * from foo;"
			);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testExecute()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		WbSchemaReport reporter = new WbSchemaReport();
		reporter.setConnection(con);

		TestUtil util = getTestUtil();
		WbFile output = util.getFile("ora_diff.xml");
		String sql = "WbSchemaReport -tables=* -types=table,sequence -file='" + output.getFullPath() + "'";
		StatementRunnerResult result = reporter.execute(sql);
		assertTrue(result.getMessageBuffer().toString(), output.exists());

		String xml = FileUtil.readFile(output, "UTF-8");

		String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "2", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def)");
		assertEquals("Incorrect sequence count", "1", count);

		assertTrue(output.delete());

		sql = "WbSchemaReport -types=table,view -file='" + output.getFullPath() + "'";
		result = reporter.execute(sql);
		assertTrue(result.getMessageBuffer().toString(), output.exists());

		xml = FileUtil.readFile(output, "UTF-8");

		count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "2", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def)");
		assertEquals("Incorrect sequence count", "0", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def)");
		assertEquals("Incorrect view count", "1", count);

		sql = "WbSchemaReport -includeSequences=true -includeViews=true -file='" + output.getFullPath() + "'";
		result = reporter.execute(sql);
		assertTrue(result.getMessageBuffer().toString(), output.exists());

		xml = FileUtil.readFile(output, "UTF-8");

		count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "2", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def)");
		assertEquals("Incorrect sequence count", "1", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def)");
		assertEquals("Incorrect view count", "1", count);
		assertTrue(output.delete());

		sql = "WbSchemaReport -types=table,view,sequence -tables=s* -file='" + output.getFullPath() + "'";
		result = reporter.execute(sql);
		assertTrue(result.getMessageBuffer().toString(), output.exists());

		xml = FileUtil.readFile(output, "UTF-8");

		count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "0", count);
		count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def)");
		assertEquals("Incorrect sequence count", "1", count);
		assertTrue(output.delete());

	}

}
