/*
 * WbSchemaDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.io.FileReader;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.gui.profiles.ProfileKey;
import workbench.sql.StatementRunnerResult;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class WbSchemaDiffTest
	extends WbTestCase
{

	private TestUtil util;

	public WbSchemaDiffTest()
	{
		super("WbSchemaDiffTest");
	}

	@Test
	public void testTreatViewAsTable()
		throws Exception
	{
		setupDatabase();
		Statement stmt = null;

		WbConnection target = ConnectionMgr.getInstance().getConnection(new ProfileKey("target"), "manual");
		try
		{
			stmt = target.createStatement();
			stmt.executeUpdate("drop view something");
			stmt.executeUpdate("create table v_person (person_id integer, firstname varchar(100))");
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
			// Close the connection in order to be able to check if WbSchemaReport
			// frees them correctly when they are opened during the compare
			target.disconnect();
		}

		WbSchemaDiff diff = new WbSchemaDiff();
		File output = new File(util.getBaseDir(), "view_table_test.xml");
		output.delete();
		StatementRunnerResult result = diff.execute("WbSchemaDiff -file='" + output.getAbsolutePath() + "' -viewAsTable=true -includeForeignKeys=false -includePrimaryKeys=false -includeIndex=false -includeSequences=false -referenceProfile=source -targetProfile=target");
		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());
		assertTrue("File not created", output.exists());

		FileReader in = new FileReader(output);
		String xml = FileUtil.readCharacters(in);

		String value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='V_PERSON']/add-column/column-def[@name='LASTNAME'])");
		assertEquals("Incorrect table count", "1", value);
		assertEquals("Connections not closed", 0, ConnectionMgr.getInstance().getOpenCount());
	}

	@Test
	public void testBaseDiff()
		throws Exception
	{
		setupDatabase();

		WbSchemaDiff diff = new WbSchemaDiff();
		File output = new File(util.getBaseDir(), "diffTest.xml");
		output.delete();
		StatementRunnerResult result = diff.execute("WbSchemaDiff -file='" + output.getAbsolutePath() + "' -excludeTables=TO_* -includeForeignKeys=false -includePrimaryKeys=false -includeIndex=false -includeSequences=true -referenceProfile=source -targetProfile=target");
		assertTrue(result.isSuccess());
		assertTrue("File not created", output.exists());

		FileReader in = new FileReader(output);
		String xml = FileUtil.readCharacters(in);

		String value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table)");
		assertEquals("Incorrect table count", "2", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/sequence-info)");
		assertEquals("Incorrect sequence info count", "3", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/table-info)");
		assertEquals("Incorrect table info count", "3", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/view-info)");
		assertEquals("Incorrect view info count", "1", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='ADDRESS']/add-column/column-def)");
		assertEquals("Incorrect table count", "2", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='ADDRESS']/remove-column)");
		assertEquals("Incorrect table count", "2", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='ADDRESS']/remove-column[1]/@name");
		assertEquals("Wrong view to create", "PONE", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='ADDRESS']/remove-column[2]/@name");
		assertEquals("Wrong view to create", "REMARK", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/modify-column[1]/@name");
		assertEquals("Incorrect table count", "FIRSTNAME", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/create-view[1]/view-def/@name");
		assertEquals("Wrong view to create", "V_PERSON", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/drop-view/view-name/text()");
		assertEquals("Incorrect sequence to delete", "SOMETHING", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/update-sequence/sequence-def/@name");
		assertEquals("Incorrect sequence to update", "SEQ_TWO", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/create-sequence/sequence-def/@name");
		assertEquals("Incorrect sequence to create", "SEQ_THREE", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/drop-sequence/sequence-name/text()");
		assertEquals("Incorrect sequence to delete", "SEQ_TO_BE_DELETED", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/drop-table/table-name)");
		assertEquals("Wrong drop table count", "1", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/drop-table/table-name/text()");
		assertEquals("Incorrect sequence to delete", "DROP_ME", value);

//		if (!output.delete())
//		{
//			fail("could not delete output file");
//		}
		assertEquals("Connections not closed", 0, ConnectionMgr.getInstance().getOpenCount());
	}

	private void setupDatabase()
		throws SQLException, ClassNotFoundException
	{
		util = new TestUtil("schemaDiffTest");

		WbConnection source = util.getConnection(new File(util.getBaseDir(), "source"), "source", false);
		WbConnection target = util.getConnection(new File(util.getBaseDir(), "target"), "target", false);

		Statement stmt = null;

		try
		{
			stmt = source.createStatement();
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50))");
			stmt.executeUpdate("create table person_address (person_id integer, address_id integer, primary key (person_id, address_id))");
			stmt.executeUpdate("create table to_ignore (some_id integer, another_id integer)");
			stmt.executeUpdate("alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id)");
			stmt.executeUpdate("alter table person_address add constraint fk_pa_address foreign key (address_id) references address(address_id)");

			stmt.executeUpdate("CREATE VIEW v_person AS SELECT * FROM person");
			stmt.executeUpdate("CREATE sequence seq_one");
			stmt.executeUpdate("CREATE sequence seq_two  increment by 5");
			stmt.executeUpdate("CREATE sequence seq_three");
			SqlUtil.closeStatement(stmt);

			stmt = target.createStatement();
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(50), lastname varchar(100))");
			stmt.executeUpdate("create table to_ignore (some_id integer)");
			stmt.executeUpdate("create table drop_me (some_id integer)");
			stmt.executeUpdate("create table address (address_id integer primary key, street varchar(10), city varchar(100), pone varchar(50), remark varchar(500))");
			stmt.executeUpdate("create table person_address (person_id integer, address_id integer, primary key (person_id, address_id))");
			stmt.executeUpdate("alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id)");

			stmt.executeUpdate("CREATE VIEW something AS SELECT * FROM address");

			stmt.executeUpdate("CREATE sequence seq_one");
			stmt.executeUpdate("CREATE sequence seq_two");
			stmt.executeUpdate("CREATE sequence seq_to_be_deleted");
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
			source.disconnect();
			target.disconnect();
		}
	}
}
