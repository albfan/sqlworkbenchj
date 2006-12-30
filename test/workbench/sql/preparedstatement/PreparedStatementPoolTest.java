/*
 * PreparedStatementPoolTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.preparedstatement;

import java.sql.Statement;
import junit.framework.*;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

/**
 *
 * @author support@sql-workbench.net
 */
public class PreparedStatementPoolTest extends TestCase
{
	
	public PreparedStatementPoolTest(String testName)
	{
		super(testName);
	}

	public void testPool()
	{
		TestUtil util = new TestUtil(getClass().getName()+"_testPool");
		try
		{
			util.prepareEnvironment();
			WbConnection con = util.getConnection();
			Statement stmt = con.createStatement();
			stmt.executeUpdate("CREATE TABLE prep_test (nr integer, name varchar(100))");
			PreparedStatementPool pool = new PreparedStatementPool(con);
			boolean added = pool.addPreparedStatement("select * from prep_test");
			assertEquals("Statement without parameters was added", false, added);

			added = pool.addPreparedStatement("select * from prep_test where nr = '?'");
			assertEquals("Statement without parameters was added", false, added);
			
			String insert = "insert into prep_test (nr, name) values (?,?)";
			added = pool.addPreparedStatement(insert);
			assertEquals("INSERT statement was not added", true, added);
			
			String update = "update prep_test set name = 'test' where nr = ?";
			added = pool.addPreparedStatement(update);
			assertEquals("UPDATE statement was not added", true, added);
			
			StatementParameters p = pool.getParameters(insert);
			assertEquals("Incorrect number of parameters", 2, p.getParameterCount());
			assertEquals("Incorrect first parameter type", java.sql.Types.INTEGER, p.getParameterType(0));
			assertEquals("Incorrect second parameter type", java.sql.Types.VARCHAR, p.getParameterType(1));
			
			p = pool.getParameters(update);
			assertEquals("Incorrect number of parameters", 1, p.getParameterCount());			
			assertEquals("Incorrect parameter type", java.sql.Types.INTEGER, p.getParameterType(0));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	
}
