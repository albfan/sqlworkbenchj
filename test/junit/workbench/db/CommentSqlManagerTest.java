/*
 * CommentSqlManagerTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.util.List;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.util.CollectionUtil;
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
			String sql = mgr.getCommentSqlTemplate(type);
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
			String sql = mgr.getCommentSqlTemplate(type);
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
			String sql = mgr.getCommentSqlTemplate(type);
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
			String sql = mgr.getCommentSqlTemplate(type);
			assertNotNull("No template found for type " + type, sql);
		}
	}



}
