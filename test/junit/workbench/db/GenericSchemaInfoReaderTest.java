/*
 * GenericSchemaInfoReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import workbench.db.derby.DerbyTestUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class GenericSchemaInfoReaderTest
	extends WbTestCase
{
	public GenericSchemaInfoReaderTest()
	{
		super("GenericSchemaInfoReaderTest");
	}

	@Test
	public void testReadSchema()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection h2 = util.getConnection();
		try
		{
			String schema = h2.getCurrentSchema();
			assertEquals("PUBLIC", schema);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}

		WbConnection hsql = util.getHSQLConnection("inforeader");
		try
		{
			String schema = hsql.getCurrentSchema();
			assertEquals("PUBLIC", schema);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}

		WbConnection derby = DerbyTestUtil.getDerbyConnection(util.getBaseDir());
		try
		{
			String schema = derby.getCurrentSchema();
			assertEquals("APP", schema);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
