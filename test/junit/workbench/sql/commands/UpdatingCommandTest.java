/*
 * UpdatingCommandTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.sql.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.RowDataReader;
import workbench.storage.RowDataReaderFactory;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;

import workbench.util.EncodingUtil;
import workbench.util.SqlUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class UpdatingCommandTest
	extends WbTestCase
{
	private TestUtil util;
	private WbConnection connection;
	private StatementRunner runner;

	public UpdatingCommandTest()
	{
		super("UpdatingCommandTest");
		util = getTestUtil();
	}

	@Before
	public void setUp()
		throws Exception
	{
		util.emptyBaseDirectory();
		runner = util.createConnectedStatementRunner();
		connection = runner.getConnection();
	}

	@After
	public void tearDown()
		throws Exception
	{
		connection.disconnect();
	}

	@Test
	public void testInsertBlob()
	{
		try
		{
			Statement stmt = this.connection.createStatement();
			stmt.executeUpdate("CREATE MEMORY TABLE blob_test(nr integer, blob_data BINARY)");
			stmt.close();

			final byte[] blobData = new byte[] { 1,2,3,4,5,6 };
			File blobFile = new File(util.getBaseDir(), "blob_data.data");
			OutputStream out = new FileOutputStream(blobFile);
			out.write(blobData);
			out.close();

			String sql = "-- read blob from file\ninsert into blob_test(nr, blob_data)\nvalues\n(1,{$blobfile='" + blobFile.getName() + "'})";
			runner.runStatement(sql);
			StatementRunnerResult result = runner.getResult();
			if (!result.isSuccess()) System.out.println(result.getMessages().toString());
			assertEquals("Insert not executed", true, result.isSuccess());

			stmt = this.connection.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, blob_data from blob_test");
			ResultInfo info = new ResultInfo(rs.getMetaData(), this.connection);
			RowDataReader reader = RowDataReaderFactory.createReader(info, connection);
			if (rs.next())
			{

				RowData data = reader.read(rs, false);

				Object value = data.getValue(0);
				int nr = ((Integer)value).intValue();
				assertEquals("Wrong id inserted", 1, nr);

				value = data.getValue(1);
				assertTrue(value instanceof byte[]);

				byte[] blob = (byte[])value;
				assertEquals("Wrong blob size retrieved", blobData.length, blob.length);

				for (int i = 0; i < blob.length; i++)
				{
					assertEquals("Wrong blob contents", blobData[i], blob[i]);
				}
			}
			else
			{
				fail("No data in table");
			}
			SqlUtil.closeAll(rs, stmt);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testInsertClob()
	{
		try
		{
			Statement stmt = this.connection.createStatement();
			stmt.executeUpdate("CREATE MEMORY TABLE clob_test(nr integer, clob_data LONGVARCHAR)");
			stmt.close();

			final String clobData = "Clob data to be inserted";
			File clobFile = new File(util.getBaseDir(), "clob_data.data");
			Writer w = EncodingUtil.createWriter(clobFile, "UTF8", false);
			w.write(clobData);
			w.close();

			String sql = "-- read clob from file\ninsert into clob_test(nr, clob_data)\nvalues\n(1,{$clobfile='" + clobFile.getName() + "' encoding='UTF-8'})";
			runner.runStatement(sql);
			StatementRunnerResult result = runner.getResult();
			if (!result.isSuccess()) System.out.println(result.getMessages().toString());
			assertEquals("Insert not executed", true, result.isSuccess());
			assertEquals(1, result.getTotalUpdateCount());

			stmt = this.connection.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, clob_data from clob_test");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong id inserted", 1, nr);

				String value = rs.getString(2);
				assertEquals("Wrong clob inserted", clobData, value);
			}
			else
			{
				fail("No data in table");
			}
			SqlUtil.closeAll(rs, stmt);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testUpdate()
	{
		try
		{
			Statement stmt = this.connection.createStatement();
			stmt.executeUpdate("CREATE MEMORY TABLE update_test(nr integer primary key, some_data VARCHAR(10))");
			stmt.executeUpdate("insert into update_test (nr, some_data) values (1, 'one')");
			stmt.executeUpdate("insert into update_test (nr, some_data) values (2, 'two')");
			stmt.executeUpdate("insert into update_test (nr, some_data) values (3, 'three')");
			stmt.close();

			String sql = "-- udpate one row\nupdate update_test set some_data = 'THREE' where nr = 3";
			runner.runStatement(sql);
			StatementRunnerResult result = runner.getResult();
			if (!result.isSuccess()) System.out.println(result.getMessages().toString());
			assertEquals("Update not executed", true, result.isSuccess());
			assertEquals(1, result.getTotalUpdateCount());

			stmt = this.connection.createStatement();
			ResultSet rs = stmt.executeQuery("select some_data from update_test where nr = 3");
			if (rs.next())
			{
				String value = rs.getString(1);
				assertEquals("Wrong value updated", "THREE", value);
			}
			else
			{
				fail("No data in table");
			}
			rs.close();
			rs = stmt.executeQuery("select count(*) from update_test where nr = 3");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Wrong row count", 1, count);
			}
			else
			{
				fail("No data in table");
			}
			SqlUtil.closeAll(rs, stmt);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
