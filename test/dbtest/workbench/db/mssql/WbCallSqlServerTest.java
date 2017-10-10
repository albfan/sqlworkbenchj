/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.DelimiterDefinition;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCallSqlServerTest
  extends WbTestCase
{
  public WbCallSqlServerTest()
  {
    super("WbCallSqlServerTest");
  }

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
    WbConnection con = SQLServerTestUtil.getSQLServerConnection();
    SQLServerTestUtil.dropAllObjects(con);
  }

  @Test
  public void testMultipleResults()
    throws Exception
  {
    WbConnection con = SQLServerTestUtil.getSQLServerConnection();
    assertNotNull(con);

    String sql =
      "create table person (id integer, firstname varchar(100), lastname varchar(100));\n" +
      "insert into person values (1, 'Arthur', 'Dent'), (2, 'Zaphod', 'Beeblebrox');\n" +
      "commit;";

    TestUtil.executeScript(con, sql);

    String proc =
      "create procedure multi_result\n" +
      "as\n" +
      "  select * from person where id = 1;\n" +
      "  select * from person where id = 2;\n" +
      "GO\n";
    TestUtil.executeScript(con, proc, DelimiterDefinition.DEFAULT_MS_DELIMITER);


		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
    StatementRunnerResult result = runner.runStatement("exec multi_result");
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(2, result.getDataStores().size());
    assertEquals(1, result.getDataStores().get(0).getValueAsInt(0, 0, -1));
    assertEquals(2, result.getDataStores().get(1).getValueAsInt(0, 0, -1));
  }
}
