/*
 * SelectFKValueMarker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectFKValueMarker
{
	private final String columnName;
	private final TableIdentifier table;
	private final boolean allowMultiSelect;
	
	public SelectFKValueMarker(String column, TableIdentifier baseTable, boolean multiSelectAllowed)
	{
		this.columnName = column;
		this.table = baseTable;
		this.allowMultiSelect = multiSelectAllowed;
	}

	public boolean getAllowMultiSelect()
	{
		return allowMultiSelect;
	}

	public String getColumnName()
	{
		return columnName;
	}

	public TableIdentifier getTable()
	{
		return table;
	}

	@Override
	public String toString()
	{
		return "(" + ResourceMgr.getString("MnuTxtSelectFkValue") + ")";
	}

}
