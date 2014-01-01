/*
 * HsqlConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.hsqldb;

import workbench.db.AbstractConstraintReader;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;
import workbench.util.StringUtil;

/**
 * Constraint reader for HSQLDB
 * @author  Thomas Kellerer
 */
public class HsqlConstraintReader
	extends AbstractConstraintReader
{
	private String sql;

	public HsqlConstraintReader(WbConnection dbConnection)
	{
		super(dbConnection.getDbId());
		this.sql = "select chk.constraint_name, chk.check_clause \n" +
			"from information_schema.system_check_constraints chk, information_schema.system_table_constraints cons \n" +
			"where chk.constraint_name = cons.constraint_name  \n" +
			"and cons.constraint_type = 'CHECK' \n" +
			"and cons.table_name = ?; \n";

		if (JdbcUtils.hasMinimumServerVersion(dbConnection, "1.9"))
		{
			this.sql = sql.replace("system_check_constraints", "check_constraints");
			this.sql = sql.replace("system_table_constraints", "table_constraints");
		}
	}

	@Override
	public String getColumnConstraintSql()
	{
		return null;
	}

	@Override
	public String getTableConstraintSql()
	{
		return this.sql;
	}

	@Override
	public boolean isSystemConstraintName(String name)
	{
		if (StringUtil.isBlank(name))	return false;
		return name.trim().startsWith("SYS_");
	}
}
