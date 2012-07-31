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
package workbench.db.oracle;


import org.junit.Test;
import static org.junit.Assert.*;

import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleSchemaReaderTest
	extends WbTestCase
{
	public OracleSchemaReaderTest()
	{
		super("OracleSchemaReaderTest");
	}

	@Test
	public void testReadSchema()
		throws Exception
	{
		WbConnection conn = OracleTestUtil.getOracleConnection();
		if (conn == null) return;

		try
		{
			String schema = conn.getCurrentSchema();
			assertEquals(OracleTestUtil.SCHEMA_NAME, schema);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
