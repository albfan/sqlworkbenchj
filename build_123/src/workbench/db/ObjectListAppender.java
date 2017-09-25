/*
 * ObjectListExtender.java
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


import workbench.storage.DataStore;

/**
 * An ObjectListAppender adds standard types to the list of objects retrieved by DbMetadata.getObjects().
 *
 * Once retrieved those objects don't need any further special treatment.
 *
 * For more specialized object types an ObjectListExtender should be used.
 *
 * @author Thomas Kellerer
 */
public interface ObjectListAppender
{
  /**
   * Adds the objects managed by this extender to the existing result.
   *
   * The DataStore's structure is the one created by DbMetadata.getObjects()
   *
   * @param con the database connection to be used
   * @param result the DataStore containing the objects already returned by the driver
   * @param requestedTypes the object types as passed to DbMetadata.getObjects()
   * @return true if the list was changed
   * @see DbMetadata#getObjects(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
   *
   */
  boolean extendObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes);

}
