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
package workbench.gui.dbobjects.objecttree;

import java.util.Set;

import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class GlobalTreeNode
    extends ObjectTreeNode
{
  private final Set<String> typesToShow = CollectionUtil.caseInsensitiveSet();

  public GlobalTreeNode(Set<String> showTypes)
  {
    super(ResourceMgr.getString("LblGlobalObjects"), TreeLoader.TYPE_GLOBAL);
    setAllowsChildren(true);
    setTypesToShow(showTypes);
  }

  public void setTypesToShow(Set<String> showTypes)
  {
    typesToShow.clear();
    typesToShow.addAll(showTypes);
  }

  @Override
  public boolean loadChildren(WbConnection connection)
  {
    if (connection == null) return false;
    DbSettings dbs = connection.getDbSettings();
    if (dbs == null) return false;
    DbMetadata meta = connection.getMetadata();
    if (meta == null) return false;

    Set<String> types = dbs.getGlobalObjectTypes();
    if (CollectionUtil.isEmpty(types)) return false;

    for (String type : types)
    {
      if (typesToShow.isEmpty() || typesToShow.contains(type))
      {
        GlobalTypeNode typeNode = new GlobalTypeNode(type);
        add(typeNode);
      }
    }
    setChildrenLoaded(true);
    return true;
  }
}
