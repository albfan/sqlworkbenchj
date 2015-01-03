/*
 * GenericSchemaInfoReaderTest.java
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
