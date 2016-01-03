/*
 * DependencyDuplicateFinder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.util.FileUtil;
import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
class DependencyDuplicateFinder
{
	private final DependencyNode root;
	private final Map<TableIdentifier, Integer> tableLevels = new HashMap<>();

	DependencyDuplicateFinder(DependencyNode rootNode)
	{
		this.root = rootNode;
	}

	Set<String> getDuplicates()
	{
		Set<String> result = new HashSet<>();
		List<NodeInformation> tree = buildTree(root, 0);
		for (NodeInformation info : tree)
		{
			if (info.level > getHighestLevel(info.node.getTable()))
			{
				String path = getNodePath(info.node);
				if (LogMgr.isTraceEnabled())
				{
					LogMgr.logTrace("DependencyDuplicateFinder.getDuplicates()", "Node " + path + " is redundant");
				}
				result.add(path);
			}
		}
		return result;
	}

	static String getNodePath(DependencyNode node)
	{
		StringBuilder path = new StringBuilder(10);
		path.append('/');
		path.append(node.getTable().getTableExpression());
		DependencyNode parent = node.getParent();
		while (parent != null)
		{
			path.insert(0, "/" + parent.getTable().getTableExpression());
			parent = parent.getParent();
		}
		return path.toString();
	}

	void dumpTree(List<NodeInformation> tree, String fname)
	{
		FileWriter writer = null;
		try
		{
			writer = new FileWriter(new File("c:/temp", fname));
			writer.append(this.root.getTable().toString() + "\n");
			for (NodeInformation infoNode : tree)
			{
				String indent = StringUtil.padRight("", (infoNode.level + 1) * 4);
				writer.append(indent);
				writer.append(infoNode.node.getTable().toString());
				String cols = StringUtil.listToString(infoNode.node.getColumns().keySet(), ',');
				writer.append(" (").append(cols).append(')');
				writer.append('\n');
			}
		}
		catch (IOException io)
		{
			//ignore
		}
		finally
		{
			FileUtil.closeQuietely(writer);
		}
	}

	private int getHighestLevel(TableIdentifier table)
	{
		Integer lvl = tableLevels.get(table);
		if (lvl == null) return 0;
		return lvl.intValue();
	}

	List<NodeInformation> buildTree(DependencyNode root, int level)
	{
		List<NodeInformation> result = new ArrayList<>();
		List<DependencyNode> children = root.getChildren();
		if (children.isEmpty()) return result;

		for (DependencyNode child : children)
		{
			NodeInformation info = new NodeInformation();
			info.node = child;
			info.level = level;
			if (!tableLevels.containsKey(child.getTable()))
			{
				tableLevels.put(child.getTable(), Integer.valueOf(level));
			}
			result.add(info);
			result.addAll(buildTree(child, level + 1));
		}
		return result;
	}

	class NodeInformation
	{
		DependencyNode node;
		int level;
	}

}
