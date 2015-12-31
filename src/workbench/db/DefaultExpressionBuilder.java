/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db;

/**
 *
 * @author Thomas Kellerer
 */
public class DefaultExpressionBuilder
	implements DmlExpressionBuilder
{
	protected DbSettings settings;

	public DefaultExpressionBuilder(WbConnection conn)
	{
		if (conn != null)
		{
			setDbSettings(conn.getDbSettings());
		}
	}

	/**
	 * Use a different instance of DbSettings
	 * @param dbSettings
	 */
	public final void setDbSettings(DbSettings dbSettings)
	{
		settings = dbSettings;
	}

	@Override
	public String getDmlExpression(ColumnIdentifier column)
	{
		if (settings == null)
		{
			return "?";
		}
		String expression = settings.getDmlExpressionValue(column.getDbmsType());
		if (expression == null)
		{
			return "?";
		}
		return expression;
	}

	@Override
	public boolean isDmlExpressionDefined(String baseType)
	{
		if (settings == null) return false;
		return settings.isDmlExpressionDefined(baseType);
	}

}
