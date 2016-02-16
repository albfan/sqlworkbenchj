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

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.db.postgres.PostgresTestUtil;
import static workbench.db.postgres.PostgresTestUtil.dropAllObjects;
import static workbench.db.postgres.PostgresTestUtil.getPostgresConnection;

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
      "copy person from stdin with (format csv, delimiter ',');\n" +
      "1,Arthur,Dent\n" +
      "2,Ford,Prefect\n" +
      "\\.";

    TestUtil.executeScript(conn, copy);
    conn.commit();

    int count = TestUtil.getNumberValue(conn, "select count(*) from person");
    assertEquals(2,count);
  }

}
