/*
 * ResultInfoDisplayBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.util.SqlUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultInfoDisplayBuilderTest
extends WbTestCase
{
	public ResultInfoDisplayBuilderTest()
	{
		super("ResultInfoDisplayBuilderTest");
	}

	@Test
	public void testGetDataStore()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con =  util.getConnection();
		String sql =
			"CREATE TABLE person (id integer primary key, firstname varchar(20), lastname varchar(20));\n" +
			"insert into person (id, firstname, lastname) values (42, 'Zaphod', 'Beeblebrox');\n" +
			"insert into person (id, firstname, lastname) values (1, 'Mary', 'Moviestar');\n"+
			"comment on column person.id is 'Primary Key';\n" +
			"comment on column person.firstname is 'Firstname';\n" +
			"commit;";
		TestUtil.executeScript(con, sql);

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.createStatement();
			String query = "select id as pid, firstname, lastname from person";
			rs = stmt.executeQuery(query);
			DataStore ds = new DataStore(rs, true, 0);
			ds.setGeneratingSql(query);
			ds.setOriginalConnection(con);

			ResultInfo info = ds.getResultInfo();

			DataStore infoDs = ResultInfoDisplayBuilder.getDataStore(info, false, false);

			assertNotNull(infoDs);
			assertEquals(3, infoDs.getRowCount());
			assertEquals(10, infoDs.getColumnCount());
			assertEquals("ID", infoDs.getValueAsString(0, "COLUMN_NAME"));
			assertEquals("PID", infoDs.getValueAsString(0, "ALIAS")); // this only works properly with H2
			assertEquals("INTEGER", infoDs.getValueAsString(0, "DATA_TYPE"));
			assertEquals("FIRSTNAME", infoDs.getValueAsString(1, "COLUMN_NAME"));

			ResultColumnMetaData meta = new ResultColumnMetaData(ds);
			meta.retrieveColumnRemarks(ds.getResultInfo(), null);

			infoDs = ResultInfoDisplayBuilder.getDataStore(info, true, false);

			assertEquals(3, infoDs.getRowCount());
			assertEquals(12, infoDs.getColumnCount());
			assertEquals("ID", infoDs.getValueAsString(0, "COLUMN_NAME"));
			assertEquals("PID", infoDs.getValueAsString(0, "ALIAS"));
			assertEquals("INTEGER", infoDs.getValueAsString(0, "DATA_TYPE"));

			assertEquals("FIRSTNAME", infoDs.getValueAsString(1, "COLUMN_NAME"));
			assertEquals("LASTNAME", infoDs.getValueAsString(2, "COLUMN_NAME"));

			assertEquals("Primary Key", infoDs.getValueAsString(0, "REMARKS"));
			assertEquals("Firstname", infoDs.getValueAsString(1, "REMARKS"));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

}