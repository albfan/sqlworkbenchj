/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.SequenceDefinition;
import workbench.db.WbConnection;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerSequenceReaderTest
  extends WbTestCase
{

  public SqlServerSequenceReaderTest()
  {
    super("SqlServerSequenceReaderTest");
  }

  @Test
  public void testGetSource()
    throws Exception
  {
    WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
    assertNotNull(conn);
    try
    {
      String sql =
        "create sequence seq_aaa minvalue 42 maxvalue 10000 cycle;\n" +
        "create sequence seq_bbb no cache;\n" +
        "commit;";
      TestUtil.executeScript(conn, sql);
      String catalog = conn.getCurrentCatalog();
      String owner = conn.getCurrentSchema();

      SqlServerSequenceReader reader = new SqlServerSequenceReader(conn);
      List<SequenceDefinition> sequences = reader.getSequences(catalog, owner, null);
      assertNotNull(sequences);
      assertEquals(2, sequences.size());

      assertEquals("seq_aaa", sequences.get(0).getSequenceName());
      assertEquals("seq_bbb", sequences.get(1).getSequenceName());

      String source = sequences.get(0).getSource(conn).toString().trim();
      String expected =
        "CREATE SEQUENCE seq_aaa\n" +
        "       AS bigint\n" +
        "       INCREMENT BY 1\n" +
        "       MINVALUE 42\n" +
        "       MAXVALUE 10000\n" +
        "       CACHE \n" +
        "       CYCLE;";
      assertEquals(expected, source);

      source = sequences.get(1).getSource(conn).toString().trim();
      System.out.println(source);
      expected =
        "CREATE SEQUENCE seq_bbb\n" +
        "       AS bigint\n" +
        "       INCREMENT BY 1\n" +
        "       NO MINVALUE\n" +
        "       NO MAXVALUE\n" +
        "       NOCACHE\n" + 
        "       NOCYCLE;";
      assertEquals(expected, source);

    }
    finally
    {
      TestUtil.executeScript(conn,
        "drop sequence seq_aaa;\n" +
        "drop sequence seq_bbb;\n" +
        "commit;");
    }
  }


}
