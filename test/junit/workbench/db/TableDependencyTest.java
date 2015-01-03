/*
 * TableDependencyTest.java
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
package workbench.db;

import java.sql.Statement;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDependencyTest
	extends WbTestCase
{
	public TableDependencyTest()
	{
		super("TableDependencyTest");
	}

	protected WbConnection createRegularDB()
		throws Exception
	{
		TestUtil util = new TestUtil("dependencyTest");
		WbConnection dbConn = util.getConnection();
		String script =
		"CREATE TABLE base  \n" +
			 "( \n" +
			 "   id1  INTEGER NOT NULL, \n" +
			 "   id2  INTEGER NOT NULL, \n" +
			 "   primary key (id1, id2) \n" +
			 "); \n"+
		"CREATE TABLE child1 \n" +
			 "( \n" +
			 "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
			 "   base_id1    INTEGER NOT NULL, \n" +
			 "   base_id2    INTEGER NOT NULL, \n" +
			 "   FOREIGN KEY (base_id1, base_id2) REFERENCES base (id1,id2) \n" +
			 "); \n" +
		"CREATE TABLE child2 \n" +
			 "( \n" +
			 "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
			 "   base_id1    INTEGER NOT NULL, \n" +
			 "   base_id2    INTEGER NOT NULL, \n" +
			 "   FOREIGN KEY (base_id1, base_id2) REFERENCES base (id1,id2) \n" +
			 "); \n" +
		"CREATE TABLE child2_detail \n" +
			 "( \n" +
			 "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
			 "   child_id    INTEGER NOT NULL, \n" +
			 "   FOREIGN KEY (child_id) REFERENCES child2 (id) \n" +
			 "); \n"+
		"CREATE TABLE child1_detail \n" +
			 "( \n" +
			 "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
			 "   child1_id    INTEGER NOT NULL, \n" +
			 "   FOREIGN KEY (child1_id) REFERENCES child1 (id) \n" +
			 "); \n" +
		"CREATE TABLE child1_detail2 \n" +
			 "( \n" +
			 "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
			 "   detail_id    INTEGER NOT NULL, \n" +
			 "   FOREIGN KEY (detail_id) REFERENCES child1_detail (id) \n" +
			 ");\n" +
		"commit;\n";
		TestUtil.executeScript(dbConn, script);
		return dbConn;
	}

	@After
	public void tearDown()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testMultiSchema()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection dbConn = util.getConnection();
		String script =
			"create schema one;\n" +
			"create schema two;\n" +
			"create table one.t1 (id integer not null primary key);\n" +
			"create table one.t2 (id integer not null primary key, t1_id integer not null references one.t1(id));\n" +
			"create table two.t3 (id integer not null primary key, t2_id integer not null references one.t2(id));\n" +
			"commit;";
		TestUtil.executeScript(dbConn, script);
		try
		{
			TableIdentifier base = dbConn.getMetadata().findTable(new TableIdentifier("ONE.T1"));
			TableDependency dep = new TableDependency(dbConn, base);
			dep.setRetrieveDirectChildrenOnly(false);
			dep.readTreeForChildren();
			DependencyNode root = dep.getRootNode();
			assertNotNull("No root returned", root);

			List<DependencyNode> l1 = root.getChildren();
			assertNotNull(l1);
			assertEquals(1, l1.size());

			DependencyNode c1 = l1.get(0);
			assertNotNull(c1);
			assertEquals("T2", c1.getTable().getTableName());

			List<DependencyNode> l2 = c1.getChildren();
			assertNotNull(l2);
			assertEquals(1, l2.size());
			DependencyNode c2 = l2.get(0);
			assertNotNull(c2);
			assertEquals("T3", c2.getTable().getTableName());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testDependency()
		throws Exception
	{
		try
		{
			WbConnection dbConn = createRegularDB();
			TableIdentifier base = dbConn.getMetadata().findTable(new TableIdentifier("BASE"));

			TableDependency dep = new TableDependency(dbConn, base);
			dep.readTreeForChildren();
			DependencyNode root = dep.getRootNode();
			assertNotNull("No root returned", root);
			List<DependencyNode> leafs = dep.getLeafs();
			for (DependencyNode node : leafs)
			{
				int level = node.getLevel();
				String tbl = node.getTable().getTableName();
				if (tbl.equalsIgnoreCase("base"))
				{
					assertEquals("Wrong level for base table", 0, level);
				}
				if (tbl.equalsIgnoreCase("child1") || tbl.equalsIgnoreCase("child2"))
				{
					assertEquals("Wrong level for childX tables", 1, level);
				}
				if (tbl.equalsIgnoreCase("child1_detail") || tbl.equalsIgnoreCase("child2_detail"))
				{
					assertEquals("Wrong level for detail tables", 2, level);
				}
			}
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	protected WbConnection createTwoLevelCycleDB()
		throws Exception
	{
		TestUtil util = new TestUtil("dependencyCycleTest");
		WbConnection dbConn = util.getConnection();
		Statement stmt = dbConn.createStatement();

		String sql = null;

		sql = "CREATE TABLE base \n" +
					 "( \n" +
					 "   id          INTEGER NOT NULL PRIMARY KEY " +
					 ")";
		stmt.executeUpdate(sql);

		sql = "CREATE TABLE tbl1 \n" +
					 "( \n" +
					 "   id          INTEGER NOT NULL PRIMARY KEY, " +
					 "   base_id     INTEGER, " +
					 "   tbl2_id     integer," +
					 "   foreign key (base_id) references base (id) " +
					 ")";
		stmt.executeUpdate(sql);

		sql = "CREATE TABLE tbl2 \n" +
					 "( \n" +
					 "   id          INTEGER NOT NULL PRIMARY KEY," +
					 "   tbl1_id     integer " +
					 ")";
		stmt.executeUpdate(sql);

		sql = "alter table tbl1 add foreign key (tbl2_id) references tbl2 (id)";
		stmt.executeUpdate(sql);

		sql = "alter table tbl2 add foreign key (tbl1_id) references tbl1 (id)";
		stmt.executeUpdate(sql);

		return dbConn;
	}

	@Test
	public void testCycle()
		throws Exception
	{
		try
		{
			WbConnection con = createTwoLevelCycleDB();
			TableIdentifier tbl = new TableIdentifier("base");
			TableDependency dep = new TableDependency(con, tbl);
			dep.readTreeForChildren();
			assertEquals(false, dep.wasAborted());
			assertNotNull("No root returned", dep.getRootNode());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	protected WbConnection createSingleLevelCycleDB()
		throws Exception
	{
		TestUtil util = new TestUtil("dependencyCycleTest");
		WbConnection dbConn = util.getConnection();
		Statement stmt = dbConn.createStatement();

		String sql = null;

		sql = "CREATE TABLE tbl1 \n" +
					 "( \n" +
					 "   id          INTEGER NOT NULL PRIMARY KEY, " +
					 "   tbl2_id     integer" +
					 ")";
		stmt.executeUpdate(sql);

		sql = "CREATE TABLE tbl2 \n" +
					 "( \n" +
					 "   id          INTEGER NOT NULL PRIMARY KEY," +
					 "   tbl1_id     integer " +
					 ")";
		stmt.executeUpdate(sql);

		sql = "alter table tbl1 add foreign key (tbl2_id) references tbl2 (id)";
		stmt.executeUpdate(sql);

		sql = "alter table tbl2 add foreign key (tbl1_id) references tbl1 (id)";
		stmt.executeUpdate(sql);

		return dbConn;
	}

	@Test
	public void testDirectCycle()
		throws Exception
	{
		try
		{
			WbConnection con = createSingleLevelCycleDB();
			TableIdentifier tbl = new TableIdentifier("tbl1");
			TableDependency dep = new TableDependency(con, tbl);
			dep.readTreeForChildren();
			assertEquals(false, dep.wasAborted());
			assertNotNull("No root returned", dep.getRootNode());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testIndirectCycle()
		throws Exception
	{
		String sql = "CREATE TABLE user_account ( \n" +
             "  id integer NOT NULL , \n" +
             "  createdById integer DEFAULT NULL, \n" +
             "  createdWorkstationId integer DEFAULT NULL, \n" +
             "  lastChangedById integer DEFAULT NULL, \n" +
             "  lastChangedWorkstationId integer DEFAULT NULL, \n" +
             "  PRIMARY KEY (id) \n" +
             "); \n" +
             " \n" +
             "CREATE TABLE workstation ( \n" +
             "  id integer NOT NULL , \n" +
             "  createdById integer DEFAULT NULL, \n" +
             "  createdWorkstationId integer DEFAULT NULL, \n" +
             "  lastChangedById integer DEFAULT NULL, \n" +
             "  lastChangedWorkstationId integer DEFAULT NULL, \n" +
             "  workstationGroupId integer DEFAULT NULL, \n" +
             "  defaultInvoiceGroupId integer DEFAULT NULL, \n" +
             "  PRIMARY KEY (id) \n" +
             ");  \n" +
             " \n" +
             "CREATE TABLE invoicegroup ( \n" +
             "  id integer NOT NULL , \n" +
             "  createdById integer DEFAULT NULL, \n" +
             "  createdWorkstationId integer DEFAULT NULL, \n" +
             "  lastChangedById integer DEFAULT NULL, \n" +
             "  lastChangedWorkstationId integer DEFAULT NULL, \n" +
             "  PRIMARY KEY (id) \n" +
             "); \n" +
             " \n" +
             "ALTER TABLE user_account ADD FOREIGN KEY (createdWorkstationId) REFERENCES workstation (id); \n" +
             "ALTER TABLE user_account ADD FOREIGN KEY (lastChangedWorkstationId) REFERENCES workstation (id); \n" +
             "ALTER TABLE user_account ADD FOREIGN KEY (lastChangedById) REFERENCES user_account (id); \n" +
             "ALTER TABLE user_account ADD FOREIGN KEY (createdById) REFERENCES user_account (id); \n" +
             " \n" +
             "ALTER TABLE workstation ADD FOREIGN KEY (createdWorkstationId) REFERENCES workstation (id); \n" +
             "ALTER TABLE workstation ADD FOREIGN KEY (lastChangedWorkstationId) REFERENCES workstation (id); \n" +
             "ALTER TABLE workstation ADD FOREIGN KEY (lastChangedById) REFERENCES user_account (id); \n" +
             "ALTER TABLE workstation ADD FOREIGN KEY (createdById) REFERENCES user_account (id); \n" +
             "ALTER TABLE workstation ADD FOREIGN KEY (defaultInvoiceGroupId) REFERENCES invoicegroup (id); \n" +
             " \n" +
             "ALTER TABLE invoicegroup ADD FOREIGN KEY (createdWorkstationId) REFERENCES workstation (id); \n" +
             "ALTER TABLE invoicegroup ADD FOREIGN KEY (lastChangedWorkstationId) REFERENCES workstation (id); \n" +
             "ALTER TABLE invoicegroup ADD FOREIGN KEY (lastChangedById) REFERENCES user_account (id); \n" +
             "ALTER TABLE invoicegroup ADD FOREIGN KEY (createdById) REFERENCES user_account (id); ";

		WbConnection con = getTestUtil().getConnection();
		TestUtil.executeScript(con, sql);

		try
		{
			TableIdentifier tbl = new TableIdentifier("user_account");
			TableDependency dep = new TableDependency(con, tbl);
			dep.readTreeForChildren();
			assertEquals(false, dep.wasAborted());
			assertNotNull("No root returned", dep.getRootNode());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testCycle2()
		throws Exception
	{
		String sql =
			"create table main  \n" +
			"( \n" +
			"   BASE integer not null,  \n" +
			"   REGION integer not null,  \n" +
			"   YEAR integer not null,  \n" +
			"   MOVEMENTNUM integer not null, \n" +
			"   location integer not null, \n" +
			"   store integer not null, \n" +
			"   purchorg integer not null, \n" +
			"   ordernum integer not null, \n" +
			"   DELIVERYPLACE integer not null, \n" +
			"   ADDRPOOL integer not null,  \n" +
			"   num integer \n" +
			"); \n" +
			" \n" +
			"ALTER TABLE main \n" +
			"   ADD CONSTRAINT PK_main PRIMARY KEY (BASE, REGION, YEAR, MOVEMENTNUM); \n" +
			" \n" +
			"create table STORE  \n" +
			"( \n" +
			"  BASE integer not null, \n" +
			"  REGION integer not null,  \n" +
			"  LOCATION integer not null, \n" +
			"  STORE integer not null, \n" +
			"  constraint pk_store primary key (base, region, location, store) \n" +
			"); \n" +
			" \n" +
			"create table ORDERS  \n" +
			"( \n" +
			"  BASE integer not null, \n" +
			"  PURCHORG integer not null,  \n" +
			"  ORDERNUM integer not null,  \n" +
			"  primary key (base, purchorg, ordernum) \n" +
			"); \n" +
			" \n" +
			"create table REGION  \n" +
			"(  \n" +
			"   BASE integer not null, \n" +
			"   REGION integer not null, \n" +
			"   primary key (base,region) \n" +
			"); \n" +
			" \n" +
			"create table DELIVERYPLACE  \n" +
			"( \n" +
			"  BASE integer not null, \n" +
			"  REGION integer not null, \n" +
			"  LOCATION integer not null, \n" +
			"  STORE integer not null, \n" +
			"  DELIVERYPLACE integer not null,  \n" +
			"  primary key (BASE,REGION,LOCATION,STORE,DELIVERYPLACE) \n" +
			"); \n" +
			" \n" +
			"create table CONTACTPERSON  \n" +
			"( \n" +
			"   BASE integer not null, \n" +
			"   ADDRPOOL integer not null, \n" +
			"   num integer not null,  \n" +
			"   primary key (BASE,ADDRPOOL,num) \n" +
			"); \n" +
			" \n" +
			"ALTER TABLE main \n" +
			"  ADD CONSTRAINT F_main_STORE FOREIGN KEY (BASE, REGION, LOCATION, STORE) \n" +
			"  REFERENCES STORE (BASE,REGION,LOCATION,STORE); \n" +
			" \n" +
			"ALTER TABLE main \n" +
			"  ADD CONSTRAINT F_main_ORDER FOREIGN KEY (BASE, PURCHORG, ORDERNUM) \n" +
			"  REFERENCES ORDERS (BASE,PURCHORG,ORDERNUM); \n" +
			" \n" +
			"ALTER TABLE main \n" +
			"  ADD CONSTRAINT F_main_REGION FOREIGN KEY (BASE, REGION) \n" +
			"  REFERENCES REGION (BASE,REGION); \n" +
			" \n" +
			"ALTER TABLE main \n" +
			"  ADD CONSTRAINT F_main_DELIVERYPLACE FOREIGN KEY (BASE, REGION, LOCATION, STORE, DELIVERYPLACE) \n" +
			"  REFERENCES DELIVERYPLACE (BASE,REGION,LOCATION,STORE,DELIVERYPLACE); \n" +
			" \n" +
			"ALTER TABLE main \n" +
			"  ADD CONSTRAINT F_main_CONTACTPERSON FOREIGN KEY (BASE, ADDRPOOL, num) \n" +
			"  REFERENCES CONTACTPERSON (BASE,ADDRPOOL,num);\n" +
			"commit;";

		WbConnection con = getTestUtil().getConnection();
		TestUtil.executeScript(con, sql);

		try
		{
			TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("main"));
			TableDependency dep = new TableDependency(con, tbl);
			dep.readTreeForParents();
			assertEquals(false, dep.wasAborted());
			assertNotNull("No root returned", dep.getRootNode());
			List<DependencyNode> children = dep.getRootNode().getChildren();
			assertEquals(5, children.size());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testGetOutgoingFks()
		throws Exception
	{
		WbConnection con = createRegularDB();
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("child2"));

		TableDependency dep = new TableDependency(con);
		dep.setMainTable(tbl);
		List<DependencyNode> fklist = dep.getOutgoingForeignKeys();
		assertNotNull(fklist);
		assertEquals(1, fklist.size());
		assertEquals("BASE", fklist.get(0).getTable().getTableName());

		tbl = con.getMetadata().findTable(new TableIdentifier("child2_detail"));
		dep.setMainTable(tbl);
		fklist = dep.getOutgoingForeignKeys();
		assertNotNull(fklist);
		assertEquals(1, fklist.size());
		assertEquals("CHILD2", fklist.get(0).getTable().getTableName());
	}
}
