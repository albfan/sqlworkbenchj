/*
 * WbCallHandler
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.List;
import workbench.sql.StatementRunnerResult;
import workbench.sql.preparedstatement.ParameterDefinition;

/**
 *
 * @author Thomas Kellerer
 */
public interface WbCallHandler
{
	String getExecutedSQL();
	List<ParameterDefinition> prepareStatement(String command);
	StatementRunnerResult execute()
		throws SQLException;
	void done();
}
