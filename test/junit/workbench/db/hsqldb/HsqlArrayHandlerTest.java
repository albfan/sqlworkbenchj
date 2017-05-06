/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017 Thomas Kellerer.
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
package workbench.db.hsqldb;


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
public class HsqlArrayHandlerTest
  extends WbTestCase
{

  public HsqlArrayHandlerTest()
  {
    super("HsqlArrayHandlerTest");
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
  public void testGetBaseType()
    throws Exception
  {
    ColumnIdentifier col = new ColumnIdentifier("xlist", java.sql.Types.ARRAY);
    col.setDbmsType("VARCHAR(10) ARRAY[10]");
    HsqlArrayHandler handler = new HsqlArrayHandler(null);

    String baseType = handler.getBaseType(col);
    assertEquals("VARCHAR", baseType);

    col = new ColumnIdentifier("xlist", java.sql.Types.ARRAY);
    col.setDbmsType("INTEGER ARRAY");
    baseType = handler.getBaseType(col);
    assertEquals("INTEGER", baseType);
  }

  @Test
  public void createArray()
    throws Exception
  {
    String input = "[1,2,3]";
    HsqlArrayHandler handler = new HsqlArrayHandler(null);
    Object[] data = handler.createArray(input, "INTEGER");
    assertNotNull(data);
    assertEquals(3, data.length);
    assertTrue(data[0] instanceof Number);

    input = "['foo','arthur''s house']";
    data = handler.createArray(input, "VARCHAR");
    assertNotNull(data);
    assertEquals(2, data.length);
    assertEquals("foo", data[0]);
    assertEquals("arthur's house", data[1]);
  }
}
