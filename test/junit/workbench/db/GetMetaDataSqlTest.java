/*
 * GetMetaDataSqlTest.java
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

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class GetMetaDataSqlTest
{
	public GetMetaDataSqlTest()
	{
	}

	@Test
	public void testContainsWhere()
	{
		GetMetaDataSql meta = new GetMetaDataSql();
		String sql = "select * from foo";
		assertFalse(meta.containsWhere(sql));

		sql = "select * from foo where x=5";
		assertTrue(meta.containsWhere(sql));

		sql = "select * from (select x from y where a = 42) t";
		assertFalse(meta.containsWhere(sql));

		sql = "select * from (select x from y where a = 42 union all select c from d where x = 1) t";
		assertFalse(meta.containsWhere(sql));

		sql = "select * from (select x from y where a = 42) t where t.x = 6";
		assertTrue(meta.containsWhere(sql));

		sql = "select *\n" +
			"from a \n" +
			"  join b on a.x = b.y \n" +
			"  join c on c.x = b.y \n" +
			"where a.x in (1,2)";
		assertTrue(meta.containsWhere(sql));
	}

	@Test
	public void testGenerateSql()
	{
		GetMetaDataSql meta = new GetMetaDataSql();
		meta.setBaseSql("select * from all_source");
		meta.setSchemaField("owner");
		meta.setSchema("FOOBAR");
		String sql = meta.getSql();
		assertEquals("select * from all_source WHERE owner = 'FOOBAR'", sql);

		meta.setBaseSql("select * from all_source where type = 'V'");
		sql = meta.getSql();
		assertEquals("select * from all_source where type = 'V' AND owner = 'FOOBAR'", sql);

		meta.setBaseSql("select * from (select foo from bar) t where type = 'V'");
		sql = meta.getSql();
		assertEquals("select * from (select foo from bar) t where type = 'V' AND owner = 'FOOBAR'", sql);

		meta.setBaseSql("select * from (select foo from bar) t ");
		sql = meta.getSql();
		System.out.println(sql);
		assertEquals("select * from (select foo from bar) t WHERE owner = 'FOOBAR'", sql);
	}
}