/*
 * TextFileParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TextFileParserTest
	extends WbTestCase
{
	private TestUtil util;
	private WbConnection connection;

	public TextFileParserTest()
	{
		super("TextFileParserTest");
		util = getTestUtil();
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
		this.connection.disconnect();
	}

	@Test
	public void testSetColumns()
		throws Exception
	{
		TextFileParser parser = new TextFileParser();
		parser.setConnection(connection);
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
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
		cols =new ArrayList<ColumnIdentifier>();
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
		util.emptyBaseDirectory();
		WbConnection wb = util.getConnection();

		Statement stmt = wb.createStatement();
		stmt.executeUpdate("CREATE TABLE person (nr integer, firstname varchar(100), lastname varchar(100))");
		wb.commit();
		stmt.close();

		return wb;
	}
}
