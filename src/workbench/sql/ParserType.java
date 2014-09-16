/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
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
package workbench.sql;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public enum ParserType
{
	Standard,
	Postgres,
	SqlServer;

	public static ParserType getTypeFromConnection(WbConnection conn)
	{
		if (conn == null) return Standard;
		return getTypeFromDBID(conn.getDbId());
	}

	public static ParserType getTypeFromDBID(String dbid)
	{
		if (dbid == null) return Standard;
		if (DbMetadata.DBID_PG.equals(dbid)) return Postgres;
		if (DbMetadata.DBID_MS.equals(dbid)) return SqlServer;
		return Standard;
	}

}
