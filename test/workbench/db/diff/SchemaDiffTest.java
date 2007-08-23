/*
 * SchemaDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import java.sql.SQLException;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class SchemaDiffTest 
	extends junit.framework.TestCase
{
	
	private WbConnection source;
	private WbConnection target;
	
	public SchemaDiffTest(String testName)
	{
		super(testName);
	}
	
	public void tearDown()
	{
		try { source.disconnect(); } catch (Throwable th) {}
		try { target.disconnect(); } catch (Throwable th) {}
	}
	
	public void testBaseDiff()
	{
		try
		{
			setupBaseDatabase();
			SchemaDiff diff = new SchemaDiff(source, target);
			diff.setIncludeForeignKeys(true);
			diff.setIncludePrimaryKeys(true);
			diff.setIncludeProcedures(false);
			diff.setIncludeTableGrants(false);
			diff.setIncludeTableConstraints(true);
			diff.setIncludeViews(true);
			diff.compareAll();
			String xml = diff.getMigrateTargetXml();
			Thread.yield();
			System.out.println("---------------");
			System.out.println(xml);
			System.out.println("---------------");

			String count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/table-info)");
			assertEquals("Incorrect number of tables listed", "3", count);

			count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/view-info)");
			assertEquals("Incorrect number of views listed", "1", count);
			
			// Check if email column 
			String col = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='ADDRESS']/add-column/column-def[@name='EMAIL']/column-name");
			assertNotNull("Table ADDRESS not changed", col);
			assertEquals("Table ADDRESS not changed", "EMAIL", col);

			count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='ADDRESS']/add-column)");
			assertEquals("Incorrect number of columns to add to ADDRESS", "1", count);
			
			count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='ADDRESS']/remove-column[@name='REMARK'])");
			assertEquals("Remark column not removed", "1", count);
			
			String value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='ADDRESS']/modify-column[@name='STREET']/dbms-data-type");
			assertEquals("Street column not changed", "VARCHAR(50)", value);

			value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/modify-column[@name='FIRSTNAME']/dbms-data-type");
			assertEquals("Firstname column not changed", "VARCHAR(100)", value);

			value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON_ADDRESS']/modify-column[@name='ADDRESS_ID']/add-reference/table-name");
			assertEquals("FK to address not added", "ADDRESS", value);

			value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON_ADDRESS']/add-index/index-def/index-expression");
			assertEquals("Index for address_id not added", "ADDRESS_ID A", value);
			
			value = TestUtil.getXPathValue(xml, "/schema-diff/create-view/view-def[@name='V_PERSON']/view-name");
			assertEquals("View not created ", "V_PERSON", value);
			
			value = TestUtil.getXPathValue(xml, "/schema-diff/drop-view/view-name[1]");
			assertEquals("View not dropped ", "SOMETHING", value);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

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
			
			String count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/table-info)");
			assertEquals("Incorrect number of tables listed", "3", count);
			
			String value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='PERSON']/add-index/index-def/index-expression");
			assertEquals("Index for address_id not added", "LASTNAME A", value);
			
			diff.setIncludeIndex(false);
			xml = diff.getMigrateTargetXml();
			
			count = TestUtil.getXPathValue(xml, "count(/schema-diff/compare-settings/table-info)");
			assertEquals("Incorrect number of tables listed", "3", count);
			
			count = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table/add-index)");
			assertEquals("Add index present", "0", count);			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
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
			
			stmt = target.createStatement();
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(50), lastname varchar(100))");
			stmt.executeUpdate("create table address (address_id integer primary key, street varchar(10), city varchar(100), pone varchar(50), remark varchar(500))");
			stmt.executeUpdate("create table person_address (person_id integer, address_id integer, primary key (person_id, address_id))");
			stmt.executeUpdate("alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id)");
			
			stmt.executeUpdate("CREATE VIEW something AS SELECT * FROM address");
			
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
		
	}
}
