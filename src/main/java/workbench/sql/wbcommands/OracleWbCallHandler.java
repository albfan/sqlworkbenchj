/*
 * OracleWbCallHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
