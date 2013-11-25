/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
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

package workbench.db.postgres;

import java.sql.Statement;

import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.util.SqlUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresErrorPositionReaderTest
	extends WbTestCase
{

	public PostgresErrorPositionReaderTest()
	{
		super("PgErrorPos");
	}

	@Test
	public void testGetErrorPosition()
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		String sql = "SELECT x";
		Exception ex = runStatement(con, sql);
		PostgresErrorPositionReader reader = new PostgresErrorPositionReader();

		int pos = reader.getErrorPosition(con, sql, ex);
		assertEquals(8, pos);

		sql = "selct 42";
		ex = runStatement(con, sql);
		pos = reader.getErrorPosition(con, sql, ex);
		assertEquals(1, pos);
	}

	private Exception runStatement(WbConnection con, String sql)
	{
		Statement stmt = null;
		Exception result = null;
		try
		{
			stmt = con.createStatement();
			stmt.executeQuery(sql);
		}
		catch (Exception ex)
		{
			result = ex;
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
		return result;
	}
}
