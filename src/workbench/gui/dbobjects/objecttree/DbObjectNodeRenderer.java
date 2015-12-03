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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import workbench.resource.IconMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ProcedureDefinition;
import workbench.db.SynonymReader;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;



/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectNodeRenderer
	extends DefaultTreeCellRenderer
{
  private Map<String, String> iconMap = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
  private Map<String, String> iconMapOpen = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
  private List<String> tableTypes = new ArrayList<>();
  private List<String> viewTypes = new ArrayList<>();

	public DbObjectNodeRenderer()
	{
		super();
    iconMap.put(TreeLoader.TYPE_TABLE, "table");
    iconMap.put("system table", "table");
    iconMap.put(TreeLoader.TYPE_VIEW, "view");
    iconMap.put("system view", "view");
    iconMap.put(SynonymReader.SYN_TYPE_NAME, "synonym");
    iconMap.put("sequence", "sequence");
    iconMap.put(DbMetadata.MVIEW_NAME, "mview");
    iconMap.put("database", "profile");
    iconMap.put(TreeLoader.TYPE_SCHEMA, "folder");
    iconMap.put(TreeLoader.TYPE_CATALOG, "folder-db");
    iconMap.put(TreeLoader.TYPE_DBO_TYPE_NODE, "db_type");
    iconMap.put(TreeLoader.TYPE_PROCEDURES_NODE, "db_type");
    iconMap.put(TreeLoader.TYPE_TRIGGERS_NODE, "db_type");
    iconMap.put(TreeLoader.TYPE_DEPENDENCY_USED, "deps-used");
    iconMap.put(TreeLoader.TYPE_DEPENDENCY_USING, "deps-using");
    iconMap.put("procedure", "sproc");
    iconMap.put("trigger", "bullet_black");
    iconMap.put("index", "index");
    iconMap.put("type", "type");
    iconMap.put(TreeLoader.TYPE_PACKAGE_NODE, "package");

    iconMapOpen.put(TreeLoader.TYPE_SCHEMA, "folder-open");
    iconMapOpen.put(TreeLoader.TYPE_CATALOG, "folder-open-db");
    setLeafIcon(IconMgr.getInstance().getLabelIcon("bullet_black"));
	}

  public void setViewTypes(List<String> types)
  {
    if (CollectionUtil.isEmpty(types)) return;
    for (String type : viewTypes)
    {
      iconMap.remove(type);
    }

    viewTypes = new ArrayList<>(types);
    for (String type : types)
    {
      iconMap.put(type, "view");
    }
    // just to be sure
    iconMap.put(TreeLoader.TYPE_VIEW, "view");
  }

  public void setTableTypes(List<String> types)
  {
    if (CollectionUtil.isEmpty(types)) return;
    for (String type : tableTypes)
    {
      iconMap.remove(type);
    }
    tableTypes = new ArrayList<>(types);
    for (String type : types)
    {
      iconMap.put(type, "table");
    }
    // just to be sure
    iconMap.put(TreeLoader.TYPE_TABLE, "table");
  }

  public void setSynonymTypeName(String name)
  {
    iconMap.put(name, "synonym");
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
        else
        {
          setIcon(IconMgr.getInstance().getLabelIcon("bullet_black"));
        }
      }
      else if (dbo instanceof ProcedureDefinition)
      {
        ProcedureDefinition def = (ProcedureDefinition)dbo;
        if (def.isFunction())
        {
          setIcon(IconMgr.getInstance().getLabelIcon("sfunc"));
        }
        else
        {
          setIcon(IconMgr.getInstance().getLabelIcon("sproc"));
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
