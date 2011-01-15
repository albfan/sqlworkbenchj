/*
 * ClientSideTableSearcherTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.search;

import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.TableSearchConsumer;
import workbench.storage.DataStore;
import workbench.storage.filter.ContainsComparator;
import workbench.util.CollectionUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 *
 * @author Thomas Kellerer
 */
public class ClientSideTableSearcherTest
	extends WbTestCase
{
	private WbConnection con;

	public ClientSideTableSearcherTest()
	{
		super("ClientSideTableSearcherTest");
	}

	@Before
	public void setUp()
		throws Exception
	{
		TestUtil util = getTestUtil();
		con = util.getConnection();
		String script =
			"CREATE TABLE person (nr integer, firstname varchar(20), lastname varchar(20));\n" +
			"INSERT INTO person (nr, firstname, lastname) VALUES (1, 'Arthur', 'Dent');\n" +
			"INSERT INTO person (nr, firstname, lastname) VALUES (2, 'Zaphod', 'Beeblebrox');\n" +
			"INSERT INTO person (nr, firstname, lastname) VALUES (3, 'Ford', 'Prefect');\n" +
			"INSERT INTO person (nr, firstname, lastname) VALUES (4, 'Tricia', 'McMillian');\n" +
			"commit;\n" +
			"CREATE TABLE ship (id integer, shipname varchar(50));\n" +
			"INSERT INTO ship (id, shipname) VALUES (1, 'Heart of Gold');\n" +
			"INSERT INTO ship (id, shipname) VALUES (2, 'Spaceship Titanic');\n" +
			"INSERT INTO ship (id, shipname) VALUES (3, 'Vogon Planet Destructor');\n" +
			"INSERT INTO ship (id, shipname) VALUES (4, 'Stuntship');\n" +
			"INSERT INTO ship (id, shipname) VALUES (5, 'Heart of Gold 2');\n" +
			"commit;\n" +
			"CREATE TABLE document (id integer, title varchar(100), content CLOB);\n" +
			"INSERT INTO document (id, title, content) VALUES (1, 'Heart of Gold', '<content>some stuff</content>');\n" +
			"INSERT INTO document (id, title, content) VALUES (2, 'Spaceship', '<content>spaceship details</content>');\n" +
			"commit;\n";

		TestUtil.executeScript(con, script);
	}

	@After
	public void tearDown()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testSearch()
	{
		ClientSideTableSearcher searcher = new ClientSideTableSearcher();
		searcher.setConnection(con);
		searcher.setCriteria("dent", true);
		List<TableIdentifier> tables = CollectionUtil.arrayList(
			new TableIdentifier("PERSON"),
			new TableIdentifier("SHIP")
		);

		searcher.setTableNames(tables);
		SearchConsumer consumer = new SearchConsumer();
		searcher.setConsumer(consumer);
		searcher.setComparator(new ContainsComparator());
		searcher.search();

		List<DataStore> searchResult = consumer.getResults();
		assertNotNull(searchResult);
		assertEquals(2, searchResult.size());
		assertEquals(1, searchResult.get(0).getRowCount());
		assertEquals(0, searchResult.get(1).getRowCount());

		searcher.setCriteria("2", true);
		searcher.search();
		searchResult = consumer.getResults();
		assertNotNull(searchResult);
		assertEquals(2, searchResult.size());
		assertEquals(1, searchResult.get(0).getRowCount());
		assertEquals(2, searchResult.get(1).getRowCount());

		// Check exclusion of CLOB column
		tables = CollectionUtil.arrayList(new TableIdentifier("DOCUMENT"));
		searcher.setTableNames(tables);
		searcher.setExcludeLobColumns(true);
		searcher.setCriteria("stuff", true);
		searcher.search();

		searchResult = consumer.getResults();
		assertNotNull(searchResult);
		assertEquals(1, searchResult.size());
		assertEquals(0, searchResult.get(0).getRowCount());

		searcher.setExcludeLobColumns(false);
		searcher.search();

		searchResult = consumer.getResults();
		assertNotNull(searchResult);
		assertEquals(1, searchResult.size());
		assertEquals(1, searchResult.get(0).getRowCount());


		tables = CollectionUtil.arrayList(
			new TableIdentifier("PERSON"),
			new TableIdentifier("SHIP")
		);
		searcher.setCriteria("dent", false);
		searcher.setTableNames(tables);
		searcher.search();
		searchResult = consumer.getResults();
		assertNotNull(searchResult);
		assertEquals(2, searchResult.size());
		assertEquals(0, searchResult.get(0).getRowCount());
		assertEquals(0, searchResult.get(1).getRowCount());

		tables = CollectionUtil.arrayList(
			new TableIdentifier("PERSON")
		);
		searcher.setTableNames(tables);
		searcher.setCriteria("Dent", false);
		searcher.search();
		searchResult = consumer.getResults();
		assertNotNull(searchResult);
		assertEquals(1, searchResult.size());
		assertEquals(1, searchResult.get(0).getRowCount());
	}

	private class SearchConsumer
		implements TableSearchConsumer
	{
		private List<DataStore> results = CollectionUtil.arrayList();

		public List<DataStore> getResults()
		{
			return results;
		}

		public void setCurrentTable(String aTablename, String aStatement)
		{
		}

		public void error(String msg)
		{
		}

		public void tableSearched(TableIdentifier table, DataStore result)
		{
			results.add(result);
		}

		public void setStatusText(String aStatustext)
		{
		}

		public void searchStarted()
		{
			results.clear();
		}

		public void searchEnded()
		{
		}

	}
}
