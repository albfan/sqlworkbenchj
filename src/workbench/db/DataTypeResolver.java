/*
 * DataTypeResolver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db;

/**
 * An interface to return the SQL code for a given JDBC data type.
 *
 * @author Thomas Kellerer
 */
public interface DataTypeResolver
{
  /**
   * Return a SQL for the indicated data type
   * @param dbmsName the name of the type
   * @param sqlType the numeric value from java.sql.Types
   * @param size the size of the column
   * @param digits the digits, &lt; 0 if not applicable
   * @return the SQL "display" for the given datatype
   */
  String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits);

  /**
   * Fixes data types returned by the JDBC driver to the correct one
   * @param type
   * @return the JDBC data type to be used instead
   */
  int fixColumnType(int type, String dbmsType);

  /**
   * Return the Java class to be used for the passed datatype.
   * If null is returned, the information from the driver is used (ResultSetMetaData.getColumnClassName())
   *
   * @param type the JDBC data type
   * @param dbmsType the DBMS data type name
   * @return null if the driver default should be used, a fully qualified classname otherwise
   */
  String getColumnClassName(int type, String dbmsType);
}
