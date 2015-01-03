/*
 * FirebirdProcedureReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.firebird;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.DelimiterDefinition;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdProcedureReaderTest
	extends WbTestCase
{

	public FirebirdProcedureReaderTest()
	{
		super("FirebirdProcedureReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		FirebirdTestUtil.initTestCase();
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;

		con.setAutoCommit(true);
		String sql =
			"CREATE PROCEDURE answer(a integer) \n" +
			"RETURNS (the_answer   integer)  \n" +
			"AS\n" +
			"BEGIN \n" +
			"  the_answer = 42; \n" +
			"END;  \n" +
			"/\n";

		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		con.setAutoCommit(false);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		FirebirdTestUtil.cleanUpTestCase();
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;

		con.setAutoCommit(true);
		TestUtil.executeScript(con, "drop procedure answer;");
	}

	@Test
	public void testGetProcedureHeader()
		throws Exception
	{
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		assertNotNull("No connection available", con);

		ProcedureReader reader = con.getMetadata().getProcedureReader();
		assertTrue(reader instanceof FirebirdProcedureReader);

		List<ProcedureDefinition> procs = reader.getProcedureList(null, null, null);
		assertEquals(1, procs.size());
		ProcedureDefinition proc = procs.get(0);
		assertEquals("ANSWER", proc.getProcedureName());
		DataStore cols = reader.getProcedureColumns(proc);

		assertEquals(2, cols.getRowCount()); // one input parameter, one output parameter

		String colName = cols.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
		assertEquals("THE_ANSWER", colName);
		String type = cols.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
		assertEquals("OUT", type);

		colName = cols.getValueAsString(1, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
		assertEquals("A", colName);
		type = cols.getValueAsString(1, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
		assertEquals("IN", type);

		String sql = proc.getSource(con).toString();
		System.out.println(sql);
		assertTrue(sql.startsWith("CREATE PROCEDURE ANSWER (A INTEGER)"));
		assertTrue(sql.contains("THE_ANSWER INTEGER"));
		assertTrue(sql.contains("the_answer = 42;"));
	}
}
