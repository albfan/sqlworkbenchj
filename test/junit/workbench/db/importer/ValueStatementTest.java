/*
 * ValueStatementTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.importer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class ValueStatementTest
	extends WbTestCase
{

	public ValueStatementTest(String testName)
	{
		super(testName);
	}

	public void testStatementParsing()
	{
		String sql = "select max(id) from the_table where some_col = $2 and other_col = $14";
		ValueStatement stmt = new ValueStatement(sql);
		assertEquals(1, stmt.getIndexInStatement(2));
		assertEquals(2, stmt.getIndexInStatement(14));
		assertEquals("select max(id) from the_table where some_col = ? and other_col = ?", stmt.getSelectSQL());
		Set<Integer> indexes = stmt.getInputColumnIndexes();
		assertEquals(2, indexes.size());
		assertTrue(indexes.contains(new Integer(2)));
		assertTrue(indexes.contains(new Integer(14)));
	}
	
	public void testGetValue()
		throws Exception
	{
		TestUtil util = getTestUtil();
		
		String sql = "select max(id) from person where first_name = $7";
		ValueStatement stmt = new ValueStatement(sql);
		assertEquals(1, stmt.getIndexInStatement(7));
		String script = "CREATE TABLE person (id integer, first_name varchar(50), last_name varchar(50));\n" +
			"INSERT INTO person VALUES (1, 'Arthur', 'Dent');\n" +
			"INSERT INTO person VALUES (2, 'Zaphod', 'Beeblebrox');\n" +
			"COMMIT\n";
		WbConnection con = util.getConnection();
		try
		{
			TestUtil.executeScript(con, script);
			Map<Integer, Object> data = new HashMap<Integer, Object>();
			data.put(7, "Arthur");
			Object id = stmt.getDatabaseValue(con, data);
			assertNotNull(id);
			assertEquals(new Integer(1), id);
			stmt.done();
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
