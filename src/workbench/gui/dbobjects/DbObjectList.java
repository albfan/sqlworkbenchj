/*
 * DbObjectList.java
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
package workbench.gui.dbobjects;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.interfaces.Reloadable;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;

/**
 * @author Thomas Kellerer
 */

public interface DbObjectList
	extends Reloadable
{
	TableIdentifier getObjectTable();
  TableDefinition getCurrentTableDefinition();
	List<DbObject> getSelectedObjects();
	WbConnection getConnection();
	Component getComponent();
  int getSelectionCount();

  public static class Util
  {
    public static List<TableIdentifier> getSelectedTableObjects(DbObjectList objectList)
    {
      if (objectList == null) return Collections.emptyList();

      WbConnection conn = objectList.getConnection();
      if (conn == null) return Collections.emptyList();

      DbMetadata meta = conn.getMetadata();
      if (meta == null) return Collections.emptyList();

      List<DbObject> objects = objectList.getSelectedObjects();
      if (CollectionUtil.isEmpty(objects)) return Collections.emptyList();

      List<TableIdentifier> tables = new ArrayList<>(objects.size());
      for (DbObject dbo : objects)
      {
        if (dbo instanceof TableIdentifier && meta.objectTypeCanContainData(dbo.getObjectType()))
        {
          tables.add((TableIdentifier)dbo);
        }
      }
      return tables;
    }
  }

}
