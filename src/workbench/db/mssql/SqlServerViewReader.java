/*
 * SqlServerViewReader.java
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
package workbench.db.mssql;

import workbench.db.DefaultViewReader;
import workbench.db.NoConfigException;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerViewReader
	extends DefaultViewReader
{

	public SqlServerViewReader(WbConnection con)
	{
		super(con);
	}

	@Override
	public CharSequence getViewSource(TableIdentifier viewId)
		throws NoConfigException
	{
		SpHelpTextRunner runner = new SpHelpTextRunner();
		CharSequence sql = runner.getSource(connection, viewId.getRawCatalog(), viewId.getRawSchema(), viewId.getRawTableName());
		if (!StringUtil.endsWith(sql, ';'))
		{
			StringBuilder full = new StringBuilder(sql.length() + 1);
			full.append(sql);
			full.append(';');
			return full;
		}
		return sql;
	}

}
