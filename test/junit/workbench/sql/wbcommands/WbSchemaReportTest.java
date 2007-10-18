/*
 * WbSchemaReportTest.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Statement;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbSchemaReportTest
	extends TestCase
{

	private WbConnection source;
	private TestUtil util;
	
	public WbSchemaReportTest(String testName)
	{
		super(testName);
	}

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
			assertEquals("Incorrect table count", "3", count);
			
			count = TestUtil.getXPathValue(xml, "count(/schema-report/view-def[@name='V_PERSON'])");
			assertEquals("Incorrect view count", "1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-report/sequence-def)");
			assertEquals("Incorrect sequence count", "3", count);
			
			String value = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='PERSON']/grant/privilege");
			assertEquals("Wrong privilege", "SELECT", value);
			 
			count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='ADDRESS']/grant)");
			assertEquals("Incorrect grant count", "4", count);

			if (!output.delete())
			{
				fail("could not delete output file");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
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
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50))");
			stmt.executeUpdate("create table person_address (person_id integer, address_id integer, primary key (person_id, address_id))");
			stmt.executeUpdate("alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id)");
      stmt.executeUpdate("alter table person_address add constraint fk_pa_address foreign key (address_id) references address(address_id)");

			stmt.executeUpdate("CREATE VIEW v_person AS SELECT * FROM person");
			stmt.executeUpdate("CREATE sequence seq_one");
			stmt.executeUpdate("CREATE sequence seq_two  increment by 5");
			stmt.executeUpdate("CREATE sequence seq_three");
			
			stmt.executeUpdate("create user arthur password '42'");
			stmt.executeUpdate("GRANT SELECT ON person TO arthur");
			stmt.executeUpdate("GRANT SELECT,INSERT,UPDATE,DELETE ON address TO arthur");
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}
	
}
