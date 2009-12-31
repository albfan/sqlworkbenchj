/*
 * WbDescribeObject.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.SqlUtil;

/**
 * Display the definition of a database object
 * <br>
 * This command will return multiple result sets:
 * <br>
 * For tables, the following DataStores are returned
 * <ol>
 *		<li>The table definition (columns)</li>
 *		<li>A list of indexes defined for the table</li>
 *    <li>A list of triggers defined for the table</li>
 * </ol>
 *
 * For Views, the view definiton and the view source is returned.
 * 
 * @author  Thomas Kellerer
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
	public static final String ARG_DEPEND = "dependencies";
	public static final String ARG_OBJECT = "object";
	
	public WbDescribeObject()
	{
		super();
		this.isUpdatingCommand = true;
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_DEPEND, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_OBJECT);
	}

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

		cmdLine.parse(getCommandLine(sql));
		String object = null;
		boolean includeDependencies = true;

		if (cmdLine.hasArguments())
		{
			object = cmdLine.getValue(ARG_OBJECT);
			includeDependencies = cmdLine.getBoolean(ARG_DEPEND, true);
			if (object == null)
			{
				object = cmdLine.getUnknownArguments();
				if (object.startsWith("-"))
				{
					object = object.substring(1);
				}
			}
		}
		else
		{
			object = SqlUtil.stripVerb(SqlUtil.makeCleanSql(sql, false, false));
		}
		ObjectInfo info = new ObjectInfo();
		StatementRunnerResult result = info.getObjectInfo(currentConnection, object, includeDependencies);
		result.setSourceCommand(sql);
		result.setShowRowCount(false);
		return result;
	}

}
