/*
 * DerbySynonymReaderTest.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.derby;

import java.util.List;
import org.junit.*;
import workbench.WbTestCase;
import static org.junit.Assert.*;import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.SequenceDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;


/**
 *
 * @author Thomas Kellerer
 */
public class DerbySynonymReaderTest
	extends WbTestCase
{

	public DerbySynonymReaderTest()
	{
		super("DerbySynonymReaderTest");
	}

	@After
	public void tearDown()
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testGetSynonymList()
		throws Exception
	{
		WbConnection con = DerbyTestUtil.getDerbyConnection("syntest");
		String sql =
			"create table foo (bar integer);\n" +
			"create synonym foobar for foo;\n";

		TestUtil.executeScript(con, sql);
		con.commit();

		List<TableIdentifier> syns = con.getMetadata().getObjectList(null, new String[] {"SYNONYM" });

		assertNotNull(syns);
		assertEquals(1, syns.size());
		TableIdentifier synonym = syns.get(0);
		assertEquals("FOOBAR", synonym.getTableName());

		TableIdentifier table = con.getMetadata().getSynonymTable(synonym);
		assertNotNull(table);
		assertEquals("FOO", table.getTableName());

		String source = synonym.getSource(con).toString();
		System.out.println(source);
		String expected =
				"CREATE SYNONYM FOOBAR\n" +
				"   FOR APP.FOO;";
		assertEquals(expected, source.trim());
	}


}
