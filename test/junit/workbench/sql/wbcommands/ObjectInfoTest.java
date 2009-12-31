/*
 * ObjectInfoTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectInfoTest
	extends TestCase
{
	private WbConnection db;
	
	public ObjectInfoTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
		TestUtil util = new TestUtil(getName());
		db = util.getConnection(getName());
		TestUtil.executeScript(db,
				"CREATE TABLE person (nr integer, person_name varchar(100));" +
				"CREATE VIEW v_person AS SELECT * FROM PERSON;" +
				"create sequence seq_id;" +
				"commit;"
		);
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		super.tearDown();
	}

	public void testGetObjectInfo()
		throws Exception
	{
		String objectName = "person";
		ObjectInfo info = new ObjectInfo();
		StatementRunnerResult tableInfo = info.getObjectInfo(db, objectName, false);
		assertTrue(tableInfo.hasDataStores());
		DataStore ds = tableInfo.getDataStores().get(0);
		assertEquals(2, ds.getRowCount());
		assertEquals("NR", ds.getValueAsString(0, 0));
		assertEquals("PERSON_NAME", ds.getValueAsString(1, 0));

		StatementRunnerResult viewInfo = info.getObjectInfo(db, "v_person", false);
//		System.out.println(viewInfo.getSourceCommand());
		assertTrue(viewInfo.getSourceCommand().startsWith("CREATE FORCE VIEW"));
		assertTrue(viewInfo.hasDataStores());
		
		DataStore viewDs = tableInfo.getDataStores().get(0);
		assertEquals(2, viewDs.getRowCount());
		assertEquals("NR", viewDs.getValueAsString(0, 0));
		assertEquals("PERSON_NAME", viewDs.getValueAsString(1, 0));

		StatementRunnerResult seqInfo = info.getObjectInfo(db, "seq_id", false);
//		System.out.println(seqInfo.getSourceCommand());
		assertTrue(seqInfo.hasDataStores());
		assertEquals(1, seqInfo.getDataStores().get(0).getRowCount());
	}
}
