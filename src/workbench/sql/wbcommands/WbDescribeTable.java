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

import workbench.db.TriggerReader;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.ColumnRemover;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 * Display the definition of a table.
 *
 * This command will return multiple result sets:
 *
 * For tables, the following DataStores are returned
 * <ol>
 *		<li>The table definition (columns)</li>
 *		<li>A list of indexes defined for the table</li>
 *    <li>A list of triggers defined for the table</li>
 * </ol>
 *
 * For Views, the view definiton and the view source is returned.
 * 
 * @author  support@sql-workbench.net
 * @see workbench.db.DbMetadata#getTableDefinition(workbench.db.TableIdentifier)
 * @see workbench.db.IndexReader#getTableIndexInformation(workbench.db.TableIdentifier)
 * @see workbench.db.TriggerReader#getTableTriggers(workbench.db.TableIdentifier)
 * @see workbench.db.ViewReader#getExtendedViewSource(workbench.db.TableIdentifier, boolean) 
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
		StatementRunnerResult result = new StatementRunnerResult(sql);
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
		cols.setResultName(toDescribe.getTableName());
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
				index.setResultName(toDescribe.getTableName() +  " - " + ResourceMgr.getString("TxtDbExplorerIndexes"));
				result.addDataStore(index);
			}

			TriggerReader trgReader = new TriggerReader(currentConnection);
			DataStore triggers = trgReader.getTableTriggers(toDescribe);
			if (triggers != null && triggers.getRowCount() > 0)
			{
				triggers.setResultName(toDescribe.getTableName() +  " - " + ResourceMgr.getString("TxtDbExplorerTriggers"));
				result.addDataStore(triggers);
			}
		}

		return result;
	}

}
