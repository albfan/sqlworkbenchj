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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import workbench.AppArguments;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.sql.BatchRunner;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2TestUtil
{
	private static boolean isAvailable = true;


	public static String getSchemaName()
	{
		return getProperty("wbjunit.db2.schema", "wbjunit").toUpperCase();
	}

	private static String getProperty(String key, String defaultValue)
	{
		String value= System.getProperty(key, null);
		if (value == null || value.equals("${" + key + "}"))
		{
			return defaultValue;
		}
		return value;
	}
	/**
	 * Return a connection to a locally running DB2 database
	 * @return null if DB2 is not available
	 */
	public static WbConnection getDb2Connection()
	{
		final String id = "WBJUnitDB2";
		if (!isAvailable) return null;
		try
		{
			WbConnection con = ConnectionMgr.getInstance().findConnection(id);
			if (con != null) return con;

			String dbname = getProperty("wbjunit.db2.testdb", "tkdb");
			String username = getProperty("wbjunit.db2.user", "thomas");
			String pwd = getProperty("wbjunit.db2.password", "welcome");

			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:db2://localhost:50000/" + dbname + "' -username=" + username + " -password=" + pwd + " -driver=com.ibm.db2.jcc.DB2Driver");
			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
			prof.setName("WBJUnitDB2");
			ConnectionMgr.getInstance().addProfile(prof);
			con = ConnectionMgr.getInstance().getConnection(prof, id);
			return con;
		}
		catch (Throwable th)
		{
			isAvailable = false;
			return null;
		}
	}

	public static void initTestCase()
		throws Exception
	{
		String schema = getSchemaName();
		TestUtil util = new TestUtil(schema);
		util.prepareEnvironment();

		if (!isAvailable) return;
		
		WbConnection con = getDb2Connection();
		if (con == null)
		{
			isAvailable = false;
			return;
		}

		Statement stmt = null;
		try
		{
			stmt = con.createStatement();

			stmt.execute("set schema " + schema);
			dropAllObjects(con, schema);
			con.commit();
		}
		catch (Exception e)
		{
			con.rollbackSilently();
		}
	}

	public static void cleanUpTestCase()
	{
		if (!isAvailable) return;
		WbConnection con = getDb2Connection();
		String schema = getSchemaName();
		//dropAllObjects(con, schema);
		ConnectionMgr.getInstance().disconnectAll();
	}

	public static void dropAllObjects(WbConnection con, String schema)
	{
		if (con == null) return;

		Statement drop = null;

		try
		{
			List<TableIdentifier> tables = con.getMetadata().getObjectList(getSchemaName(), null);
			drop = con.createStatement();
			for (TableIdentifier tbl : tables)
			{
				String dropSql = tbl.getDropStatement(con, true);
				try
				{
					drop.execute(dropSql);
				}
				catch (SQLException e)
				{
					// ignore
				}
			}
			con.commit();
		}
		catch (Exception e)
		{
			con.rollbackSilently();
		}
		finally
		{
			SqlUtil.closeStatement(drop);
		}
	}

}
