/*
 * PostgresViewReader
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.SQLException;
import workbench.db.DefaultViewReader;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.resource.Settings;

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
