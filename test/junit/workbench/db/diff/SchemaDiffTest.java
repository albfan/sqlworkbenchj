/*
 * SchemaDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import org.junit.Test;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.After;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SchemaDiffTest
	extends WbTestCase
{
	private WbConnection source;
	private WbConnection target;

	public SchemaDiffTest()
	{
		super("SchemaDiffTest");
	}

	@After
	public void tearDown()
	{
		try { source.disconnect(); } catch (Throwable th) {}
		try { target.disconnect(); } catch (Throwable th) {}
	}

	@Test
	public void testBaseDiff()
		throws Exception
	{
		setupBaseDatabase();
		SchemaDiff diff = new SchemaDiff(source, target);
		diff.setIncludeForeignKeys(true);
		diff.setIncludePrimaryKeys(true);
		diff.setIncludeProcedures(false);
		diff.setIncludeTableGrants(false);
		diff.setIncludeTableConstraints(true);
		diff.setIncludeSequences(true);
		diff.setIncludeViews(true);
		diff.setIncludeTriggers(true);
		diff.compareAll();
		String xml = diff.getMigrateTargetXml();
//		TestUtil util = new TestUtil("testBaseDiff");
//		TestUtil.writeFile(new File(util.getBaseDir(), "basediff.xml"), xml);
//		Thread.yield();
//		System.out.println("---------------\n" + xml + "\n---------------");

		String count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/table-info)");
		assertEquals("Incorrect number of tables listed", "4", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/view-info[@compareTo='V_PERSON'])");
		assertEquals("Incorrect number of views listed", "1", count);

		// Check if email column
		String col = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='ADDRESS']/add-column/column-def[@name='EMAIL']/column-name");
		assertNotNull("Table ADDRESS not changed", col);
		assertEquals("Table ADDRESS not changed", "EMAIL", col);

		count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='ADDRESS']/add-column)");
		assertEquals("Incorrect number of columns to add to ADDRESS", "1", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-diff/add-table[@name='NEW_TABLE'])");
		assertEquals("New table not detected", "1", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='ADDRESS']/remove-column[@name='REMARK'])");
		assertEquals("Remark column not removed", "1", count);

		String value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='ADDRESS']/modify-column[@name='STREET']/dbms-data-type");
		assertEquals("Street column not changed", "VARCHAR(50)", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/modify-column[@name='FIRSTNAME']/dbms-data-type");
		assertEquals("Firstname column not changed", "VARCHAR(100)", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON_ADDRESS']/add-foreign-keys/foreign-key)");
		assertEquals("Wrong FK count", "1", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON_ADDRESS']/add-foreign-keys/foreign-key/references/table-name");
		assertEquals("FK to address not added", "ADDRESS", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON_ADDRESS']/add-foreign-keys/foreign-key/source-columns/column[1]");
		assertEquals("Wrong FK source column", "ADDRESS_ID", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON_ADDRESS']/add-foreign-keys/foreign-key/source-columns/column)");
		assertEquals("Wrong FK source column count", "1", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON_ADDRESS']/add-foreign-keys/foreign-key/referenced-columns/column[1]");
		assertEquals("Wrong FK target column", "ADDRESS_ID", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON_ADDRESS']/add-foreign-keys/foreign-key/referenced-columns/column)");
		assertEquals("Wrong FK target column count", "1", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON_ADDRESS']/add-index/index-def/index-expression");
		assertEquals("Index for address_id not added", "ADDRESS_ID ASC", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/create-view/view-def[@name='V_PERSON']/view-name");
		assertEquals("View not created ", "V_PERSON", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/drop-view/view-name[1]");
		assertEquals("View not dropped ", "SOMETHING", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/update-sequence[1]/sequence-def/sequence-name");
		assertEquals("Sequence not updated", "SEQ_TWO", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/create-sequence[1]/sequence-def/sequence-name");
		assertEquals("Sequence not created", "SEQ_THREE", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/drop-sequence/sequence-name[1]");
		assertEquals("Sequence not dropped", "SEQ_TO_BE_DELETED", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/create-trigger/trigger-def/trigger-name");
		assertEquals("Trigger not created", "TRIG_INS", value);

	}

	@Test
	public void testGrantDiff()
	{
		try
		{
			setupGrantTestDb();
			SchemaDiff diff = new SchemaDiff(source, target);
			diff.setIncludeForeignKeys(false);
			diff.setIncludeIndex(false);
			diff.setIncludePrimaryKeys(false);
			diff.setIncludeProcedures(false);
			diff.setIncludeTableGrants(true);
			diff.setIncludeTableConstraints(false);
			diff.setIncludeViews(false);
			diff.compareAll();
			String xml = diff.getMigrateTargetXml();
//			TestUtil util = new TestUtil("testGrantDiff");
//			TestUtil.writeFile(new File(util.getBaseDir(), "grantdiff.xml"), xml);

			String value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/add-grants/grant[1]/grantee");
			assertEquals("Grantee not correct", "UNIT_TEST", value);

			value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/add-grants/grant[1]/privilege");
			assertEquals("Privilege not correct", "SELECT", value);

			value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/revoke-grants/grant[1]/privilege");
			assertEquals("DELETE not revoked", "DELETE", value);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	/**
	 * Check if an index change is detected even though nothing else has changed
	 */
	@Test
	public void testIndexChangeOnly()
	{
		try
		{
			setupIndexDiffTestDb();
			SchemaDiff diff = new SchemaDiff(source, target);
			diff.setIncludeForeignKeys(true);
			diff.setIncludeIndex(true);
			diff.setIncludePrimaryKeys(true);
			diff.setIncludeProcedures(false);
			diff.setIncludeTableGrants(false);
			diff.setIncludeTableConstraints(true);
			diff.setIncludeViews(false);
			diff.compareAll();
			String xml = diff.getMigrateTargetXml();
//			TestUtil util = new TestUtil("testIndexChangeOnly");
//			TestUtil.writeFile(new File(util.getBaseDir(), "indexdiff.xml"), xml);
			String count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/table-info)");
			assertEquals("Incorrect number of tables listed", "3", count);

			String value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/add-index/index-def/index-expression");
			assertEquals("Index for address_id not added", "LASTNAME ASC", value);

			value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='ADDRESS']/drop-index");
			assertEquals("Wrong index dropped", "INDEX_TO_BE_DELETED", value);

			diff.setIncludeIndex(false);
			xml = diff.getMigrateTargetXml();

			count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/table-info)");
			assertEquals("Incorrect number of tables listed", "3", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON']/add-index)");
			assertEquals("Add index present", "0", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='ADDRESS']/drop-index)");
			assertEquals("Add index present", "0", count);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testCheckConstraint()
	{
		try
		{
			setupCheckTest();
			SchemaDiff diff = new SchemaDiff(source, target);
			diff.setIncludeForeignKeys(false);
			diff.setIncludeIndex(false);
			diff.setIncludePrimaryKeys(false);
			diff.setIncludeProcedures(false);
			diff.setIncludeTableGrants(false);
			diff.setIncludeTableConstraints(true);
			diff.setCompareConstraintsByName(true);
			diff.setIncludeViews(false);
			diff.compareAll();
			String xml = diff.getMigrateTargetXml();
//			System.out.println("**********\n" + xml + "\n*****************");
			String count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON']/table-constraints/add-constraint/constraint-definition[@name='LNAME_MIN_LENGTH'])");
			assertEquals("1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON']/table-constraints/modify-constraint/constraint-definition[@name='POSITIVE_ID'])");
			assertEquals("1", count);

			diff.setCompareConstraintsByName(false);
			xml = diff.getMigrateTargetXml();
			count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON']/table-constraints/add-constraint/constraint-definition[@name='LNAME_MIN_LENGTH'])");
			assertEquals("1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON']/table-constraints/add-constraint/constraint-definition[@name='POSITIVE_ID'])");
			assertEquals("1", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON']/table-constraints/drop-constraint/constraint-definition[@name='POSITIVE_ID'])");
			assertEquals("1", count);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void setupCheckTest()
		throws SQLException, ClassNotFoundException
	{
		TestUtil util = new TestUtil("schemaDiffTest");

		this.source = util.getConnection("source");
		this.target = util.getConnection("target");

		TestUtil.executeScript(source,
			"create table person (" +
			"  person_id integer primary key, " +
			"  firstname varchar(100), " +
			"  lastname varchar(100), " +
			"  constraint positive_id check (person_id > 0)," +
			"  constraint lname_min_length check (length(lastname) > 5), " +
			"  check (length(firstname) > 5)" +
			")");

		TestUtil.executeScript(target,
			"create table person (" +
			"  person_id integer primary key, " +
			"  firstname varchar(100), " +
			"  lastname varchar(100), " +
			"  constraint positive_id  check (person_id > 1) " +
			")");

	}
	
	private void setupGrantTestDb()
		throws SQLException, ClassNotFoundException
	{
		TestUtil util = new TestUtil("schemaDiffTest");

		this.source = util.getConnection("source");
		this.target = util.getConnection("target");
		Statement stmt = null;

		try
		{
			stmt = source.createStatement();
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("CREATE USER unit_test PASSWORD 'secret'");
			stmt.executeUpdate("GRANT SELECT ON PERSON to unit_test");

			stmt = target.createStatement();
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("CREATE USER unit_test PASSWORD 'secret'");
			stmt.executeUpdate("GRANT DELETE ON PERSON to unit_test");
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	private void setupIndexDiffTestDb()
		throws SQLException, ClassNotFoundException
	{
		TestUtil util = new TestUtil("schemaDiffTest");

		this.source = util.getConnection("source");
		this.target = util.getConnection("target");
		Statement stmt = null;

		try
		{
			stmt = source.createStatement();
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50))");
			stmt.executeUpdate("create table person_address (person_id integer, address_id integer, primary key (person_id, address_id))");
			stmt.executeUpdate("alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id)");
      stmt.executeUpdate("alter table person_address add constraint fk_pa_address foreign key (address_id) references address(address_id)");

			stmt.executeUpdate("create index test_index on person (lastname)");

			stmt = target.createStatement();
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50))");
			stmt.executeUpdate("create table person_address (person_id integer, address_id integer, primary key (person_id, address_id))");
			stmt.executeUpdate("alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id)");
      stmt.executeUpdate("alter table person_address add constraint fk_pa_address foreign key (address_id) references address(address_id)");
			stmt.executeUpdate("create index index_to_be_deleted on address (street);");

		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	private void setupBaseDatabase()
		throws SQLException, ClassNotFoundException
	{
		TestUtil util = new TestUtil("schemaDiffTest");

		this.source = util.getConnection("source");
		this.target = util.getConnection("target");

		TestUtil.executeScript(source, 
			"create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n"+
			"create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n"+
			"create table person_address (person_id integer, address_id integer, primary key (person_id, address_id));\n"+
			"create table new_table (id integer primary key, some_data varchar(100));\n"+
			"alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id);\n"+
			"alter table person_address add constraint fk_pa_address foreign key (address_id) references address(address_id);\n"+
			"CREATE VIEW v_person AS SELECT * FROM person;\n"+
			"CREATE sequence seq_one;\n"+
			"CREATE sequence seq_two  increment by 5;\n"+
			"CREATE sequence seq_three;\n" +
			"CREATE TRIGGER TRIG_INS BEFORE INSERT ON person FOR EACH ROW CALL \"workbench.db.diff.H2TestTrigger\";\n" +
			"commit;");

		TestUtil.executeScript(target,
			"create table person (person_id integer primary key, firstname varchar(50), lastname varchar(100));\n"+
			"create table address (address_id integer primary key, street varchar(10), city varchar(100), pone varchar(50), remark varchar(500));\n"+
			"create table person_address (person_id integer, address_id integer, primary key (person_id, address_id));\n"+
			"alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id);\n"+
			"CREATE VIEW something AS SELECT * FROM address;\n"+
			"CREATE sequence seq_one;\n"+
			"CREATE sequence seq_two;\n"+
			"CREATE sequence seq_to_be_deleted;\n" +
			"commit;");

	}
}
