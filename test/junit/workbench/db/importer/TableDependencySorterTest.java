/*
 * TableDependencySorterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.importer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.DeleteScriptGeneratorTest;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDependencySorterTest
	extends WbTestCase
{
	private WbConnection dbConn;

	public TableDependencySorterTest()
	{
		super("ImportFileHandlerTest");
	}

	private void createTables()
		throws Exception
	{
		TestUtil util = getTestUtil("dependencyTest");
		dbConn = util.getConnection();
		Statement stmt = dbConn.createStatement();
			String baseSql = "CREATE TABLE base  \n" +
             "( \n" +
             "   id1  INTEGER NOT NULL, \n" +
             "   id2  INTEGER NOT NULL, \n" +
             "   primary key (id1, id2) \n" +
             ")";
			stmt.execute(baseSql);

			String child1Sql = "CREATE TABLE child1 \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   base_id1    INTEGER NOT NULL, \n" +
             "   base_id2    INTEGER NOT NULL, \n" +
             "   FOREIGN KEY (base_id1, base_id2) REFERENCES base (id1,id2) \n" +
             ")";
			stmt.executeUpdate(child1Sql);

			String child2Sql = "CREATE TABLE child2 \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   base_id1    INTEGER NOT NULL, \n" +
             "   base_id2    INTEGER NOT NULL, \n" +
             "   FOREIGN KEY (base_id1, base_id2) REFERENCES base (id1,id2) \n" +
             ")";
			stmt.executeUpdate(child2Sql);

			String child3Sql = "CREATE TABLE child2_detail \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   child_id    INTEGER NOT NULL, \n" +
             "   FOREIGN KEY (child_id) REFERENCES child2 (id) \n" +
             ")";
			stmt.executeUpdate(child3Sql);

			String sql = "CREATE TABLE child1_detail \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   child1_id    INTEGER NOT NULL, \n" +
             "   FOREIGN KEY (child1_id) REFERENCES child1 (id) \n" +
             ")";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE child1_detail2 \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   detail_id    INTEGER NOT NULL, \n" +
             "   FOREIGN KEY (detail_id) REFERENCES child1_detail (id) \n" +
             ")";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE tbl1 \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY" +
             ")";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE tbl2 \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY" +
             ")";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE tbl3 \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY" +
             ")";
			stmt.executeUpdate(sql);
	}

	@After
	public void tearDown()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testPlainTables()
		throws Exception
	{
		createTables();
		ArrayList<TableIdentifier> tables = new ArrayList<TableIdentifier>();
		for (int i=0; i < 3; i++)
		{
			tables.add(new TableIdentifier("tbl" + (i + 1)));
		}
		TableDependencySorter sorter = new TableDependencySorter(this.dbConn);
		List<TableIdentifier> result = sorter.sortForInsert(tables);
		assertEquals("Not enough entries", tables.size(), result.size());
	}

	@Test
	public void testNonExistingTable()
		throws Exception
	{
		TestUtil util = getTestUtil("dependencyTest");
		dbConn = util.getConnection();
		Statement stmt = dbConn.createStatement();
		stmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
		stmt.executeUpdate("create table address (person_id integer not null primary key, address_details varchar(100))");
		stmt.executeUpdate("alter table address add foreign key (person_id) references person(nr)");
		dbConn.commit();

		TableDependencySorter sorter = new TableDependencySorter(this.dbConn);
		List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
		TableIdentifier person = new TableIdentifier("PERSON");
		TableIdentifier address = new TableIdentifier("ADDRESS");
		tables.add(person);
		tables.add(address);
		tables.add(new TableIdentifier("some_data"));
		List<TableIdentifier> result = sorter.sortForInsert(tables);
//		System.out.println("********************");
//		for (TableIdentifier tbl : result)
//		{
//			System.out.println(tbl.getTableName());
//		}
//		System.out.println("********************");
		assertEquals(2, result.size());
		assertTrue(tableIndex("person", result) < tableIndex("address", result));
	}

	@Test
	public void testCheckDependencies()
		throws Exception
	{
		createTables();
		TableIdentifier base = new TableIdentifier("base");
		TableIdentifier child1 = new TableIdentifier("child1");
		TableIdentifier child2 = new TableIdentifier("child2");
		TableIdentifier child1_detail = new TableIdentifier("child1_detail");
		TableIdentifier child1_detail2 = new TableIdentifier("child1_detail2");
		TableIdentifier child2_detail = new TableIdentifier("child2_detail");
		ArrayList<TableIdentifier> tbl = new ArrayList<TableIdentifier>();
		tbl.add(child1);
		tbl.add(base);
		tbl.add(child2);
		tbl.add(child1_detail);
		tbl.add(child2_detail);
		tbl.add(child1_detail2);
		TableDependencySorter sorter = new TableDependencySorter(this.dbConn);
		List<TableIdentifier> result = sorter.sortForDelete(tbl, false);
//		for (TableIdentifier t : result)
//		{
//			System.out.println(t.toString());
//		}
//		System.out.println("--------------------");
		assertEquals("Not enough entries", tbl.size(), result.size());
		assertTrue("Wrong sort for child1", tableIndex("child1_detail2", result) < tableIndex("child1", result));
		assertTrue(tableIndex("child2_detail", result) < tableIndex("child2", result));
		assertTrue(tableIndex("child1", result) < tableIndex("base", result));
		assertTrue(tableIndex("child2", result) < tableIndex("base", result));

		List<TableIdentifier> insertList = sorter.sortForInsert(tbl);
//		for (TableIdentifier t : insertList)
//		{
//			System.out.println(t.toString());
//		}
		assertEquals("Not enough entries", tbl.size(), insertList.size());
		assertTrue("Wrong first table for insert", base.compareNames(insertList.get(0)));
		assertTrue(tableIndex("child2", insertList) > tableIndex("base", insertList));
		assertTrue(tableIndex("child1", insertList) > tableIndex("base", insertList));
		assertTrue(tableIndex("child2_detail", insertList) > tableIndex("child2", insertList));
		assertTrue(tableIndex("child1_detail", insertList) > tableIndex("child1", insertList));
		assertTrue(tableIndex("child1_detail2", insertList) > tableIndex("child1", insertList));
	}

	@Test
	public void testCheckAddMissing()
		throws Exception
	{
		createTables();
		TableIdentifier child1 = new TableIdentifier("child1");
		ArrayList<TableIdentifier> tbl = new ArrayList<TableIdentifier>();
		tbl.add(child1);

		TableDependencySorter sorter = new TableDependencySorter(this.dbConn);
		List<TableIdentifier> result = sorter.sortForDelete(tbl, true);
//		for (TableIdentifier t : result)
//		{
//			System.out.println(t.toString());
//		}
//		System.out.println("--------------------");

		// Should have added child1_detail and child1_detail2
		assertEquals("Not enough entries", 3, result.size());
		assertTrue(tableIndex("child1_detail", result) < tableIndex("child1", result));
		assertTrue(tableIndex("child1_detail2", result) < tableIndex("child1", result));
	}

	@Test
	public void testCatDeleteDependency()
		throws Exception
	{
		TestUtil util = new TestUtil("dependencyTest");
		dbConn = util.getConnection();

		InputStream in = getClass().getResourceAsStream("dependency_tables.sql");
		Reader r = new InputStreamReader(in);
		String script = FileUtil.readCharacters(r);
		TestUtil.executeScript(dbConn, script);

		List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
		TableIdentifier product = new TableIdentifier("PRODUCT");
		TableIdentifier catNode = new TableIdentifier("CATALOGUE_NODE");
		tables.add(catNode);
		tables.add(product);

		TableDependencySorter sorter = new TableDependencySorter(this.dbConn);
		List<TableIdentifier> result = sorter.sortForDelete(tables, true);

//		for (TableIdentifier t : result)
//		{
//			System.out.println(t.getTableName());
//		}
//		System.out.println("--------------------");

		assertEquals(6, result.size());

		assertTrue(tableIndex("product", result) > tableIndex("shop", result));
		assertTrue(tableIndex("product", result) > tableIndex("catalogue_node", result));
		assertTrue(tableIndex("shop", result) < tableIndex("catalogue", result));
		assertTrue(tableIndex("localised_catalogue_node", result) < tableIndex("catalogue_node", result));
	}

	@Test
	public void testCatDependencyForInsert()
		throws Exception
	{
		TestUtil util = new TestUtil("dependencyTest");
		dbConn = util.getConnection();

		InputStream in = getClass().getResourceAsStream("dependency_tables.sql");
		Reader r = new InputStreamReader(in);
		String script = FileUtil.readCharacters(r);
		TestUtil.executeScript(dbConn, script);

		ArrayList<TableIdentifier> tbl = new ArrayList<TableIdentifier>();
		TableIdentifier cat = new TableIdentifier("catalogue");
		TableIdentifier node = new TableIdentifier("catalogue_node");
		tbl.add(cat);
		tbl.add(node);

		TableDependencySorter sorter = new TableDependencySorter(this.dbConn);
		List<TableIdentifier> result = sorter.sortForInsert(tbl);
//		for (TableIdentifier t : result)
//		{
//			System.out.println(t.getTableName());
//		}
		assertEquals(2, result.size());
		assertTrue(node.compareNames(result.get(0)));
		assertTrue(cat.compareNames(result.get(1)));
	}

	@Test
	public void testInsertSorterHR()
		throws Exception
	{
		TestUtil util = getTestUtil();
		dbConn = util.getHSQLConnection("hr");

		InputStream in = getClass().getResourceAsStream("hr_schema.sql");
		Reader r = new InputStreamReader(in);
		String script = FileUtil.readCharacters(r);
		TestUtil.executeScript(dbConn, script);

		ArrayList<TableIdentifier> tables = new ArrayList<TableIdentifier>();
		tables.add(new TableIdentifier("COUNTRIES"));
		tables.add(new TableIdentifier("DEPARTMENTS"));
		tables.add(new TableIdentifier("EMPLOYEES"));
		tables.add(new TableIdentifier("jobs"));
		tables.add(new TableIdentifier("job_history"));
		tables.add(new TableIdentifier("locations"));
		tables.add(new TableIdentifier("regions"));

		TableDependencySorter sorter = new TableDependencySorter(this.dbConn);
		List<TableIdentifier> result = sorter.sortForInsert(tables);

//		for (TableIdentifier t : result)
//		{
//			System.out.println(t.getTableName());
//		}
		assertEquals(7, result.size());
		assertTrue(tableIndex("departments", result) < tableIndex("employees", result));
		assertTrue(tableIndex("countries", result) > tableIndex("regions", result));
		assertTrue(tableIndex("jobs", result) < tableIndex("job_history", result));
	}

	@Test
	public void testDeleteRegions()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		InputStream in = DeleteScriptGeneratorTest.class.getResourceAsStream("gen_delete_schema.sql");
		Reader r = new InputStreamReader(in);
		String sql = FileUtil.readCharacters(r);

		TestUtil.executeScript(con, sql);

		TableDependencySorter sorter = new TableDependencySorter(con);

		List<TableIdentifier> toDelete2 = sorter.sortForDelete(CollectionUtil.arrayList(new TableIdentifier("countries")), true);
//		for (TableIdentifier tbl : toDelete2)
//		{
//			System.out.println(tbl.getTableName());
//		}
		assertTrue(tableIndex("countries", toDelete2) > tableIndex("stores", toDelete2));
		assertTrue(tableIndex("store_details", toDelete2) < tableIndex("stores", toDelete2));
		assertTrue("Wrong order for sto_details_item", tableIndex("sto_details_item", toDelete2) < tableIndex("store_details", toDelete2));
		assertTrue(tableIndex("sto_details_data", toDelete2) < tableIndex("store_details", toDelete2));
		assertTrue(tableIndex("regions", toDelete2) < tableIndex("countries", toDelete2));
		assertTrue(tableIndex("sales_mgr", toDelete2) < tableIndex("account_mgr", toDelete2));
		assertTrue(tableIndex("account_mgr", toDelete2) < tableIndex("region_mgr", toDelete2));
	}

	@Test
	public void testDeleteProductHierarchy()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		InputStream in = DeleteScriptGeneratorTest.class.getResourceAsStream("gen_delete_schema.sql");
		Reader r = new InputStreamReader(in);
		String sql = FileUtil.readCharacters(r);

		TestUtil.executeScript(con, sql);

		TableDependencySorter sorter = new TableDependencySorter(con);

		List<TableIdentifier> tables = CollectionUtil.arrayList(
			new TableIdentifier("vending_machines"),
			new TableIdentifier("sto_details_item"),
			new TableIdentifier("countries"),
			new TableIdentifier("account_mgr"),
			new TableIdentifier("sales_mgr"),
			new TableIdentifier("sto_details_data"),
			new TableIdentifier("regions"),
			new TableIdentifier("stores"),
			new TableIdentifier("region_mgr"),
			new TableIdentifier("store_details")
		);

		List<TableIdentifier> toDelete = sorter.sortForDelete(tables, false);

//		for (TableIdentifier tbl : toDelete)
//		{
//			System.out.println(tbl.getTableName());
//		}

		assertTrue(tableIndex("countries", toDelete) > tableIndex("stores", toDelete));
		assertTrue(tableIndex("store_details", toDelete) < tableIndex("stores", toDelete));
		assertTrue(tableIndex("sto_details_item", toDelete) < tableIndex("store_details", toDelete));
		assertTrue(tableIndex("sto_details_data", toDelete) < tableIndex("store_details", toDelete));
		assertTrue(tableIndex("regions", toDelete) < tableIndex("countries", toDelete));
		assertTrue(tableIndex("sales_mgr", toDelete) < tableIndex("account_mgr", toDelete));
		assertTrue(tableIndex("account_mgr", toDelete) < tableIndex("region_mgr", toDelete));
	}

	@Test
	public void testInsertProductHierarchy()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		InputStream in = DeleteScriptGeneratorTest.class.getResourceAsStream("gen_delete_schema.sql");
		Reader r = new InputStreamReader(in);
		String sql = FileUtil.readCharacters(r);

		TestUtil.executeScript(con, sql);

		TableDependencySorter sorter = new TableDependencySorter(con);

		List<TableIdentifier> tables = CollectionUtil.arrayList(
			new TableIdentifier("vending_machines"),
			new TableIdentifier("sto_details_item"),
			new TableIdentifier("countries"),
			new TableIdentifier("account_mgr"),
			new TableIdentifier("sales_mgr"),
			new TableIdentifier("sto_details_data"),
			new TableIdentifier("regions"),
			new TableIdentifier("stores"),
			new TableIdentifier("region_mgr"),
			new TableIdentifier("store_details")
		);

		List<TableIdentifier> toInsert = sorter.sortForInsert(tables);

//		for (TableIdentifier tbl : toInsert)
//		{
//			System.out.println(tbl.getTableName());
//		}

		assertTrue(tableIndex("countries", toInsert) < tableIndex("stores", toInsert));
		assertTrue(tableIndex("stores", toInsert) < tableIndex("store_details", toInsert));
		assertTrue(tableIndex("sto_details_item", toInsert) > tableIndex("store_details", toInsert));
		assertTrue(tableIndex("sto_details_data", toInsert) > tableIndex("store_details", toInsert));
		assertTrue(tableIndex("regions", toInsert) > tableIndex("countries", toInsert));
		assertTrue(tableIndex("sales_mgr", toInsert) > tableIndex("account_mgr", toInsert));
		assertTrue(tableIndex("account_mgr", toInsert) > tableIndex("region_mgr", toInsert));
	}

	@Test
	public void testMultiLevel()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		InputStream in = getClass().getResourceAsStream("gen_insert_schema.sql");
		Reader r = new InputStreamReader(in);
		String sql = FileUtil.readCharacters(r);

		TestUtil.executeScript(con, sql);
		TableIdentifier country = con.getMetadata().findTable(new TableIdentifier("COUNTRIES"));
		TableIdentifier regions = con.getMetadata().findTable(new TableIdentifier("REGIONS"));
		TableIdentifier prdDetails = con.getMetadata().findTable(new TableIdentifier("PRODUCT_DETAILS"));
		TableIdentifier products = new TableIdentifier("products");
		TableIdentifier salesMgr = new TableIdentifier("sales_mgr");
		TableIdentifier stores = new TableIdentifier("stores");
		TableIdentifier storeDetails = new TableIdentifier("store_details");
		TableDependencySorter sorter = new TableDependencySorter(con);
		List<TableIdentifier> tables = sorter.sortForInsert(CollectionUtil.arrayList(country, regions, prdDetails, stores, salesMgr, storeDetails, products));
//		System.out.println(tables);
		assertTrue(tableIndex("stores", tables) < tableIndex("store_details", tables));
		assertTrue(tableIndex("products", tables) < tableIndex("product_details", tables));
		assertTrue(tableIndex("regions", tables) < tableIndex("sales_mgr", tables));
	}

	private int tableIndex(String tableName, List<TableIdentifier> tables)
	{
		for (int i=0; i < tables.size(); i++)
		{
			if (tables.get(i).getTableName().equalsIgnoreCase(tableName)) return i;
		}
		throw new IllegalArgumentException("Table " + tableName + " not found!");
	}
}
