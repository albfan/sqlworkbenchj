/*
 * PostgresTestHelper
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.ibm;

import java.util.List;
import java.sql.Statement;
import workbench.AppArguments;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.sql.BatchRunner;
import workbench.util.ArgumentParser;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2TestUtil
{
	private static boolean isAvailable = true;
	public static final String SCHEMA_NAME = "WBJUNIT";
	
	/**
	 * Return a connection to a locally running DB2 database
	 * @return null if DB2 is not available
	 */
	public static WbConnection getDb2Connection()
	{
		final String id = "WBJUnitDB2";
		try
		{
			WbConnection con = ConnectionMgr.getInstance().findConnection(id);
			if (con != null) return con;

			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:db2://localhost:50000/wbjunit' -username=wbjunit -password=wbjunit -driver=com.ibm.db2.jcc.DB2Driver");
			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
			prof.setName("WBJUnitDB2");
			ConnectionMgr.getInstance().addProfile(prof);
			con = ConnectionMgr.getInstance().getConnection(prof, id);
			return con;
		}
		catch (Throwable th)
		{
			return null;
		}
	}

	public static void initTestCase(String schema)
		throws Exception
	{
		TestUtil util = new TestUtil(schema);
		util.prepareEnvironment();

		WbConnection con = getDb2Connection();
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
		if (!isAvailable) return;
		WbConnection con = getDb2Connection();
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
