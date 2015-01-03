/*
 * CteAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.completion;

import java.util.ArrayList;
import java.util.List;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class CteAnalyzer
	extends SelectAnalyzer
{
	private CteParser cteParser;

	public CteAnalyzer(WbConnection conn, String baseSql, int realCursorPos)
	{
		this(conn, new CteParser(conn, baseSql), realCursorPos);
	}

	private CteAnalyzer(WbConnection conn, CteParser cte, int realCursorPos)
	{
		super(conn, cte.getBaseSql(), realCursorPos - cte.getBaseSqlStart());
		cteParser = cte;
	}

	@Override
	protected void retrieveTables()
	{
		super.retrieveTables();
		List<CteDefinition> defs = cteParser.getCteDefinitions();
		for (int i=0; i < defs.size(); i++)
		{
			CteDefinition def = defs.get(i);
			String name = def.getName();
			TableIdentifier tbl = new TableIdentifier(name);
			elements.add(i, tbl);
		}
	}

	@Override
	protected boolean retrieveColumns()
	{
		if (tableForColumnList != null)
		{
			String name = tableForColumnList.getTableName();
			for (CteDefinition def : cteParser.getCteDefinitions())
			{
				if (name.equalsIgnoreCase(def.getName()))
				{
					title = name + ".*";
					elements = new ArrayList();
					elements.addAll(def.getColumns());
					return true;
				}
			}
		}
		return super.retrieveColumns();
	}

}
