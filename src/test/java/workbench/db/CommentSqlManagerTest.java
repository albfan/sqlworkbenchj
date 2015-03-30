/*
 * CommentSqlManagerTest.java
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

import java.util.List;

import workbench.WbTestCase;

import workbench.util.CollectionUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class CommentSqlManagerTest
	extends WbTestCase
{

	public CommentSqlManagerTest()
	{
		super("CommentSqlManagerTest");
	}

	@Test
	public void testH2()
	{
		CommentSqlManager mgr = new CommentSqlManager("h2");
		List<String> types = CollectionUtil.arrayList("alias", "domain", "constant", "table", "view", "column", "index", "sequence");
		for (String type : types)
		{
			String sql = mgr.getCommentSqlTemplate(type, null);
			assertNotNull("No template found for type " + type, sql);
		}
	}

	@Test
	public void testPostgreSQL()
	{
		CommentSqlManager mgr = new CommentSqlManager("postgresql");
		List<String> types = CollectionUtil.arrayList("function", "table", "view", "column", "type", "index", "sequence", "trigger");
		for (String type : types)
		{
			String sql = mgr.getCommentSqlTemplate(type, null);
			assertNotNull("No template found for type " + type, sql);
		}
	}

	@Test
	public void testOracle()
	{
		CommentSqlManager mgr = new CommentSqlManager("oracle");
		List<String> types = CollectionUtil.arrayList("table", "column", "materialized view");
		for (String type : types)
		{
			String sql = mgr.getCommentSqlTemplate(type, null);
			assertNotNull("No template found for type " + type, sql);
		}
	}

	@Test
	public void testSQLServer()
	{
		CommentSqlManager mgr = new CommentSqlManager("microsoft_sql_server");
		List<String> types = CollectionUtil.arrayList("table", "column", "view", "synonym", "procedure");
		for (String type : types)
		{
			String sql = mgr.getCommentSqlTemplate(type, null);
			assertNotNull("No template found for type " + type, sql);
		}
	}



}
