/*
 * WbDescribeTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;

import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.ColumnRemover;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbDescribeTable
	extends SqlCommand
{
	public static final String VERB = "DESC";
	public static final String VERB_LONG = "DESCRIBE";

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public String getAlternateVerb()
	{
		return VERB_LONG;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String table = SqlUtil.stripVerb(SqlUtil.makeCleanSql(sql, false, false));

		TableIdentifier tbl = new TableIdentifier(table);

		TableIdentifier toDescribe = this.currentConnection.getMetadata().findSelectableObject(tbl);

		if (toDescribe == null)
		{
			result.setFailure();
			String msg = ResourceMgr.getString("ErrTableOrViewNotFound");
			msg = msg.replace("%name%", table);
			result.addMessage(msg);
			return result;
		}

		DataStore ds = currentConnection.getMetadata().getTableDefinition(toDescribe);

		DbSettings dbs = currentConnection.getDbSettings();

		if (dbs.isSynonymType(toDescribe.getType()))
		{
			TableIdentifier target = currentConnection.getMetadata().getSynonymTable(toDescribe);
			if (target != null)
			{
				result.addMessage(toDescribe.getTableExpression(currentConnection) + " --> " + target.getTableExpression(currentConnection));
			}
		}

		CharSequence viewSource = null;
		if (dbs.isViewType(toDescribe.getType()))
		{
			viewSource = currentConnection.getMetadata().getViewReader().getExtendedViewSource(toDescribe, ds, false, false);
		}

		ColumnRemover remover = new ColumnRemover(ds);
		DataStore cols = remover.removeColumnsByName("java.sql.Types", "SCALE/SIZE", "PRECISION");
		result.setSuccess();
		result.addDataStore(cols);

		if (viewSource != null)
		{
			result.addMessage("------------------ " + toDescribe.getType() + " SQL ------------------");
			result.addMessage(viewSource);
		}
		else if (toDescribe.getType().indexOf("TABLE") > -1)
		{
			DataStore index = currentConnection.getMetadata().getIndexReader().getTableIndexInformation(toDescribe);
			if (index.getRowCount() > 0)
			{
				result.addDataStore(index);
			}
		}

		return result;
	}

}
