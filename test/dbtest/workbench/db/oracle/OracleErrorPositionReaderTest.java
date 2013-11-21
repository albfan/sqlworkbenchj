/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.oracle;

import workbench.WbTestCase;

import workbench.db.WbConnection;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleErrorPositionReaderTest
	extends WbTestCase
{

	public OracleErrorPositionReaderTest()
	{
		super("GetErrorTest");
	}

	@Test
	public void testGetErrorPosition()
		throws Exception
	{
		WbConnection conn = OracleTestUtil.getOracleConnection();
		OracleErrorPositionReader reader = new OracleErrorPositionReader();
		int pos = reader.getErrorPosition(conn, "select 42 from dualx");
		assertEquals(15, pos);

		pos = reader.getErrorPosition(conn, "select 42 from dual");
		assertEquals(-1, pos);
	}

}
