/*
 * WbTriggerSource.java
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
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;

import workbench.db.TriggerReader;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * Display the source code of a trigger.
 * @see workbench.db.TriggerReader#getTriggerSource(java.lang.String, java.lang.String, java.lang.String, workbench.db.TableIdentifier)
 *
 * @author Thomas Kellerer
 */
public class WbTriggerSource
	extends SqlCommand
{

	public static final String VERB = "WBTRIGGERSOURCE";
	public static final String FORMATTED_VERB = "WbTriggerSource";

	public WbTriggerSource()
	{
		super();
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(sql);

		DbObject object = new TableIdentifier(args);

		TriggerReader reader = new TriggerReader(currentConnection);
		TriggerDefinition trg = reader.findTrigger(object.getCatalog(), object.getSchema(), object.getObjectName());
		String source = null;
		if (trg != null)
		{
			source = reader.getTriggerSource(trg);
		}

		if (source != null)
		{
			result.addMessage(source);
			result.setSuccess();
		}
		else
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrTrgNotFound", object.getObjectExpression(currentConnection)));
			result.setFailure();
		}

		return result;
	}
}
