/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.importer.detector;

import java.io.File;
import java.sql.Types;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TextFileTableDetectorTest
  extends WbTestCase
{

  public TextFileTableDetectorTest()
  {
    super("TableDetectorTest");
  }

  @Before
  public void setUp()
  {
  }

  @After
  public void tearDown()
  {
  }

  @Test
  public void testAnalyzeFile()
    throws Exception
  {
    File data = new File(getTestUtil().getBaseDir(), "data.txt");
    TestUtil.writeFile(data,
      "id,firstname,lastname,dob,last_login,salary\n" +
      "4,Marvin,42,1000-01-01,2018-09-01 18:19:20,\n" +
      "2,Ford,Prefect,1975-01-01,2014-03-04 17:18:19,1234.567\n" +
      "3,Tricia,McMillan,1983-10-26,2015-09-01 18:19:20,4456.1\n" +
      "1,Arthur,Dent,1980-01-01,2015-07-06 14:15:16,42\n",
      "UTF-8");

    TextFileTableDetector detector = new TextFileTableDetector(data, ",", "\"", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", true, "UTF-8");
    detector.setSampleSize(100);
    detector.analyzeFile();
    List<ColumnIdentifier> columns = detector.getDBColumns();
    assertNotNull(columns);
    assertEquals(6,columns.size());
    for (int i=0; i < columns.size(); i++)
    {
      ColumnIdentifier col = columns.get(i);
      switch (i)
      {
        case 0:
          assertEquals("id", col.getColumnName());
          assertEquals(Types.INTEGER, col.getDataType());
          break;
        case 1:
          assertEquals("firstname", col.getColumnName());
          assertEquals(Types.VARCHAR, col.getDataType());
          assertEquals(6, col.getColumnSize());
          break;
        case 2:
          assertEquals("lastname", col.getColumnName());
          assertEquals(Types.VARCHAR, col.getDataType());
          assertEquals(8, col.getColumnSize());
          break;
        case 3:
          assertEquals("dob", col.getColumnName());
          assertEquals(Types.DATE, col.getDataType());
          break;
        case 4:
          assertEquals("last_login", col.getColumnName());
          assertEquals(Types.TIMESTAMP, col.getDataType());
          break;
        case 5:
          assertEquals("salary", col.getColumnName());
          assertEquals(Types.DECIMAL, col.getDataType());
          assertEquals(3, col.getDecimalDigits());
          break;
      }
    }
    detector.setTableName("csv_table");
    String create = detector.getCreateTable(null);
    assertNotNull(create);
    String expected =
      "create table csv_table\n" +
      "(\n" +
      "  id           integer,\n" +
      "  firstname    varchar(32767),\n" +
      "  lastname     varchar(32767),\n" +
      "  dob          date,\n" +
      "  last_login   timestamp,\n" +
      "  salary       decimal(8,3)\n" +
      ");";
    assertEquals(expected, create.trim().toLowerCase());
    detector.setAlwaysUseVarchar(true);
    detector.analyzeFile();
    create = detector.getCreateTable(null);
    assertNotNull(create);
    expected =
      "create table csv_table\n" +
      "(\n" +
      "  id           varchar(32767),\n" +
      "  firstname    varchar(32767),\n" +
      "  lastname     varchar(32767),\n" +
      "  dob          varchar(32767),\n" +
      "  last_login   varchar(32767),\n" +
      "  salary       varchar(32767)\n" +
      ");";
    assertEquals(expected, create.trim().toLowerCase());
  }

}
