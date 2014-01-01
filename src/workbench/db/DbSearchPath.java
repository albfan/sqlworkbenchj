/*
 * DbSearchPath.java
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
package workbench.db;

import java.util.Collections;
import java.util.List;

import workbench.db.ibm.Db2SearchPath;
import workbench.db.postgres.PostgresUtil;

/**
 *
 * @author Thomas Kellerer
 */
public interface DbSearchPath
{
	List<String> getSearchPath(WbConnection dbConn, String defaultSchema);

	DbSearchPath DEFAULT_HANDLER = new DbSearchPath()
	{
		@Override
		public List<String> getSearchPath(WbConnection dbConn, String defaultSchema)
		{
			if (defaultSchema == null)
			{
				defaultSchema = dbConn.getCurrentSchema();
			}
			if (defaultSchema == null)
			{
				return Collections.emptyList();
			}
			return Collections.singletonList(dbConn.getMetadata().adjustSchemaNameCase(defaultSchema));
		}
	};

	DbSearchPath PG_HANDLER = new DbSearchPath()
	{
		@Override
		public List<String> getSearchPath(WbConnection dbConn, String defaultSchema)
		{
			if (defaultSchema != null && dbConn != null)
			{
				return Collections.singletonList(dbConn.getMetadata().adjustSchemaNameCase(defaultSchema));
			}
			return PostgresUtil.getSearchPath(dbConn);
		}
	};

	class Factory
	{
		public static DbSearchPath getSearchPathHandler(WbConnection con)
		{
			if (con != null && con.getMetadata().isPostgres())
			{
				return PG_HANDLER;
			}
			else if (con.getDbId().equals("db2i"))
			{
				return new Db2SearchPath();
			}
			return DEFAULT_HANDLER;
		}
	}
}
