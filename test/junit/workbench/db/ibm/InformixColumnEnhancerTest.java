/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.ibm;

import java.sql.Types;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Thomas Kellerer
 */
public class InformixColumnEnhancerTest
	extends WbTestCase
{

	public InformixColumnEnhancerTest()
	{
		super("InformixColumnEnhancerTest");
	}

	@Test
	public void testGetQualifier()
	{
		InformixColumnEnhancer enhancer = new InformixColumnEnhancer();
		assertEquals("YEAR TO MINUTE", enhancer.getQualifier(3080));
		assertEquals("YEAR TO SECOND", enhancer.getQualifier(3594));
		assertEquals("HOUR TO SECOND", enhancer.getQualifier(1642));
		assertEquals("MONTH TO DAY", enhancer.getQualifier(1060));
		assertEquals("YEAR TO DAY", enhancer.getQualifier(2052));
	}

	@Test
	public void testUpdateColumns()
		throws Exception
	{
		TestUtil util = getTestUtil("IFXDB");

		try
		{
			WbConnection con = util.getConnection("IFXDB");
			TestUtil.executeScript(con,
				"create schema \"informix\";\n" +
				"create table \"informix\".\"systables\" \n" +
				"(\n" +
				"  tabid integer not null primary key,\n" +
				"  tabname varchar(50),\n" +
				"  owner varchar(50)\n" +
				");\n" +
				"\n" +
				"create table \"informix\".\"syscolumns\"\n" +
				"(\n" +
				"  tabid integer,\n" +
				"  colname varchar(50),\n" +
				"  coltype integer,\n" +
				"  collength integer \n" +
				");\n" +
				"\n" +
				"\n" +
				"insert into \"informix\".\"systables\" \n" +
				"   (tabid, tabname, owner) \n" +
				"values (1, 'DATE_TEST', 'PUBLIC');\n" +
				"\n" +
				"insert into \"informix\".\"syscolumns\" \n" +
				"  (tabid, colname, coltype, collength) \n" +
				"values \n" +
				"(1, 'DATE_ONE', 10, 3080),\n" +
				"(1, 'DATE_TWO', 10, 1642),\n" +
				"(1, 'INTERVAL_ONE', 14, 1060),\n" +
				"(1, 'INTERVAL_TWO', 270, 2052);\n" +
				"commit;\n"
			);
			TableIdentifier tbl = new TableIdentifier("IFXDB", "PUBLIC", "DATE_TEST");
			ColumnIdentifier d1 = new ColumnIdentifier("DATE_ONE", Types.TIMESTAMP);
			d1.setDbmsType("DATETIME");

			ColumnIdentifier d2 = new ColumnIdentifier("DATE_TWO", Types.TIMESTAMP);
			d2.setDbmsType("DATETIME");

			ColumnIdentifier i1 = new ColumnIdentifier("INTERVAL_ONE", Types.CHAR);
			i1.setDbmsType("INTERVAL");

			ColumnIdentifier i2 = new ColumnIdentifier("INTERVAL_TWO", Types.CHAR);
			i2.setDbmsType("interval(2052)");

			List<ColumnIdentifier> cols = CollectionUtil.arrayList(d1, d2, i1, i2);
			TableDefinition def = new TableDefinition(tbl, cols);
			InformixColumnEnhancer enhancer = new InformixColumnEnhancer();
			enhancer.updateColumnDefinition(def, con);
			assertEquals("DATETIME YEAR TO MINUTE", d1.getDbmsType());
			assertEquals("DATETIME HOUR TO SECOND", d2.getDbmsType());
			assertEquals("INTERVAL MONTH TO DAY", i1.getDbmsType());
			assertEquals("interval YEAR TO DAY", i2.getDbmsType());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}

	}
}
