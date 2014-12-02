/*
 * SqlServerProcedureReaderTest.java
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
package workbench.db.mssql;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerProcedureReaderTest
	extends WbTestCase
{

	public SqlServerProcedureReaderTest()
	{
		super("SqlServerProcedureReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("SqlServerProcedureReaderTest");
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull("No connection available", conn);
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull("No connection available", conn);
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@Test
	public void testGetProcedures()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		assertNotNull("No connection available", conn);

		String sql =
			"create procedure answer(@value integer output)\n" +
			"as \n" +
			" set @value = 42;\n";

		TestUtil.executeScript(conn, sql);
		ProcedureReader reader = conn.getMetadata().getProcedureReader();
		assertTrue(reader instanceof SqlServerProcedureReader);
		List<ProcedureDefinition> procedureList = reader.getProcedureList(SQLServerTestUtil.DB_NAME, "dbo", null);
		assertNotNull(procedureList);
		assertEquals(1, procedureList.size());
		CharSequence source = procedureList.get(0).getSource(conn);
		assertNotNull(source);
		String sourceSql = source.toString();
		String delimiter = conn.getAlternateDelimiter().getDelimiter();
		assertEquals(SqlUtil.trimSemicolon(sql) + "\n" + delimiter, sourceSql.trim());

		procedureList = reader.getProcedureList(SQLServerTestUtil.DB_NAME, "dbo", "answer");
		assertEquals(1, procedureList.size());
	}
}
