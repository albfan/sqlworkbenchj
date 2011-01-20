/*
 * H2ColumnEnhancerTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
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
