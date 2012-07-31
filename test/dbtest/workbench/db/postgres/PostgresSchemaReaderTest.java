/*
 * PostgresSchemaReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;


import org.junit.Test;
import static org.junit.Assert.*;

import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresSchemaReaderTest
	extends WbTestCase
{
	public PostgresSchemaReaderTest()
	{
		super("PostgresSchemaReaderTest");
	}
	
	@Test
	public void testReadSchema()
		throws Exception
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		if (conn == null) return;

		try
		{
			String schema = conn.getCurrentSchema();
			assertEquals("public", schema);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
