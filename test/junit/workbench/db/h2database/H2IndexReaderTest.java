/*
 * H2IndexReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.h2database;

import org.junit.AfterClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class H2IndexReaderTest
	extends WbTestCase
{

	public H2IndexReaderTest()
	{
		super("H2IndexReaderTest");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testGetPrimaryKeyIndex()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		TestUtil.executeScript(con, "create table person (id integer primary key, person_name varchar(100));");
		TableIdentifier tbl = new TableIdentifier("PERSON");
		IndexReader reader = con.getMetadata().getIndexReader();
		assertTrue(reader instanceof H2IndexReader);
		String pkname = reader.getPrimaryKeyIndex(tbl);
		assertNotNull(pkname);
	}
}
