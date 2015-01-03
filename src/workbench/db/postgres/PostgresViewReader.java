/*
 * PostgresViewReader.java
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
package workbench.db.postgres;

import java.sql.SQLException;

import workbench.resource.Settings;

import workbench.db.DefaultViewReader;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresViewReader
	extends DefaultViewReader
{

	public PostgresViewReader(WbConnection con)
	{
		super(con);
	}

	@Override
	public CharSequence getExtendedViewSource(TableDefinition view, boolean includeDrop, boolean includeCommit)
		throws SQLException
	{
		CharSequence source = super.getExtendedViewSource(view, includeDrop, false);
		PostgresRuleReader ruleReader = new PostgresRuleReader();

		CharSequence rules = ruleReader.getTableRuleSource(this.connection, view.getTable());
		StringBuilder result = new StringBuilder(source.length() + (rules == null ? 0 : rules.length()));
		result.append(source);
		if (rules != null)
		{
			result.append("\n\n");
			result.append(rules);
		}

		if (includeCommit)
		{
			result.append("COMMIT;");
			result.append(Settings.getInstance().getInternalEditorLineEnding());
		}
		return result;
	}
}
