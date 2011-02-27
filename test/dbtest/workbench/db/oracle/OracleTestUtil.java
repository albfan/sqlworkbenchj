/*
 * PostgresTestHelper
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.AppArguments;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.sql.BatchRunner;
import workbench.sql.DelimiterDefinition;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTestUtil
{
	private static boolean isAvailable = true;
	public static final String SCHEMA_NAME = "WBJUNIT";

	/**
	 * Return a connection to a locally running Oracle database.
	 *
	 * The user WBJUNIT should have the password WBJUNIT and the following privileges:
	 * <ul>
	 * <li>create synonym</li>
	 * <li>create materialized view</li>
	 * <li>create sequence</li>
	 * <li>create type</li>
	 * <li>create view</li>
	 * </ul>
	 * @return null if Oracle is not available
	 */
	public static WbConnection getOracleConnection()
	{
		final String id = "WBJUnitOracle";
		try
		{
			WbConnection con = ConnectionMgr.getInstance().findConnection(id);
			if (con != null) return con;

			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:oracle:thin:@localhost:1521:oradb' -username=wbjunit -password=wbjunit -driver=oracle.jdbc.OracleDriver");
			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
			prof.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
			prof.setName("WBJUnitOracle");
			prof.addConnectionProperty("oracle.jdbc.remarksReporting", "true");
			ConnectionMgr.getInstance().addProfile(prof);
			con = ConnectionMgr.getInstance().getConnection(prof, id);
			return con;
		}
		catch (Throwable th)
		{
			return null;
		}
	}

	public static void initTestCase()
		throws Exception
	{
		TestUtil util = new TestUtil("OracleJUnit");
		util.prepareEnvironment();

		if (!isAvailable) return;

		WbConnection con = getOracleConnection();
		if (con == null)
		{
			isAvailable = false;
			return;
		}
		dropAllObjects(con);
	}

	public static void cleanUpTestCase()
	{
		if (!isAvailable) return;
		WbConnection con = getOracleConnection();
		dropAllObjects(con);
		ConnectionMgr.getInstance().disconnectAll();
	}

	public static void dropAllObjects(WbConnection con)
	{
		if (con == null) return;

		Statement stmt = null;
		Statement drop = null;
		ResultSet rs = null;
		String sql = "SELECT object_type, object_name FROM user_objects";
		try
		{
			stmt = con.createStatement();
			drop = con.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String type = rs.getString(1);
				String name = rs.getString(2);
				String dropSql = getDropStatement(name, type);
				try
				{
					drop.execute(dropSql);
				}
				catch (SQLException e)
				{
					// ignore
				}
			}
			stmt.executeUpdate("purge recyclebin");
		}
		catch (Exception e)
		{
			con.rollbackSilently();
		}
		finally
		{
			SqlUtil.closeResult(rs);
			SqlUtil.closeStatement(stmt);
			SqlUtil.closeStatement(drop);
		}
	}

	private static String getDropStatement(String name, String type)
	{
		if (type.equals("TABLE"))
		{
			return "DROP TABLE " + name + " CASCADE CONSTRAINTS";
		}
		else
		{
			return "DROP " + type + " " + name;
		}
	}
}
