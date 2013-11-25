/*
 * ErrorPositionReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import workbench.db.oracle.OracleErrorPositionReader;
import workbench.db.postgres.PostgresErrorPositionReader;

/**
 *
 * @author Thomas Kellerer
 */
public interface ErrorPositionReader
{
	/**
	 * Return the position of the error as reported by the DBMS.
	 *
	 * @param con   the connection on which the error happened
	 *              (may be used by the reader to retrieve additional information)
	 * @param sql   the SQL statement that caused the error
	 * @param ex    the error exception
	 * @return the position of the error inside the SQL or -1 if this could not be determined
	 */
	int getErrorPosition(WbConnection con, String sql, Exception ex);

	/**
	 * Identifies if exceptions from this DBMS contain the error position.
	 *
	 * This can be used to determine if the error message shown to the end user
	 * should be enhanced with information about the error position.
	 *
	 * @return true if the exception contains the error position.
	 */
	String enhanceErrorMessage(String sql, String errorMessage, int errorPosition);

	public class Factory
	{
		private static final ErrorPositionReader dummyReader = new ErrorPositionReader()
		{
			@Override
			public int getErrorPosition(WbConnection con, String sql, Exception ex)
			{
				return -1;
			}

			@Override
			public String enhanceErrorMessage(String sql, String originalMessage, int errorPosition)
			{
				return originalMessage;
			}
		};

		/**
		 * Create an instance of an ErrorPositionReader for the DBMS identified by the connection.
		 *
		 * @param conn  the connection
		 * @return an ErrorPositionReader, never null
		 */
		public static ErrorPositionReader createPositionReader(WbConnection conn)
		{
			if (conn == null) return dummyReader;
			if (conn.getMetadata().isOracle())
			{
				return new OracleErrorPositionReader();
			}
			if (conn.getMetadata().isPostgres())
			{
				return new PostgresErrorPositionReader();
			}
			return dummyReader;
		}
	}

}
