/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.sql.wbcommands;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;
import workbench.db.postgres.PostgresTestUtil;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static workbench.db.postgres.PostgresTestUtil.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PgCopyCommandTest
  extends WbTestCase
{

  public PgCopyCommandTest()
  {
    super("PgCopyCommandTest");
  }

	@Before
	public void setUpClass()
		throws Exception
	{
		WbConnection con = getPostgresConnection();
		dropAllObjects(con);
	}

	@After
	public void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

  @Test
  public void testExecute()
    throws Exception
  {
    WbConnection conn = PostgresTestUtil.getPostgresConnection();
    assertNotNull(conn);

    TestUtil.executeScript(conn,
      "create table person (id integer, firstname text, lastname text);\n" +
      "commit;");

    String copy =
      "copy person (id, firstname, lastname) from stdin with (format csv, delimiter ',');\n" +
      "1,Arthur,Dent\n" +
      "2,Ford,Prefect\n" +
      "\\.\n" +
      "commit;";

    // this tests the parser to strip the ending \. from the command
    ScriptParser parser = new ScriptParser(ParserType.Postgres);
    parser.setScript(copy);
    String parsedSql = parser.getCommand(0);

    assertEquals("commit", parser.getCommand(1));
    
    // PgCopyCommand needs a StatementRunner reference
    // so run this statement through one
    StatementRunner runner = new StatementRunner();
    runner.setConnection(conn);
    runner.runStatement(parsedSql);
    StatementRunnerResult result = runner.getResult();
    assertTrue(result.isSuccess());

    int count = TestUtil.getNumberValue(conn, "select count(*) from person");
    assertEquals(2,count);

    String name = (String)TestUtil.getSingleQueryValue(conn, "select firstname from person where id = 1");
    assertEquals("Arthur", name);
  }

}
