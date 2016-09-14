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
package workbench.db.postgres;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ObjectScripter;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PgObjectScripterTest
  extends WbTestCase
{
  private static final String SCHEMA = "scripter_test";

  public PgObjectScripterTest()
  {
    super("PgObjectScripterTest");
  }

  @BeforeClass
  public static void setUpClass()
    throws Exception
  {
    PostgresTestUtil.initTestCase(SCHEMA);
  }

  @AfterClass
  public static void tearDownClass()
    throws Exception
  {
    PostgresTestUtil.cleanUpTestCase();
  }

  @Test
  public void testTableOptions()
    throws Exception
  {
    WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

    TestUtil.executeScript(con,
      "create sequence code_seq;\n" +
      "create table base_table (code varchar(10) default concat('cd', to_char(nextval('code_seq'), '99999999')), some_data varchar(100));\n" +
      "alter sequence code_seq owned by base_table.code;\n" +
      "commit;\n");

    List<TableIdentifier> objects = con.getMetadata().getObjectList(SCHEMA, new String[] {"TABLE", "SEQUENCE" });
    ObjectScripter scripter = new ObjectScripter(objects, con);
    String script = scripter.getScript();
    ScriptParser parser = new ScriptParser(ParserType.Postgres);
    parser.setScript(script);
    assertEquals(4, parser.getSize()); // three statements and a COMMIT
    assertTrue(parser.getCommand(0).startsWith("CREATE SEQUENCE code_seq"));
    assertTrue(parser.getCommand(1).contains("nextval('code_seq'"));
    assertTrue(parser.getCommand(2).equals("ALTER SEQUENCE code_seq OWNED BY base_table.code"));
  }


}
