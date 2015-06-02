/*
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
 */ package workbench.db.mssql;

import java.io.BufferedReader;
import java.io.File;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.BatchRunner;

import workbench.util.EncodingUtil;
import workbench.util.FileUtil;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbExportSqlServerTest
	extends WbTestCase
{

	public WbExportSqlServerTest()
	{
		super("WbExportSqlServerTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("export_test");
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull("No connection available", con);

		TestUtil.executeScript(con,
			"create table data (some_data binary(4), data_id integer not null);\n" +
			"insert into data (data_id, some_data) values (1, 0x01020304);\n" +
			"insert into data (data_id, some_data) values (2, 0x02030405);\n" +
			"commit;\n"
			);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull("No connection available", conn);
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@Test
	public void testBlobExport()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		assertNotNull("No connection available", conn);

		TestUtil util = getTestUtil();

		String dir = util.getBaseDir();
		File output = new File(dir, "data.txt");
		String sql =
			"WbExport -type=text\n" +
			"         -file='" + output.getAbsolutePath() + "'\n" +
			"         -delimiter=';'\n" +
			"         -encoding='ISO-8859-1'\n" +
			"         -header=true\n" +
			"         -blobType=dbms\n" +
			"         -dateFormat='yyyy-MM-dd';" +
			" select some_data, data_id from data order by data_id";


		BatchRunner runner = new BatchRunner();
		runner.setConnection(conn);
		runner.runScript(sql);

		String msg = runner.getMessages();
		assertTrue(msg, runner.isSuccess());
		assertTrue(output.exists());
		BufferedReader in = null;
		try
		{
			in = EncodingUtil.createBufferedReader(output, "ISO-8859-1");
			List<String> lines = FileUtil.getLines(in);
			assertNotNull(lines);
			assertEquals(3, lines.size());
			assertEquals("0x01020304;1", lines.get(1));
			assertEquals("0x02030405;2", lines.get(2));
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
		assertTrue(output.delete());
	}
}
