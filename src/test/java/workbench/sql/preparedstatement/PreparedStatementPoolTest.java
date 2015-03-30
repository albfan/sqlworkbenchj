/*
 * PreparedStatementPoolTest.java
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
package workbench.sql.preparedstatement;

import java.sql.Statement;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class PreparedStatementPoolTest
	extends WbTestCase
{

	public PreparedStatementPoolTest()
	{
		super("PreparedStatementPoolTest");
	}

	@Test
	public void testPool()
		throws Exception
	{
		TestUtil util = getTestUtil();
		try
		{
			// Using HSQLDB as H2 does not implement getParameterMetaData() correctly
			WbConnection con = util.getHSQLConnection("testPool");
			Statement stmt = con.createStatement();
			stmt.executeUpdate("CREATE TABLE prep_test (nr integer, name varchar(100))");
			PreparedStatementPool pool = new PreparedStatementPool(con);
			boolean added = pool.addPreparedStatement("select * from prep_test");
			assertEquals("Statement without parameters was added", false, added);

			added = pool.addPreparedStatement("select * from prep_test where name = '?'");
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
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
