/*
 * DependencyTreeDumper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
import java.util.Map;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DependencyTreeDumper
{
	public static void dumpTree(TableIdentifier rootTable, Map<Integer, Set<DependencyNode>> levels, String fname)
	{
		FileWriter writer = null;
		try
		{
			writer = new FileWriter(new File(fname));
			writer.append(rootTable.getTableExpression() + "\n");

			for (Map.Entry<Integer, Set<DependencyNode>> entry : levels.entrySet())
			{
				int level = entry.getKey();
				for (DependencyNode node : entry.getValue())
				{
					writer.append(StringUtil.padRight("", level*2));
					writer.append(node.getTable() + " (" + node.getFkName() + ")\n");
				}
			}
		}
		catch (Exception ex)
		{
			LogMgr.logDebug("dumpTree()", "error writing tree", ex);
		}
		finally
		{
			FileUtil.closeQuietely(writer);
		}
	}


}
