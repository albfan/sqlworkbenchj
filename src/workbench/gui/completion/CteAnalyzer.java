/*
 * CteAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
		this(conn, new CteParser(baseSql), realCursorPos);
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
