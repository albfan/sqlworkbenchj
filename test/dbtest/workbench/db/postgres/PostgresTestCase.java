/*
 * PostgresTestHelper
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.Statement;
import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTestCase
	extends WbTestCase
{

	public PostgresTestCase(String testName)
	{
		super(testName);
	}

	public static void initTestCase(String schema)
		throws Exception
	{
		TestUtil util = new TestUtil(schema);
		util.prepareEnvironment();

		WbConnection con = TestUtil.getPostgresConnection();
		if (con == null)
		{
			return;
		}
		
		Statement stmt = null;
		try
		{
			stmt = con.createStatement();


			if (StringUtil.isBlank(schema))
			{
				schema = "junit";
			}
			else
			{
				schema = schema.toLowerCase();
			}

			List<String> schemas = con.getMetadata().getSchemas();
			if (schemas.contains(schema))
			{
				// Make sure everything is cleaned up
				dropTestSchema(con, schema);
			}
			stmt.execute("create schema "+ schema);
			stmt.execute("set session schema '" + schema + "'");
			con.commit();
		}
		catch (Exception e)
		{
			con.rollbackSilently();
		}
	}

	public static void cleanUpTestCase(String schema)
	{
		WbConnection con = TestUtil.getPostgresConnection();
		dropTestSchema(con, schema);
		ConnectionMgr.getInstance().disconnectAll();
	}
	
	public static void dropTestSchema(WbConnection con, String schema)
	{
		if (con == null) return;

		Statement stmt = null;
		try
		{
			stmt = con.createStatement();

			if (StringUtil.isBlank(schema))
			{
				schema = "junit";
			}
			else
			{
				schema = schema.toLowerCase();
			}

			stmt.execute("drop schema "+ schema + " cascade");
			con.commit();
		}
		catch (Exception e)
		{
			con.rollbackSilently();
		}
	}
}
