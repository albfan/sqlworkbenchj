/*
 * WbProcSource.java
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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;

import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * Display the source code for a procedure.
 *
 * @see workbench.db.ProcedureDefinition#getSource(workbench.db.WbConnection)
 * @author Thomas Kellerer
 */
public class WbProcSource
	extends SqlCommand
{
	public static final String VERB = "WBPROCSOURCE";

	public WbProcSource()
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

		TableIdentifier object = new TableIdentifier(args);
		object.adjustCase(currentConnection);

		ProcedureReader reader = currentConnection.getMetadata().getProcedureReader();
		ProcedureDefinition def = new ProcedureDefinition(object.getCatalog(), object.getSchema(), object.getObjectName(), DatabaseMetaData.procedureResultUnknown);
		if (reader.procedureExists(def))
		{
			CharSequence source = def.getSource(currentConnection);
			result.addMessage(source);
			result.setSuccess();
		}
		else
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrProcNotFound", args));
			result.setFailure();
		}
		
		return result;
	}

}
