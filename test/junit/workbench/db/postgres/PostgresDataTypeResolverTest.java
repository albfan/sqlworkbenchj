/*
 * PostgresDataTypeResolverTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.postgres;

import java.sql.Types;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class PostgresDataTypeResolverTest
{

	@Test
	public void testGetSqlTypeDisplay()
	{
		PostgresDataTypeResolver resolver = new PostgresDataTypeResolver();

		String display = resolver.getSqlTypeDisplay("NUMERIC", Types.NUMERIC, 65535, 0);
		assertEquals("NUMERIC", display);

		display = resolver.getSqlTypeDisplay("NUMERIC", Types.NUMERIC, 131089, 0);
		assertEquals("NUMERIC", display);

		display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 300, 0);
		assertEquals("VARCHAR(300)", display);

		display = resolver.getSqlTypeDisplay("varchar", Types.VARCHAR, Integer.MAX_VALUE, 0);
		assertEquals("varchar", display);

		display = resolver.getSqlTypeDisplay("text", Types.VARCHAR, 300, 0);
		assertEquals("text", display);

		display = resolver.getSqlTypeDisplay("int8", Types.BIGINT, 0, 0);
		assertEquals("bigint", display);

		display = resolver.getSqlTypeDisplay("int4", Types.INTEGER, 0, 0);
		assertEquals("integer", display);

		display = resolver.getSqlTypeDisplay("int2", Types.SMALLINT, 0, 0);
		assertEquals("smallint", display);
	}

}
