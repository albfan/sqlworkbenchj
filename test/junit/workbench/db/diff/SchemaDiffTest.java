/*
 * SchemaDiffTest.java
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
package workbench.db.diff;

import java.io.File;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.util.SqlUtil;

import org.junit.After;
import org.junit.Test;

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
		diff.setSchemas("REF", "OLD");

		String xml = diff.getMigrateTargetXml();
		TestUtil util = getTestUtil();
		TestUtil.writeFile(new File(util.getBaseDir(), "basediff.xml"), xml);
//		System.out.println("---------------\n" + xml + "\n---------------");

		String count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/table-info)");
		assertEquals("Incorrect number of tables listed", "4", count);

		count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/view-info[@compareTo='SOURCE.REF.V_PERSON'])");
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

		String value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='ADDRESS']/modify-column[@name='STREET']/new-column-attributes/dbms-data-type");
		assertEquals("Street column not changed", "VARCHAR(50)", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/modify-column[@name='FIRSTNAME']/new-column-attributes/dbms-data-type");
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

		value = TestUtil.getXPathValue(xml, "/schema-diff/drop-sequence/sequence-def/sequence-name[1]");
		assertEquals("Sequence not dropped", "SEQ_TO_BE_DELETED", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/create-trigger/trigger-def/trigger-name");
		assertEquals("Trigger not created", "TRIG_INS", value);

	}

	@Test
	public void testGrantDiff()
		throws Exception
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
//			TestUtil util = getTestUtil();
//			TestUtil.writeFile(new File(util.getBaseDir(), "grantdiff.xml"), xml);

		String value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/add-grants/grant[1]/grantee");
		assertEquals("Grantee not correct", "UNIT_TEST", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/add-grants/grant[1]/privilege");
		assertEquals("Privilege not correct", "SELECT", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/revoke-grants/grant[1]/privilege");
		assertEquals("DELETE not revoked", "DELETE", value);
	}

	/**
	 * Check if an index change is detected even though nothing else has changed
	 */
	@Test
	public void testIndexChangeOnly()
		throws Exception
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
//		TestUtil util = getTestUtil();
//		TestUtil.writeFile(new File(util.getBaseDir(), "indexdiff.xml"), xml);
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

	@Test
	public void testCheckConstraint()
		throws Exception
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

	private void setupCheckTest()
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.emptyBaseDirectory();

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
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.emptyBaseDirectory();

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
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.emptyBaseDirectory();

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
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.emptyBaseDirectory();

		this.source = util.getConnection("source");
		this.target = util.getConnection("target");

		TestUtil.executeScript(source,
			"create schema ref;\n" +
			"create table ref.person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n"+
			"create table ref.address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n"+
			"create table ref.person_address (person_id integer, address_id integer, primary key (person_id, address_id));\n"+
			"create table ref.new_table (id integer primary key, some_data varchar(100));\n"+
			"alter table ref.person_address add constraint ref.fk_pa_person foreign key (person_id) references ref.person(person_id);\n"+
			"alter table ref.person_address add constraint ref.fk_pa_address foreign key (address_id) references ref.address(address_id);\n"+
			"CREATE VIEW ref.v_person AS SELECT * FROM ref.person;\n"+
			"CREATE sequence ref.seq_one;\n"+
			"CREATE sequence ref.seq_two  increment by 5;\n"+
			"CREATE sequence ref.seq_three;\n" +
			"CREATE TRIGGER ref.TRIG_INS BEFORE INSERT ON ref.person FOR EACH ROW CALL \"workbench.db.diff.H2TestTrigger\";\n" +
			"commit;");

		TestUtil.executeScript(target,
			"create schema old;\n" +
			"create table old.person (person_id integer primary key, firstname varchar(50), lastname varchar(100));\n"+
			"create table old.address (address_id integer primary key, street varchar(10), city varchar(100), pone varchar(50), remark varchar(500));\n"+
			"create table old.person_address (person_id integer, address_id integer, primary key (person_id, address_id));\n"+
			"alter table old.person_address add constraint old.fk_pa_person foreign key (person_id) references old.person(person_id);\n"+
			"CREATE VIEW old.something AS SELECT * FROM old.address;\n"+
			"CREATE sequence old.seq_one;\n"+
			"CREATE sequence old.seq_two;\n"+
			"CREATE sequence old.seq_to_be_deleted;\n" +
			"commit;");

	}
}
