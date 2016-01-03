/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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
package workbench.gui.dbobjects.objecttree.vertica;

import workbench.gui.dbobjects.objecttree.ObjectTreeNode;
import workbench.gui.dbobjects.objecttree.TreeLoader;

/**
 *
 * @author Thomas Kellerer
 */
public class ProjectionColumn
  extends ObjectTreeNode
{
  private final String dataType;

  public ProjectionColumn(String colName, String columnType)
  {
    super(colName, TreeLoader.TYPE_PROJECTION_COL_NODE);
    setAllowsChildren(false);
    dataType = columnType;
  }

  @Override
  public String toString()
  {
    return "<html>" + getName() + " - <tt>" + dataType + "</tt></html>";
  }

}
