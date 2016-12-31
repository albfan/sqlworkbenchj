/*
 * TypeMapperTest.java
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

import java.sql.SQLException;
import java.sql.Types;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.ssh.SshException;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TypeMapperTest
	extends WbTestCase
{

	public TypeMapperTest()
	{
		super("TypeMapperTest");
	}


	@Test
	public void testGetTypeName()
	{
		TypeMapper mapper = new TypeMapper();
		mapper.parseUserTypeMap("3:DOUBLE;2:NUMERIC($size, $digits);-1:VARCHAR2($size);93:datetime year to second");
		String type = mapper.getUserMapping(3, 1, 1);
		assertEquals("DOUBLE", type);
		type = mapper.getUserMapping(2, 11, 3);
		assertEquals("NUMERIC(11, 3)", type);
		type = mapper.getUserMapping(-1, 100, 0);
		assertEquals("VARCHAR2(100)", type);
		type = mapper.getUserMapping(93, 0, 0);
		assertEquals("datetime year to second", type);
	}

	@Test
	public void testExtendedColtype()
	{
		TypeMapper mapper = new TypeMapper();
		mapper.parseUserTypeMap("12:VARCHAR($size) UNICODE");
		String type = mapper.getUserMapping(12, 30, 0);
    assertEquals("VARCHAR(30) UNICODE", type);
	}

	@Test
	public void testDbMapping()
		throws SQLException, ClassNotFoundException, SshException
	{
		try
		{
			TestUtil util = getTestUtil();
			WbConnection conn = util.getConnection("TypeMapper");
			TypeMapper mapper = new TypeMapper(conn);
			String type = mapper.getTypeName(Types.VARCHAR, 20, -1);
			assertEquals("VARCHAR(20)", type);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
