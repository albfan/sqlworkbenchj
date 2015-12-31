/*
 * WbSchemaReportTest.java
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
package workbench.sql.wbcommands;

import java.io.File;
import java.sql.SQLException;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;

import workbench.util.FileUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbSchemaReportTest
	extends WbTestCase
{
	private WbConnection source;
	private TestUtil util;

	public WbSchemaReportTest()
	{
		super("WbSchemaReportTest");
	}

	@Test
	public void testTypeFilter()
		throws Exception
	{
		try
		{
			TestUtil tutil = getTestUtil();
			WbConnection conn = tutil.getConnection();

			String script =
			"create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100), constraint positive_id check (person_id > 0));\n" +
			"create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" +
			"create table customer (customer_id integer primary key); \n" +
			"CREATE VIEW v_person AS SELECT * FROM Person;\n" +
			"CREATE VIEW customer_view AS SELECT * FROM customer;\n" +
			"CREATE sequence seq_one;\n" +
			"CREATE sequence other_seq  increment by 5;\n" +
			"commit;\n";
			TestUtil.executeScript(conn, script);

			WbSchemaReport report = new WbSchemaReport();
			report.setConnection(conn);

			File output = tutil.getFile("selected.xml");
			output.delete();

			StatementRunnerResult result = report.execute(
				"WbReport -file='" + output.getAbsolutePath() + "' \n" +
				"         -objectTypeNames='table:person,address' \n" +
				"         -objectTypeNames=view:v* \n" +
				"         -objectTypeNames=sequence:s*"
			);

			assertTrue(result.getMessages().toString(), result.isSuccess());
			assertTrue("File not created", output.exists());

			String xml = FileUtil.readFile(output, "UTF-8");

			String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
			assertEquals("Incorrect table count", "2", count);
			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='PERSON'])");
			assertEquals("PERSON table not written", "1", count);
			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='ADDRESS'])");
			assertEquals("ADDRESS table not written", "1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def)");
			assertEquals("Incorrect view count", "1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def)");
			assertEquals("Incorrect sequence count", "1", count);
			count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def[@name='SEQ_ONE'])");
			assertEquals("SEQ_ONE not written", "1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def[@name='V_PERSON'])");
			assertEquals("V_PERSON not written", "1", count);

			output.delete();
			result = report.execute(
				"WbReport -file='" + output.getAbsolutePath() + "' \n" +
				"         -objectTypeNames=table:* \n" +
				"         -objectTypeNames=view:* \n" +
				"         -objectTypeNames=sequence:o*"
			);

			xml = FileUtil.readFile(output, "UTF-8");

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
			assertEquals("Incorrect table count", "3", count);
			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='PERSON'])");
			assertEquals("PERSON table not written", "1", count);
			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='ADDRESS'])");
			assertEquals("ADDRESS table not written", "1", count);
			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='CUSTOMER'])");
			assertEquals("CUSTOMER table not written", "1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def)");
			assertEquals("Incorrect view count", "2", count);
			count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def[@name='V_PERSON'])");
			assertEquals("V_PERSON not written", "1", count);
			count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def[@name='CUSTOMER_VIEW'])");
			assertEquals("CUSTOMER_VIEW not written", "1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def)");
			assertEquals("Incorrect sequence count", "1", count);
			count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def[@name='OTHER_SEQ'])");
			assertEquals("OTHER_SEQ not written", "1", count);

			assertTrue("could not delete output file", output.delete());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testExcludeViews()
		throws Exception
	{
		try
		{
			setupDatabase();
			WbSchemaReport report = new WbSchemaReport();
			report.setConnection(source);

			File output = util.getFile("report.xml");
			output.delete();
			StatementRunnerResult result = report.execute("WbReport -file='" + output.getAbsolutePath() + "' -includeSequences=false -includeTableGrants=false -includeViews=false");
			assertTrue(result.getMessages().toString(), result.isSuccess());
			assertTrue("File not created", output.exists());

			String xml = FileUtil.readFile(output, "UTF-8");

			String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
			assertEquals("Incorrect table count", "4", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def[@name='V_PERSON'])");
			assertEquals("Incorrect view count", "0", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def)");
			assertEquals("Incorrect sequence count", "0", count);

			if (!output.delete())
			{
				fail("could not delete output file");
			}
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testExecute()
		throws Exception
	{
		try
		{
			setupDatabase();
			WbSchemaReport report = new WbSchemaReport();
			report.setConnection(source);

			File output = new File(util.getBaseDir(), "report.xml");
			output.delete();
			StatementRunnerResult result = report.execute("WbSchemaReport -file='" + output.getAbsolutePath() + "' -includeSequences=true -includeTableGrants=true");
			assertTrue(result.getMessages().toString(), result.isSuccess());
			assertTrue("File not created", output.exists());

			String xml = FileUtil.readFile(output, "UTF-8");

			String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
			assertEquals("Incorrect table count", "4", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def[@name='V_PERSON'])");
			assertEquals("Incorrect view count", "1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def)");
			assertEquals("Incorrect sequence count", "3", count);

			String value = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='Person']/grant/privilege");
			assertEquals("Wrong privilege", "SELECT", value);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='Address']/grant)");
			assertEquals("Incorrect grant count", "4", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='Address']/foreign-keys/foreign-key)");
			assertEquals("Incorrect source column count", "0", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='PERSON_ADDRESS_STATUS']/foreign-keys/foreign-key)");
			assertEquals("Incorrect source column count", "1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='PERSON_ADDRESS_STATUS']/foreign-keys/foreign-key[1]/source-columns/column)");
			assertEquals("Incorrect source column count", "2", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='PERSON_ADDRESS_STATUS']/foreign-keys/foreign-key[1]/referenced-columns/column)");
			assertEquals("Incorrect source column count", "2", count);

			String col1 = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='PERSON_ADDRESS']/column-def[@name='ADR_ID']/references/column-name/text()");
			String col2 = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='PERSON_ADDRESS']/column-def[@name='PER_ID']/references/column-name/text()");
			assertEquals("ADDRESS_ID", col1);
			assertEquals("PERSON_ID", col2);

			col1 = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='PERSON_ADDRESS']/foreign-keys/foreign-key/constraint-name[text()='FK_PA_PERSON']/../referenced-columns/column/text()");
			col2 = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='PERSON_ADDRESS']/foreign-keys/foreign-key/constraint-name[text()='FK_PA_ADDRESS']/../referenced-columns/column/text()");
			assertEquals("PERSON_ID", col1);
			assertEquals("ADDRESS_ID", col2);

			String fk1 = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='PERSON_ADDRESS']/column-def[@name='ADR_ID']/references/constraint-name/text()");
			String fk2 = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='PERSON_ADDRESS']/column-def[@name='PER_ID']/references/constraint-name/text()");
			assertEquals("FK_PA_ADDRESS", fk1);
			assertEquals("FK_PA_PERSON", fk2);

			col1 = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='PERSON_ADDRESS_STATUS']/column-def[@name='ADDRESS_ID']/references/column-name/text()");
			col2 = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='PERSON_ADDRESS_STATUS']/column-def[@name='PERSON_ID']//references/column-name/text()");
			assertEquals("ADR_ID", col1);
			assertEquals("PER_ID", col2);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='PERSON_ADDRESS']/column-def/references)");
			assertEquals("Incorrect references count", "2", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='Person']/table-constraints/constraint-definition[@name='POSITIVE_ID'])");
			assertEquals("Incorrect references count", "1", count);
//			if (!output.delete())
//			{
//				fail("could not delete output file");
//			}
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testSchemaOnly()
		throws Exception
	{
		try
		{
			TestUtil utl = new TestUtil("schemaReportTest2");
			WbConnection con = utl.getConnection();
			TestUtil.executeScript(con,
				"create schema schema_1; \n" +
				"set schema schema_1; \n" +
				"create table s_test1 (id integer); \n" +
				"create view s_view1 as select * from s_test1; \n" +
				"create schema schema_2; \n" +
				"set schema schema_2; \n" +
				"create table s_test2 (id integer); \n" +
				"create view s_view2 as select * from s_test2; \n" +
				"commit;");

			WbSchemaReport report = new WbSchemaReport();
			report.setConnection(con);

			for (int i=1; i <= 2; i++)
			{
				File output = new File(utl.getBaseDir(), "report_" + i + ".xml");
				output.delete();
				StatementRunnerResult result = report.execute("WbSchemaReport -file='" + output.getAbsolutePath() + "' -schemas=schema_" + i);
				assertTrue(result.isSuccess());
				assertTrue("File not created", output.exists());

				String xml = FileUtil.readFile(output, "UTF-8");

				String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='S_TEST" + i + "'])");
				assertEquals("Incorrect table count", "1", count);

				count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def[@name='S_VIEW" + i + "'])");
				assertEquals("Incorrect view count", "1", count);
			}
			File output = new File(utl.getBaseDir(), "report_all.xml");
			StatementRunnerResult result = report.execute("WbReport -file='" + output.getAbsolutePath() + "' -schemas=schema_1,schema_2");
			assertTrue(result.isSuccess());
			String xml = FileUtil.readFile(output, "UTF-8");
			String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
			assertEquals("2", count);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testMultipleSchema()
		throws Exception
	{
		util = new TestUtil("schemaReportTest");
		util.emptyBaseDirectory();

		this.source = util.getHSQLConnection("procReportTest");
		TestUtil.executeScript(source,
			"create schema s1;\n" +
			"create schema s2;\n" +
			"create table s1.person (id1 integer not null);\n" +
			"create table s2.person (id2 integer not null);\n" +
			"CREATE FUNCTION s1.an_hour_before (t TIMESTAMP) \n" +
						"  RETURNS TIMESTAMP \n" +
						"  RETURN t - 1 HOUR;\n" +
			"CREATE FUNCTION s2.two_hours_before (t TIMESTAMP) \n" +
						"  RETURNS TIMESTAMP \n" +
						"  RETURN t - 2 HOUR;\n");

		WbSchemaReport report = new WbSchemaReport();
		report.setConnection(source);
		File output = new File(util.getBaseDir(), "proc_report_s1.xml");
		StatementRunnerResult result = report.execute("WbReport -file='" + output.getAbsolutePath() + "' -schemas=s1 -includeProcedures=true");
		assertTrue(result.isSuccess());
		assertTrue(output.exists());
		String xml = FileUtil.readFile(output, "UTF-8");

		String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "1", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/proc-def)");
		assertEquals("Incorrect procedure count", "1", count);

		String name = TestUtil.getXPathValue(xml, "/schema-report/table-def[1]/column-def[1]/column-name");
		assertEquals("ID1", name);

		name = TestUtil.getXPathValue(xml, "/schema-report/table-def[1]/table-schema/text()");
		assertEquals("S1", name);

		name = TestUtil.getXPathValue(xml, "/schema-report/proc-def[1]/proc-name");
		assertEquals("AN_HOUR_BEFORE", name);

		assertTrue(output.delete());

		util.emptyBaseDirectory();
		output = new File(util.getBaseDir(), "proc_report_s2.xml");

		result = report.execute("WbReport -file='" + output.getAbsolutePath() + "' -schemas=s2 -includeProcedures=true");
		assertTrue(result.isSuccess());
		assertTrue(output.exists());
		xml = FileUtil.readFile(output, "UTF-8");

		count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "1", count);

		name = TestUtil.getXPathValue(xml, "/schema-report/table-def[1]/table-schema");
		assertEquals("S2", name);

		name = TestUtil.getXPathValue(xml, "/schema-report/table-def[1]/column-def[1]/column-name");
		assertEquals("ID2", name);

		count = TestUtil.getXPathValue(xml, "count(/schema-report/proc-def)");
		assertEquals("Incorrect procedure count", "1", count);

		name = TestUtil.getXPathValue(xml, "/schema-report/proc-def[1]/proc-name");
		assertEquals("TWO_HOURS_BEFORE", name);

		assertTrue(output.delete());

		output = new File(util.getBaseDir(), "proc_report_s2.xml");
		result = report.execute("WbReport -file='" + output.getAbsolutePath() + "' -tables=s1.person");
		assertTrue(result.isSuccess());
		assertTrue(output.exists());
		xml = FileUtil.readFile(output, "UTF-8");

		count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
		assertEquals("Incorrect table count", "1", count);

		name = TestUtil.getXPathValue(xml, "/schema-report/table-def[1]/table-schema");
		assertEquals("S1", name);

		name = TestUtil.getXPathValue(xml, "/schema-report/table-def[1]/column-def[1]/column-name");
		assertEquals("ID1", name);
		assertTrue(output.delete());
	}

	private void setupDatabase()
		throws SQLException, ClassNotFoundException
	{
		util = new TestUtil("schemaReportTest");
		this.source = util.getConnection();

		String script =
			"create table \"Person\" (person_id integer primary key, firstname varchar(100), lastname varchar(100), constraint positive_id check (person_id > 0));\n" +
			"create table \"Address\" (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" +
			"create table person_address (per_id integer, adr_id integer, primary key (per_id, adr_id));\n" +
			"create table person_address_status (person_id integer, address_id integer, status_name varchar(10), primary key (person_id, address_id));\n" +

			"alter table person_address add constraint fk_pa_person foreign key (per_id) references \"Person\"(person_id);\n" +
			"alter table person_address add constraint fk_pa_address foreign key (adr_id) references \"Address\"(address_id);\n" +
			"alter table person_address_status add constraint fk_pas_pa foreign key (person_id, address_id) references person_address(per_id, adr_id);\n" +

			"CREATE VIEW v_person AS SELECT * FROM \"Person\";\n" +
			"CREATE sequence seq_one;\n" +
			"CREATE sequence seq_two  increment by 5;\n" +
			"CREATE sequence seq_three;\n" +

			"create user arthur password '42';\n" +
			"GRANT SELECT ON \"Person\" TO arthur;\n" +
			"GRANT SELECT,INSERT,UPDATE,DELETE ON \"Address\" TO arthur;\n " +
			"commit;\n";
			TestUtil.executeScript(source,script);
	}

}
