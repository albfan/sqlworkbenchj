/*
 * WbDescribeTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;

import workbench.db.TriggerReader;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;

/**
 * Display the definition of a database object
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
public class WbDescribeObject
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
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);
		
		String table = SqlUtil.stripVerb(SqlUtil.makeCleanSql(sql, false, false));
		ObjectInfo info = new ObjectInfo();
		StatementRunnerResult result = info.getObjectInfo(currentConnection, table, true);
		result.setSourceCommand(sql);
		result.setShowRowCount(false);
		return result;
	}

}
