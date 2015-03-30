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
package workbench.gui.dbobjects.objecttree.vertica;

import workbench.log.LogMgr;

import workbench.db.WbConnection;
import workbench.db.vertica.VerticaProjectionReader;

import workbench.gui.dbobjects.objecttree.ObjectTreeNode;
import workbench.gui.dbobjects.objecttree.TreeLoader;

import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class BuddyListNode
  extends ObjectTreeNode
{
  public BuddyListNode()
  {
    super("Buddies", TreeLoader.TYPE_PROJECTION_BUDDIES);
    setAllowsChildren(true);
  }

  private ProjectionNode getProjection()
  {
    if (getParent() == null) return null;
    return (ProjectionNode)getParent();
  }

  @Override
  public boolean loadChildren(WbConnection connection)
  {
    ProjectionNode projection = getProjection();
    if (projection != null)
    {
      readBuddies(connection, projection);
    }
    return true;
  }

  private void readBuddies(WbConnection conn, ProjectionNode projection)
  {
    VerticaProjectionReader reader = new VerticaProjectionReader();
    reader.setConnection(conn);
    try
    {
      DataStore buddies = reader.getProjectionCopies(projection.getName());
      for (int row = 0; row < buddies.getRowCount(); row ++)
      {
        String name = buddies.getValueAsString(row, "name");
        String node = buddies.getValueAsString(row, "node");
        add(new BuddyNode(name, node));
      }
      setChildrenLoaded(true);
    }
    catch (Exception ex)
    {
      LogMgr.logError("BuddyListNode.readBuddies()", "Could not read buddies", ex);
    }
  }

}
