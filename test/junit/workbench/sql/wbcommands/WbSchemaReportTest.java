/*
 * WbSchemaReportTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;
import org.junit.Test;

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
	public void testExcludeViews()
		throws Exception
	{
		try
		{
			setupDatabase();
			WbSchemaReport report = new WbSchemaReport();
			report.setConnection(source);

			File output = new File(util.getBaseDir(), "report.xml");
			output.delete();
			StatementRunnerResult result = report.execute("WbReport -file='" + output.getAbsolutePath() + "' -includeSequences=false -includeTableGrants=false -includeViews=false");
			assertTrue(result.isSuccess());
			assertTrue("File not created", output.exists());

			InputStreamReader r = new InputStreamReader(new FileInputStream(output), "UTF-8");
			String xml = FileUtil.readCharacters(r);
			r.close();

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
			StatementRunnerResult result = report.execute("WbReport -file='" + output.getAbsolutePath() + "' -includeSequences=true -includeTableGrants=true");
			assertTrue(result.isSuccess());
			assertTrue("File not created", output.exists());

			InputStreamReader r = new InputStreamReader(new FileInputStream(output), "UTF-8");
			String xml = FileUtil.readCharacters(r);
			r.close();

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

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='PERSON_ADDRESS']/column-def/references)");
			assertEquals("Incorrect references count", "2", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='Person']/table-constraints/constraint-definition[@name='POSITIVE_ID'])");
			assertEquals("Incorrect references count", "1", count);
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
				StatementRunnerResult result = report.execute("WbReport -file='" + output.getAbsolutePath() + "' -schemas=schema_" + i);
				assertTrue(result.isSuccess());
				assertTrue("File not created", output.exists());

				InputStreamReader r = new InputStreamReader(new FileInputStream(output), "UTF-8");
				String xml = FileUtil.readCharacters(r);
				r.close();

				String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='S_TEST" + i + "'])");
				assertEquals("Incorrect table count", "1", count);

				count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def[@name='S_VIEW" + i + "'])");
				assertEquals("Incorrect view count", "1", count);
			}

		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	private void setupDatabase()
		throws SQLException, ClassNotFoundException
	{
		util = new TestUtil("schemaReportTest");
		this.source = util.getConnection();

		Statement stmt = null;

		try
		{
			stmt = source.createStatement();
			stmt.executeUpdate("create table \"Person\" (person_id integer primary key, firstname varchar(100), lastname varchar(100), constraint positive_id check (person_id > 0))");
			stmt.executeUpdate("create table \"Address\" (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50))");
			stmt.executeUpdate("create table person_address (person_id integer, address_id integer, primary key (person_id, address_id))");
			stmt.executeUpdate("create table person_address_status (person_id integer, address_id integer, status_name varchar(10), primary key (person_id, address_id))");

			stmt.executeUpdate("alter table person_address add constraint fk_pa_person foreign key (person_id) references \"Person\"(person_id)");
      stmt.executeUpdate("alter table person_address add constraint fk_pa_address foreign key (address_id) references \"Address\"(address_id)");
      stmt.executeUpdate("alter table person_address_status add constraint fk_pas_pa foreign key (person_id, address_id) references person_address(person_id, address_id)");

			stmt.executeUpdate("CREATE VIEW v_person AS SELECT * FROM \"Person\"");
			stmt.executeUpdate("CREATE sequence seq_one");
			stmt.executeUpdate("CREATE sequence seq_two  increment by 5");
			stmt.executeUpdate("CREATE sequence seq_three");

			stmt.executeUpdate("create user arthur password '42'");
			stmt.executeUpdate("GRANT SELECT ON \"Person\" TO arthur");
			stmt.executeUpdate("GRANT SELECT,INSERT,UPDATE,DELETE ON \"Address\" TO arthur");
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

}
