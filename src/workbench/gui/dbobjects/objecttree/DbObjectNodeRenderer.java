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
package workbench.gui.dbobjects.objecttree;

import java.awt.Component;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import workbench.resource.IconMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.SynonymReader;

import workbench.util.CaseInsensitiveComparator;



/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectNodeRenderer
	extends DefaultTreeCellRenderer
{
  private Map<String, String> iconMap = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
  private Map<String, String> iconMapOpen = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);

	public DbObjectNodeRenderer()
	{
		super();
    iconMap.put(TreeLoader.TYPE_TABLE, "table");
    iconMap.put(TreeLoader.TYPE_VIEW, "table");
    iconMap.put(SynonymReader.SYN_TYPE_NAME, "synonym");
    iconMap.put("sequence", "sequence");
    iconMap.put(DbMetadata.MVIEW_NAME, "table");
    iconMap.put("database", "profile");
    iconMap.put(TreeLoader.TYPE_SCHEMA, "folder");
    iconMap.put(TreeLoader.TYPE_CATALOG, "folder");
    iconMap.put(TreeLoader.TYPE_DBO_TYPE_NODE, "db_type");
    iconMap.put("index", "index");

    iconMapOpen.put(TreeLoader.TYPE_SCHEMA, "folder-open");
    iconMapOpen.put(TreeLoader.TYPE_CATALOG, "folder-open");
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean isLeaf, int row, boolean hasFocus)
	{
		Component result = super.getTreeCellRendererComponent(tree, value, isSelected, expanded, isLeaf, row, hasFocus);

		if (value instanceof ObjectTreeNode)
		{
			ObjectTreeNode node = (ObjectTreeNode)value;
      String type = node.getType();
      DbObject dbo = node.getDbObject();
      if (dbo instanceof ColumnIdentifier)
      {
        ColumnIdentifier col = (ColumnIdentifier)dbo;
        if (col.isPkColumn())
        {
          setIcon(IconMgr.getInstance().getLabelIcon("key"));
        }
      }
      else
      {
        String key = null;

        if (expanded)
        {
          key = iconMapOpen.get(type);
        }
        if (key == null) key = iconMap.get(type);

        if (key != null)
        {
          setIcon(IconMgr.getInstance().getLabelIcon(key));
        }
      }
      setToolTipText(node.getTooltip());
		}

		return result;
	}
}
