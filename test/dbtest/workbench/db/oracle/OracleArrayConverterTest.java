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
package workbench.db.oracle;

import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleArrayConverterTest
  extends WbTestCase
{
  public OracleArrayConverterTest()
  {
    super("OracleArrayConverterTest");
  }

  public void testVArray()
  {
    String sql =
      "CREATE OR REPLACE TYPE number_list AS VARRAY(10) OF number;\n" +
      "/\n" +
      "CREATE TABLE varray_table (id number, numbers number_list);\n" +
      "\n" +
      "INSERT INTO varray_table VALUES (1, number_list(1,2,3));\n" +
      "COMMIT;";

    // output format WBJUNIT.NUMBER_LIST(1,2,3)
  }

  public void testGeometry()
  {
    String sql =
      "CREATE TABLE testgeo\n" +
      "(\n" +
      "  id     NUMBER PRIMARY KEY,\n" +
      "  shape  sdo_geometry\n" +
      ");\n" +
      "INSERT INTO testgeo \n" +
      "  (id, shape) \n" +
      "VALUES \n" +
      "  (1,SDO_GEOMETRY(2003,NULL,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(1,1, 5,7)));\n" +
      "commit;\n ";
    // output format: MDSYS.SDO_GEOMETRY(2003, NULL, NULL, MDSYS.SDO_ELEM_INFO_ARRAY(1,1003,3), MDSYS.SDO_ORDINATE_ARRAY(1,1,5,7))
  }
}
