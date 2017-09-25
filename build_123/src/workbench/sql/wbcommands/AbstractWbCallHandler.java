/*
 * AbstractWbCallHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import java.sql.CallableStatement;
import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.sql.preparedstatement.ParameterDefinition;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public abstract class AbstractWbCallHandler
	implements WbCallHandler
{
	protected WbConnection connection;

	public AbstractWbCallHandler(WbConnection dbConn)
	{
		this.connection = dbConn;
	}

	@Override
	public abstract String getExecutedSQL();

	@Override
	public abstract List<ParameterDefinition> prepareStatement(String command);

	@Override
	public abstract StatementRunnerResult execute()
		throws SQLException;

	@Override
	public void done()
	{
	}

	/**
	 * Gets the parameter metadata from the statement, and returns the defined parameters.
	 * <br/>
	 * For each OUT or IN/OUT parameters, cstm.registerOutParameter() will be called.
	 *
	 * @param cstmt the statement that should be executed
	 * @return  parameters will contain all parameters defined for the function call. Must not be null.
	 * @throws SQLException
	 */
	protected List<ParameterDefinition> checkParametersFromStatement(CallableStatement cstmt)
		throws SQLException
	{
		List<ParameterDefinition> parameters = CollectionUtil.arrayList();

		ParameterMetaData parmData = cstmt.getParameterMetaData();
		if (parmData != null)
		{
			for (int i = 0; i < parmData.getParameterCount(); i++)
			{
				int mode = parmData.getParameterMode(i + 1);
				int type = parmData.getParameterType(i + 1);

				ParameterDefinition def = new ParameterDefinition(i + 1, type);
				def.setParameterMode(mode);
				if (mode == ParameterMetaData.parameterModeOut ||
						mode == ParameterMetaData.parameterModeInOut)
				{
					cstmt.registerOutParameter(i + 1, type);
				}
			}
		}
		return parameters;
	}

	/**
	 * Returns all OUT our IN/OUT parameters from the passed List.
	 *
	 * @param parameters
	 * @see ParameterDefinition#isOutParameter()
	 */
	protected List<ParameterDefinition> getOutParameters(List<ParameterDefinition> parameters)
	{
		List<ParameterDefinition> result = new ArrayList<>();
		for (ParameterDefinition def : parameters)
		{
			if (def.isOutParameter())
			{
				result.add(def);
			}
		}
		return result;
	}

}
