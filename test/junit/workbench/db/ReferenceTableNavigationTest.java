/*
 * ReferenceTableNavigationTest.java
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
package workbench.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import workbench.TestUtil;
import workbench.storage.ColumnData;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.util.SqlUtil;


/**
 * @author Thomas Kellerer
 */
public class ReferenceTableNavigationTest
{

	@Test
	public void testChildNavigation()
		throws Exception
	{
		TestUtil util = new TestUtil("childNav");
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			WbConnection con = util.getConnection();
			Connection sqlCon = con.getSqlConnection();
			stmt = sqlCon.createStatement();
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
             "   CONSTRAINT fk_child1_base FOREIGN KEY (base_id1, base_id2) REFERENCES base (id1,id2) \n" +
             ")";
			stmt.execute(child1Sql);

			String child2Sql = "CREATE TABLE child2 \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   base_id1    INTEGER NOT NULL, \n" +
             "   base_id2    INTEGER NOT NULL, \n" +
             "   CONSTRAINT fk_child2_base FOREIGN KEY (base_id1, base_id2) REFERENCES base (id1,id2) \n" +
             ")";
			stmt.execute(child2Sql);

			String child3Sql = "CREATE TABLE child2_detail \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   child_id    INTEGER NOT NULL, \n" +
             "   FOREIGN KEY (child_id) REFERENCES child2 (id) \n" +
             ")";
			stmt.execute(child3Sql);

			stmt.execute("insert into base (id1, id2) values (1,1)");
			stmt.execute("insert into base (id1, id2) values (2,2)");
			stmt.execute("insert into base (id1, id2) values (3,3)");

			// child records for base(1,1)
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (1,1,1)");

			// child1 records for base(2,2)
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (2,2,2)");
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (3,2,2)");

			// child1 records for base(3,3)
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (4,3,3)");
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (5,3,3)");
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (6,3,3)");

			// child records for base(1,1)
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (201,1,1)");

			// child records for base(2,2)
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (202,2,2)");
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (203,2,2)");

			// child records for base(3,3)
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (204,3,3)");
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (205,3,3)");
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (206,3,3)");

			List<List<ColumnData>> rows = new LinkedList<List<ColumnData>>();

			List<ColumnData> row = new LinkedList<ColumnData>();
			row.add(new ColumnData(Integer.valueOf(1), new ColumnIdentifier("id1",java.sql.Types.INTEGER)));
			row.add(new ColumnData(Integer.valueOf(1), new ColumnIdentifier("id2",java.sql.Types.INTEGER)));
			rows.add(row);

			row = new LinkedList<ColumnData>();
			row.add(new ColumnData(Integer.valueOf(2), new ColumnIdentifier("id1",java.sql.Types.INTEGER)));
			row.add(new ColumnData(Integer.valueOf(2), new ColumnIdentifier("id2",java.sql.Types.INTEGER)));
			rows.add(row);

			TableIdentifier base = new TableIdentifier("base");

			ReferenceTableNavigation nav = new ReferenceTableNavigation(base, con);
			nav.readTreeForChildren();

			TableIdentifier t1 = new TableIdentifier("child1");
			t1.adjustCase(con);

			String select = nav.getSelectForChild(t1, "fk_child1_base", rows);
			assertNotNull("Select for Child1 not created", select);

//			System.out.println("select child1 with:" + select);
			rs = stmt.executeQuery(select);
			int count = 0;
			while (rs.next())
			{
				count ++;
				int id = rs.getInt(1);
				int bid1 = rs.getInt(2);
				int bid2 = rs.getInt(3);
				if (id == 1)
				{
					assertEquals(1, bid1);
					assertEquals(1, bid2);
				}
				else if (id == 2 || id == 3)
				{
					assertEquals(2, bid1);
					assertEquals(2, bid2);
				}
				else
				{
					fail("Incorrect id = " + id + " returned from SELECT");
				}
			}
			assertEquals(3, count);
			rs.close();

			TableIdentifier t2 = new TableIdentifier("child2");
			t2.adjustCase(con);

			String select2 = nav.getSelectForChild(t2, "fk_child2_base", rows);
			assertNotNull("Child table 2 not found", select2);

//			System.out.println("select child2 with:" + select2);
			rs = stmt.executeQuery(select2);
			count = 0;
			while (rs.next())
			{
				count ++;
				int id = rs.getInt(1);
				int bid1 = rs.getInt(2);
				int bid2 = rs.getInt(3);
				if (id == 201)
				{
					assertEquals(1, bid1);
					assertEquals(1, bid2);
				}
				else if (id == 202 || id == 203)
				{
					assertEquals(2, bid1);
					assertEquals(2, bid2);
				}
				else
				{
					fail("Incorrect id = " + id + " returned from SELECT");
				}
			}
			assertEquals(3, count);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testDoubleLink()
		throws Exception
	{
		TestUtil util = new TestUtil("TwoLinks");

		WbConnection con = util.getConnection();
		String script = "CREATE TABLE base  \n" +
					 "( \n" +
					 "   id  INTEGER NOT NULL, \n" +
					 "   data  VARCHAR(10), \n" +
					 "   primary key (id) \n" +
					 "); \n" +
					 "\n" +
					 "CREATE TABLE link_table \n" +
					 "( \n" +
					 "   link_id INTEGER NOT NULL, \n" +
					 "   \"original id\" INTEGER NOT NULL, \n" +
					 "   CONSTRAINT link_id_fk FOREIGN KEY (link_id) REFERENCES base (id), \n" +
					 "   CONSTRAINT org_id_fk FOREIGN KEY (\"original id\") REFERENCES base (id) \n" +
					 "); \n" +
					 "INSERT INTO base (id, data) VALUES (1, 'one');\n" +
					 "INSERT INTO base (id, data) VALUES (2, 'two');\n" +
					 "INSERT INTO link_table (link_id, \"original id\") vALUES (1,2);\n" +
					 "COMMIT;\n";
		TestUtil.executeScript(con, script);
		ReferenceTableNavigation nav = new ReferenceTableNavigation(new TableIdentifier("base"), con);
		nav.readTreeForChildren();
//		TableDependency dep = nav.getTree();
//		List<DependencyNode> tables = dep.getLeafs();
//		System.out.println("***************");
//		for (DependencyNode node : tables)
//		{
//			System.out.println(node.toString());
//		}
		ColumnData col1 = new ColumnData(Integer.valueOf(1), new ColumnIdentifier("ID"));
		List<ColumnData> row = new ArrayList<ColumnData>();
		row.add(col1);
		List<List<ColumnData>> all = new ArrayList<List<ColumnData>>();
		all.add(row);
		String sql = nav.getSelectForChild(new TableIdentifier("link_table"), "link_id_fk", all);
		//System.out.println("******\n" + sql);
		assertTrue(sql.indexOf("(LINK_ID = 1)") > -1);

		sql = nav.getSelectForChild(new TableIdentifier("link_table"), "org_id_fk", all);
		//System.out.println("******\n" + sql);
		assertTrue(sql.indexOf("(\"original id\" = 1)") > -1);
	}

	@Test
	public void testParentNavigation()
		throws Exception
	{
		TestUtil util = new TestUtil("parentNav");
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			WbConnection con = util.getConnection();
			Connection sqlCon = con.getSqlConnection();
			stmt = sqlCon.createStatement();
			String baseSql = "CREATE TABLE base  \n" +
             "( \n" +
             "   id1  INTEGER NOT NULL, \n" +
             "   id2  INTEGER NOT NULL, \n" +
             "   data  VARCHAR(10), \n" +
             "   primary key (id1, id2) \n" +
             ")";
			stmt.execute(baseSql);
			String child1Sql = "CREATE TABLE child1 \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   base_id1    INTEGER NOT NULL, \n" +
             "   base_id2    INTEGER NOT NULL, \n" +
             "   CONSTRAINT fk_child1_base FOREIGN KEY (base_id1, base_id2) REFERENCES base (id1,id2) \n" +
             ")";
			stmt.execute(child1Sql);

			String child2Sql = "CREATE TABLE child2 \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   base_id1    INTEGER NOT NULL, \n" +
             "   base_id2    INTEGER NOT NULL, \n" +
             "   CONSTRAINT fk_child2_base FOREIGN KEY (base_id1, base_id2) REFERENCES base (id1,id2) \n" +
             ")";
			stmt.execute(child2Sql);

			String child3Sql = "CREATE TABLE child2_detail \n" +
             "( \n" +
             "   id          INTEGER NOT NULL PRIMARY KEY, \n" +
             "   child_id    INTEGER NOT NULL, \n" +
             "   CONSTRAINT fk_child2d_base FOREIGN KEY (child_id) REFERENCES child2 (id) \n" +
             ")";
			stmt.execute(child3Sql);

			stmt.execute("insert into base (id1, id2, data) values (1,1, 'one')");
			stmt.execute("insert into base (id1, id2, data) values (2,2, 'two')");
			stmt.execute("insert into base (id1, id2, data) values (3,3, 'three')");

			// child records for base(1,1)
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (1,1,1)");

			// child1 records for base(2,2)
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (2,2,2)");
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (3,2,2)");

			// child1 records for base(3,3)
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (4,3,3)");
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (5,3,3)");
			stmt.execute("insert into child1 (id, base_id1,base_id2) values (6,3,3)");

			// child records for base(1,1)
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (201,1,1)");

			// child records for base(2,2)
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (202,2,2)");
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (203,2,2)");

			// child records for base(3,3)
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (204,3,3)");
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (205,3,3)");
			stmt.execute("insert into child2 (id, base_id1,base_id2) values (206,3,3)");

			List<List<ColumnData>> rows = new LinkedList<List<ColumnData>>();

			List<ColumnData> row = new LinkedList<ColumnData>();
			row.add(new ColumnData(Integer.valueOf(1), new ColumnIdentifier("base_id1",java.sql.Types.INTEGER)));
			row.add(new ColumnData(Integer.valueOf(1), new ColumnIdentifier("base_id2",java.sql.Types.INTEGER)));
			rows.add(row);

			row = new LinkedList<ColumnData>();
			row.add(new ColumnData(Integer.valueOf(2), new ColumnIdentifier("base_id1",java.sql.Types.INTEGER)));
			row.add(new ColumnData(Integer.valueOf(2), new ColumnIdentifier("base_id2",java.sql.Types.INTEGER)));
			rows.add(row);

			TableIdentifier base = new TableIdentifier("child1");
			ReferenceTableNavigation nav = new ReferenceTableNavigation(base, con);
			nav.readTreeForParents();
			TableDependency tree = nav.getTree();
			assertNotNull("No parent found!", tree);
			List<DependencyNode> leafs = tree.getLeafs();
			assertNotNull("No leafs for parent!", leafs);
			assertEquals("No leafs for parent!", 1, leafs.size());

			TableIdentifier tbl = new TableIdentifier("base");
			String select1 = nav.getSelectForParent(tbl, "fk_child1_base", rows);

//			System.out.println("select parent with = " + select1);
			rs = stmt.executeQuery(select1);
			int count = 0;
			while (rs.next())
			{
				count ++;
				int bid1 = rs.getInt("id1");
				int bid2 = rs.getInt("id2");
				String data = rs.getString("data");

				if ("one".equals(data))
				{
					assertEquals(1, bid1);
					assertEquals(1, bid2);
				}
				else if ("two".equals(data))
				{
					assertEquals(2, bid1);
					assertEquals(2, bid2);
				}
				else
				{
					fail("Incorrect row = with (" + bid1 + "," + bid2 + ") returned from base table");
				}
			}
			assertEquals(2, count);
			rs.close();

		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
