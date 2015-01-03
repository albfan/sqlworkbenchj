/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
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

import java.sql.SQLException;

import workbench.db.firebird.FirebirdSequenceAdjuster;
import workbench.db.h2database.H2SequenceAdjuster;
import workbench.db.hsqldb.HsqlSequenceAdjuster;
import workbench.db.ibm.Db2SequenceAdjuster;
import workbench.db.postgres.PostgresSequenceAdjuster;

/**
 *
 * @author Thomas Kellerer
 */
public interface SequenceAdjuster
{
	int adjustTableSequences(WbConnection connection, TableIdentifier table, boolean includeCommit)
		throws SQLException;

	public static class Factory
	{
		public static SequenceAdjuster getSequenceAdjuster(WbConnection conn)
		{
			if (conn == null) return null;
			if (conn.getMetadata().isPostgres())
			{
				return new PostgresSequenceAdjuster();
			}
			if (conn.getMetadata().isH2())
			{
				return new H2SequenceAdjuster();
			}
			if (conn.getMetadata().isHsql() && JdbcUtils.hasMinimumServerVersion(conn, "2.0"))
			{
				return new HsqlSequenceAdjuster();
			}
			if (conn.getDbId().equals("db2"))
			{
				return new Db2SequenceAdjuster();
			}
			if (conn.getMetadata().isFirebird() && JdbcUtils.hasMinimumServerVersion(conn, "3.0"))
			{
				return new FirebirdSequenceAdjuster();
			}
			return null;
		}
	}


}
