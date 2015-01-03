/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

package workbench.db.oracle;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.ErrorDescriptor;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleErrorPositionReaderTest
	extends WbTestCase
{

	public OracleErrorPositionReaderTest()
	{
		super("GetErrorTest");
	}

	@Test
	public void testGetErrorPosition()
		throws Exception
	{
		WbConnection conn = OracleTestUtil.getOracleConnection();
		assertNotNull(conn);

		OracleErrorPositionReader reader = new OracleErrorPositionReader();
		ErrorDescriptor error = reader.getErrorPosition(conn, "select 42 from dualx", null);
		assertNotNull(error);
		assertEquals(15, error.getErrorPosition());

		error = reader.getErrorPosition(conn, "select 42 from dual", null);
		assertNull(error);
	}

	@Test
	public void testCreate()
		throws Exception
	{
		WbConnection conn = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", conn);

		OracleErrorPositionReader reader = new OracleErrorPositionReader(true, true);
		ErrorDescriptor error = reader.getErrorPosition(conn, "create table foo (id integr)", null);
		assertNotNull(error);
		assertEquals(21, error.getErrorPosition());

		error = reader.getErrorPosition(conn, "drop tablex foo", null);
		assertNotNull(error);
		assertEquals(5, error.getErrorPosition());

		error = reader.getErrorPosition(conn, "drop table foo cascade", null);
		assertNotNull(error);
		assertEquals(22, error.getErrorPosition());

		error = reader.getErrorPosition(conn, "drop function foo()", null);
		assertNotNull(error);
		assertEquals(17, error.getErrorPosition());
	}

	@Test
	public void testPLSqlBlock()
		throws Exception
	{
		Locale currentLocale = Locale.getDefault();
		try
		{
			// make sure we use english error messages
			Locale.setDefault(Locale.ENGLISH);
			WbConnection conn = OracleTestUtil.getOracleConnection();
			assertNotNull("Oracle not available", conn);

			OracleErrorPositionReader reader = new OracleErrorPositionReader();
			String sql =
				"declare\n" +
				"  cursor c1 is \n" +
				"      select x from dual;\n" +
				"begin\n" +
				"   null;\n" +
				"end;\n";
			Statement stmt = null;
			SQLException error = null;
			try
			{
				stmt = conn.createStatement();
				stmt.execute(sql);
			}
			catch (SQLException ex)
			{
//				System.out.println("*** " + ex.getMessage());
				error = ex;
			}
			ErrorDescriptor errorInfo = reader.getErrorPosition(conn, sql, error);
			assertNotNull(error);
			assertNotNull(errorInfo);
			assertEquals(2, errorInfo.getErrorLine());
			assertEquals(13, errorInfo.getErrorColumn());
		}
		finally
		{
			Locale.setDefault(currentLocale);
		}
	}

}
