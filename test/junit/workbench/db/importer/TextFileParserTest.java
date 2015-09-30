/*
 * TextFileParserTest.java
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
package workbench.db.importer;


import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class TextFileParserTest
	extends WbTestCase
{
	private WbConnection connection;

	public TextFileParserTest()
	{
		super("TextFileParserTest");
	}

	@Before
	public void setUp()
		throws Exception
	{
		connection = prepareDatabase();
	}

	@After
	public void tearDown()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testSetColumns()
		throws Exception
	{
		TextFileParser parser = new TextFileParser();
		parser.setConnection(connection);
		List<ColumnIdentifier> cols = new ArrayList<>();
		cols.add(new ColumnIdentifier("lastname"));
		cols.add(new ColumnIdentifier("firstname"));
		cols.add(new ColumnIdentifier("nr"));
		parser.setTableName("person");
		parser.setColumns(cols);

		List<ColumnIdentifier> toImport = parser.getColumnsToImport();
		assertNotNull(toImport);
		assertEquals(3, toImport.size());
		assertEquals("NR", toImport.get(2).getColumnName());
		assertEquals("FIRSTNAME", toImport.get(1).getColumnName());
		assertEquals("LASTNAME", toImport.get(0).getColumnName());

		parser = new TextFileParser();
		parser.setConnection(connection);
		cols =new ArrayList<>();
		cols.add(new ColumnIdentifier("lastname"));
		cols.add(new ColumnIdentifier(RowDataProducer.SKIP_INDICATOR));
		cols.add(new ColumnIdentifier("firstname"));
		cols.add(new ColumnIdentifier("nr"));
		parser.setTableName("person");
		parser.setColumns(cols);

		toImport = parser.getColumnsToImport();
		assertNotNull(toImport);
		assertEquals(3, toImport.size());
		assertEquals("NR", toImport.get(2).getColumnName());
		assertEquals("FIRSTNAME", toImport.get(1).getColumnName());
		assertEquals("LASTNAME", toImport.get(0).getColumnName());
	}

	private WbConnection prepareDatabase()
		throws SQLException, ClassNotFoundException
	{
		getTestUtil().emptyBaseDirectory();
		WbConnection conn = getTestUtil().getConnection();

    TestUtil.executeScript(conn,
      "CREATE TABLE person (nr integer, firstname varchar(100), lastname varchar(100));\n" +
      "commit");

		return conn;
	}
}
