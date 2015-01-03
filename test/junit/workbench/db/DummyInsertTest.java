/*
 * DummyInsertTest.java
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
package workbench.db;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.TestUtil;

import workbench.util.SqlUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DummyInsertTest
{

	@Test
	public void testGetSource()
		throws Exception
	{
		TestUtil util = new TestUtil("dummyInsertGen1");
		WbConnection con = util.getConnection();

		try
		{
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table person (nr integer, firstname varchar(20), lastname varchar(20))");
			con.commit();
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			DummyInsert insert = new DummyInsert(person);
			assertEquals("INSERT", insert.getObjectType());

			String sql = insert.getSource(con).toString();

			String verb = SqlUtil.getSqlVerb(sql);
			assertEquals("INSERT", verb);
			assertTrue(sql.indexOf("(\n  NR_value,\n  'FIRSTNAME_value',\n  'LASTNAME_value'") > -1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testSelectedColumns()
		throws Exception
	{
		TestUtil util = new TestUtil("dummyInsertGen1");
		WbConnection con = util.getConnection();

		try
		{
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table person (nr integer, firstname varchar(20), lastname varchar(20))");
			con.commit();
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
			cols.add(new ColumnIdentifier("NR"));

			DummyInsert insert = new DummyInsert(person, cols);
			String sql = insert.getSource(con).toString();
			assertTrue(sql.trim().equals("INSERT INTO PERSON\n(\n  NR\n)\nVALUES\n(\n  NR_value\n);"));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
