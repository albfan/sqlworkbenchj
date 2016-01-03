/*
 * H2ColumnEnhancerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.h2database;

import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import org.junit.AfterClass;
import org.junit.Test;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class H2ColumnEnhancerTest
	extends WbTestCase
{

	public H2ColumnEnhancerTest()
	{
		super("H2ColumnEnhancerTest");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testUpdateColumnDefinition()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		String sql = "CREATE TABLE expression_test (id integer, id2 integer as id * 2);";
		TestUtil.executeScript(con, sql);

		TableDefinition def = con.getMetadata().getTableDefinition(new TableIdentifier("EXPRESSION_TEST"));
		assertNotNull(def);
		List<ColumnIdentifier> cols = def.getColumns();
		assertNotNull(cols);
		assertEquals(2, cols.size());
		ColumnIdentifier col = cols.get(1);
		assertEquals("ID2", col.getColumnName());
		assertEquals("AS (ID * 2)", col.getComputedColumnExpression());
	}
}
