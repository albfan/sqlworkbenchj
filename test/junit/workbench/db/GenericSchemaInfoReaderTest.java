/*
 * GenericSchemaInfoReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.derby.DerbyTestUtil;

import org.junit.Test;

import static org.junit.Assert.*;

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
