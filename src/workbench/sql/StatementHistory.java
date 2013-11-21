/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
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

package workbench.sql;

import java.util.List;

import workbench.interfaces.SqlHistoryProvider;

import workbench.sql.wbcommands.WbHistory;

import workbench.util.FixedSizeList;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class StatementHistory
	extends FixedSizeList<String>
	implements SqlHistoryProvider
{

	public StatementHistory(int max)
	{
		super(max);
		setAllowDuplicates(true);
		doAppend(true);
	}

	@Override
	public synchronized boolean add(String statement)
	{
		String verb = SqlUtil.getSqlVerb(statement);
		if (!verb.equalsIgnoreCase(WbHistory.VERB))
		{
			return super.add(statement);
		}
		return false;
	}

	@Override
	public List<String> getHistoryEntries()
	{
		return this.getEntries();
	}

	@Override
	public String getHistoryEntry(int index)
	{
		return this.get(index);
	}

}
