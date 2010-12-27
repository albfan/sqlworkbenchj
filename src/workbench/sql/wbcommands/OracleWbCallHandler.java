/*
 * OracleWbCallHandler
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.List;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.sql.preparedstatement.ParameterDefinition;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleWbCallHandler
	extends AbstractWbCallHandler
{

	public OracleWbCallHandler(WbConnection dbConn)
	{
		super(dbConn);
	}

	@Override
	public String getExecutedSQL()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public List<ParameterDefinition> prepareStatement(String command)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public StatementRunnerResult execute()
		throws SQLException
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
