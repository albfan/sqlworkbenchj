/*
 * OracleDataTypeResolverTest.java
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
package workbench.db.oracle;

import java.sql.Types;
import org.junit.Before;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDataTypeResolverTest
	extends WbTestCase
{
	public OracleDataTypeResolverTest()
	{
		super("OracleDataTypeResolverTest");
	}

	@Before
	public void setUp()
	{
	}

	@Test
	public void testGetSqlTypeDisplay()
	{
		// Test with BYTE as default semantics
		OracleDataTypeResolver resolver = new OracleDataTypeResolver(OracleUtils.BYTE_SEMANTICS, false);

		// Test non-Varchar types
		assertEquals("CLOB", resolver.getSqlTypeDisplay("CLOB", Types.CLOB, -1, -1, 0));
		assertEquals("NVARCHAR(300)", resolver.getSqlTypeDisplay("NVARCHAR", Types.VARCHAR, 300, -1, 0));
		assertEquals("CHAR(5)", resolver.getSqlTypeDisplay("CHAR", Types.CHAR, 5, -1, 0));
		assertEquals("NUMBER(10,2)", resolver.getSqlTypeDisplay("NUMBER", Types.NUMERIC, 10, 2, 0));

		String display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleUtils.BYTE_SEMANTICS);
		assertEquals("VARCHAR(200)", display);

		display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleUtils.CHAR_SEMANTICS);
		assertEquals("VARCHAR(200 Char)", display);

		resolver = new OracleDataTypeResolver(OracleUtils.CHAR_SEMANTICS, false);

		display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleUtils.BYTE_SEMANTICS);
		assertEquals("VARCHAR(200 Byte)", display);

		display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleUtils.CHAR_SEMANTICS);
		assertEquals("VARCHAR(200)", display);

		resolver = new OracleDataTypeResolver(OracleUtils.CHAR_SEMANTICS, true);

		display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleUtils.BYTE_SEMANTICS);
		assertEquals("VARCHAR(200 Byte)", display);

		display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleUtils.CHAR_SEMANTICS);
		assertEquals("VARCHAR(200 Char)", display);
	}


}
