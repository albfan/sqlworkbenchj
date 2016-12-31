/*
 * ResultColumnMetaDataTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.storage;

import java.sql.ResultSet;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultColumnMetaDataTest
	extends WbTestCase
{

	public ResultColumnMetaDataTest()
	{
		super("ResultColumnMetaDataTest");
	}

	@Test
	public void testRetrieveColumnRemarks()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		try
		{
			TestUtil.executeScript(con, "CREATE TABLE PERSON (id integer primary key, first_name varchar(50), last_name varchar(50));\n" +
				"comment on column person.id is 'Primary key';\n" +
				"comment on column person.first_name is 'The first name';" +
				"comment on column person.last_name is 'The last name';\n" +
				"commit;\n");
			Statement stmt = con.createStatement();
			String sql = "select id, first_name, last_name from person";
			ResultSet rs = stmt.executeQuery(sql);
			ResultInfo info = new ResultInfo(rs.getMetaData(), con);
			ResultColumnMetaData meta = new ResultColumnMetaData(sql, con);
			meta.retrieveColumnRemarks(info);
			for (int i=0; i < info.getColumnCount(); i++)
			{
				if (i == 0)
				{
					assertEquals("Primary key", info.getColumn(i).getComment());
				}
				if (i == 1)
				{
					assertEquals("The first name", info.getColumn(i).getComment());
				}
				if (i == 2)
				{
					assertEquals("The last name", info.getColumn(i).getComment());
				}
			}

			rs.close();

			TestUtil.executeScript(con, "create table address (person_id integer not null, address_info varchar(500));\n" +
				"comment on column address.person_id is 'The person ID';\n" +
				"comment on column address.address_info is 'The address';\n" +
				"commit;\n");

			sql = "select p.id as person_id, a.person_id as address_pid, p.first_name, p.last_name, a.address_info " +
				" from person p join address a on p.id = a.person_id";

			rs = stmt.executeQuery(sql);
			info = new ResultInfo(rs.getMetaData(), con);
			meta = new ResultColumnMetaData(sql, con);
			meta.retrieveColumnRemarks(info);
			for (int i=0; i < info.getColumnCount(); i++)
			{
				if (i == 0)
				{
					assertEquals("Primary key", info.getColumn(i).getComment());
				}
				if (i == 1)
				{
					assertEquals("The person ID", info.getColumn(i).getComment());
				}
				if (i == 2)
				{
					assertEquals("The first name", info.getColumn(i).getComment());
				}
				if (i == 3)
				{
					assertEquals("The last name", info.getColumn(i).getComment());
				}
				if (i == 4)
				{
					assertEquals("The address", info.getColumn(i).getComment());
				}
			}

			sql = "select * from person";
			rs = stmt.executeQuery(sql);
			info = new ResultInfo(rs.getMetaData(), con);
			meta = new ResultColumnMetaData(sql, con);
			meta.retrieveColumnRemarks(info);
			for (int i=0; i < info.getColumnCount(); i++)
			{
				if (i == 0)
				{
					assertEquals("Primary key", info.getColumn(i).getComment());
				}
				if (i == 1)
				{
					assertEquals("The first name", info.getColumn(i).getComment());
				}
				if (i == 2)
				{
					assertEquals("The last name", info.getColumn(i).getComment());
				}
			}

			sql = "select * from person as p";
			rs = stmt.executeQuery(sql);
			info = new ResultInfo(rs.getMetaData(), con);
			meta = new ResultColumnMetaData(sql, con);
			meta.retrieveColumnRemarks(info);
			for (int i=0; i < info.getColumnCount(); i++)
			{
				if (i == 0)
				{
					assertEquals("Primary key", info.getColumn(i).getComment());
				}
				if (i == 1)
				{
					assertEquals("The first name", info.getColumn(i).getComment());
				}
				if (i == 2)
				{
					assertEquals("The last name", info.getColumn(i).getComment());
				}
			}


			sql = "select id, first_name from person p limit 1";
			rs = stmt.executeQuery(sql);
			info = new ResultInfo(rs.getMetaData(), con);
			meta = new ResultColumnMetaData(sql, con);
			meta.retrieveColumnRemarks(info);
			for (int i=0; i < info.getColumnCount(); i++)
			{
				if (i == 0)
				{
					assertEquals("Primary key", info.getColumn(i).getComment());
				}
				if (i == 1)
				{
					assertEquals("The first name", info.getColumn(i).getComment());
				}
			}


			sql = "select p.*, a.* " +
				" from person p join address a on p.id = a.person_id";

			rs = stmt.executeQuery(sql);
			info = new ResultInfo(rs.getMetaData(), con);
			meta = new ResultColumnMetaData(sql, con);
			meta.retrieveColumnRemarks(info);
			for (int i=0; i < info.getColumnCount(); i++)
			{
//				System.out.println(info.getColumn(i).getComment());
				if (i == 0)
				{
					assertEquals("Primary key", info.getColumn(i).getComment());
				}
				if (i == 1)
				{
					assertEquals("The first name", info.getColumn(i).getComment());
				}
				if (i == 2)
				{
					assertEquals("The last name", info.getColumn(i).getComment());
				}
				if (i == 3)
				{
					assertEquals("The person ID", info.getColumn(i).getComment());
				}
				if (i == 4)
				{
					assertEquals("The address", info.getColumn(i).getComment());
				}
			}

		}
		finally
		{
			con.disconnect();
		}
	}
}
